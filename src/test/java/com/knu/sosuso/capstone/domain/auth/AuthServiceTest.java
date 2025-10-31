package com.knu.sosuso.capstone.domain.auth;

import com.knu.sosuso.capstone.global.exception.BusinessException;
import com.knu.sosuso.capstone.global.exception.error.AuthenticationError;
import com.knu.sosuso.capstone.global.security.jwt.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    private String validToken;
    private String sub;
    private User mockUser;

    @BeforeEach
    void setUp() {
        validToken = "valid.jwt.token";
        sub = "google-sub-123456";

        mockUser = User.builder()
                .sub(sub)
                .email("test@example.com")
                .name("테스트유저")
                .role("ROLE_USER")
                .picture("https://example.com/picture.jpg")
                .build();
    }

    @Nested
    @DisplayName("getUserInformation 메서드는")
    class GetUserInformationTest {

        @Test
        @DisplayName("유효한 토큰으로 사용자 정보를 조회한다")
        void getUserInformation_Success() {
            // given
            given(jwtUtil.isValidToken(validToken)).willReturn(true);
            given(jwtUtil.getSub(validToken)).willReturn(sub);
            given(userRepository.findBySub(sub)).willReturn(mockUser);

            // when
            LoginResponse response = authService.getUserInformation(validToken);

            // then
            assertThat(response).isNotNull();
            assertThat(response.userName()).isEqualTo("테스트유저");
            assertThat(response.userEmail()).isEqualTo("test@example.com");
            assertThat(response.userProfileImage()).isEqualTo("https://example.com/picture.jpg");

            verify(jwtUtil).isValidToken(validToken);
            verify(jwtUtil).getSub(validToken);
            verify(userRepository).findBySub(sub);
        }

        @Test
        @DisplayName("토큰이 null이면 TOKEN_NOT_FOUND 예외를 던진다")
        void getUserInformation_TokenIsNull_ThrowsException() {
            // given
            String nullToken = null;

            // when & then
            assertThatThrownBy(() -> authService.getUserInformation(nullToken))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("httpStatus", AuthenticationError.TOKEN_NOT_FOUND.getHttpStatus())
                    .hasMessageContaining(AuthenticationError.TOKEN_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("토큰이 빈 문자열이면 TOKEN_NOT_FOUND 예외를 던진다")
        void getUserInformation_TokenIsEmpty_ThrowsException() {
            // given
            String emptyToken = "";

            // when & then
            assertThatThrownBy(() -> authService.getUserInformation(emptyToken))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("httpStatus", AuthenticationError.TOKEN_NOT_FOUND.getHttpStatus())
                    .hasMessageContaining(AuthenticationError.TOKEN_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("토큰이 공백만 있으면 TOKEN_NOT_FOUND 예외를 던진다")
        void getUserInformation_TokenIsBlank_ThrowsException() {
            // given
            String blankToken = "   ";

            // when & then
            assertThatThrownBy(() -> authService.getUserInformation(blankToken))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("httpStatus", AuthenticationError.TOKEN_NOT_FOUND.getHttpStatus())
                    .hasMessageContaining(AuthenticationError.TOKEN_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("유효하지 않은 토큰이면 INVALID_TOKEN 예외를 던진다")
        void getUserInformation_InvalidToken_ThrowsException() {
            // given
            String invalidToken = "invalid.jwt.token";
            given(jwtUtil.isValidToken(invalidToken)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.getUserInformation(invalidToken))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("httpStatus", AuthenticationError.INVALID_TOKEN.getHttpStatus())
                    .hasMessageContaining(AuthenticationError.INVALID_TOKEN.getMessage());

            verify(jwtUtil).isValidToken(invalidToken);
        }

        @Test
        @DisplayName("사용자를 찾을 수 없으면 USER_NOT_FOUND 예외를 던진다")
        void getUserInformation_UserNotFound_ThrowsException() {
            // given
            given(jwtUtil.isValidToken(validToken)).willReturn(true);
            given(jwtUtil.getSub(validToken)).willReturn(sub);
            given(userRepository.findBySub(sub)).willReturn(null);

            // when & then
            assertThatThrownBy(() -> authService.getUserInformation(validToken))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("httpStatus", AuthenticationError.USER_NOT_FOUND.getHttpStatus())
                    .hasMessageContaining(AuthenticationError.USER_NOT_FOUND.getMessage());

            verify(jwtUtil).isValidToken(validToken);
            verify(jwtUtil).getSub(validToken);
            verify(userRepository).findBySub(sub);
        }
    }
}