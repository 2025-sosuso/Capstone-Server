package com.knu.sosuso.capstone.domain.auth;

import com.knu.sosuso.capstone.global.ResponseDto;
import com.knu.sosuso.capstone.global.exception.BusinessException;
import com.knu.sosuso.capstone.global.exception.error.AuthenticationError;
import com.knu.sosuso.capstone.global.security.CustomSuccessHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController 단위 테스트")
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private CustomSuccessHandler customSuccessHandler;

    @InjectMocks
    private AuthController authController;

    private String validToken;
    private LoginResponse mockLoginResponse;

    @BeforeEach
    void setUp() {
        validToken = "valid.jwt.token";
        mockLoginResponse = new LoginResponse(
                "테스트유저",
                "test@example.com",
                "https://example.com/picture.jpg"
        );
    }

    @Nested
    @DisplayName("googleLogin 메서드는")
    class GoogleLoginTest {

        @Test
        @DisplayName("유효한 토큰으로 로그인 성공 응답을 반환한다")
        void googleLogin_Success() {
            // given
            given(authService.getUserInformation(validToken)).willReturn(mockLoginResponse);

            // when
            ResponseDto<LoginResponse> response = authController.googleLogin(validToken);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getData()).isEqualTo(mockLoginResponse);
            assertThat(response.getMessage()).isEqualTo("Successfully signed in to Google Social.");
            assertThat(response.getTimeStamp()).isNotNull();

            verify(authService).getUserInformation(validToken);
        }

        @Test
        @DisplayName("토큰이 유효하지 않으면 예외가 발생한다")
        void googleLogin_InvalidToken_ThrowsException() {
            // given
            String invalidToken = "invalid.token";
            given(authService.getUserInformation(invalidToken))
                    .willThrow(new BusinessException(AuthenticationError.INVALID_TOKEN));

            // when & then
            assertThatThrownBy(() -> authController.googleLogin(invalidToken))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("httpStatus", AuthenticationError.INVALID_TOKEN.getHttpStatus());

            verify(authService).getUserInformation(invalidToken);
        }
    }

    @Nested
    @DisplayName("googleLogout 메서드는")
    class GoogleLogoutTest {

        @Test
        @DisplayName("로그아웃 성공 응답을 반환한다")
        void googleLogout_Success() throws Exception {
            // given
            HttpServletRequest request = new MockHttpServletRequest();
            HttpServletResponse response = new MockHttpServletResponse();

            doNothing().when(customSuccessHandler)
                    .logout(eq(validToken), any(HttpServletRequest.class), any(HttpServletResponse.class));

            // when
            ResponseDto<?> result = authController.googleLogout(validToken, request, response);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getMessage()).isEqualTo("Successfully Logged out.");
            assertThat(result.getTimeStamp()).isNotNull();

            verify(customSuccessHandler).logout(eq(validToken), any(HttpServletRequest.class), any(HttpServletResponse.class));
        }

        @Test
        @DisplayName("토큰이 null이어도 로그아웃 성공 응답을 반환한다")
        void googleLogout_NullToken_Success() throws Exception {
            // given
            HttpServletRequest request = new MockHttpServletRequest();
            HttpServletResponse response = new MockHttpServletResponse();

            doNothing().when(customSuccessHandler)
                    .logout(isNull(), any(HttpServletRequest.class), any(HttpServletResponse.class));

            // when
            ResponseDto<?> result = authController.googleLogout(null, request, response);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getMessage()).isEqualTo("Successfully Logged out.");

            verify(customSuccessHandler).logout(isNull(), any(HttpServletRequest.class), any(HttpServletResponse.class));
        }
    }
}