package com.knu.sosuso.capstone.global.security.jwt;

import com.knu.sosuso.capstone.global.exception.BusinessException;
import com.knu.sosuso.capstone.global.exception.error.AuthenticationError;
import com.knu.sosuso.capstone.global.security.CustomOAuth2User;
import com.knu.sosuso.capstone.global.security.GoogleUserInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtAuthHelper 단위 테스트")
class JwtAuthHelperTest {

    private JwtAuthHelper jwtAuthHelper;
    private CustomOAuth2User mockOAuth2User;
    private GoogleUserInfo googleUserInfo;

    @BeforeEach
    void setUp() {
        jwtAuthHelper = new JwtAuthHelper();

        googleUserInfo = new GoogleUserInfo(
                "google-sub-123",
                "test@example.com",
                "테스트유저",
                "ROLE_USER",
                "https://example.com/picture.jpg"
        );

        mockOAuth2User = new CustomOAuth2User(googleUserInfo, 1L);
    }

    @AfterEach
    void tearDown() {
        // 각 테스트 후 SecurityContext 초기화
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("getCurrentUser 메서드는")
    class GetCurrentUserTest {

        @Test
        @DisplayName("인증된 사용자 정보를 반환한다")
        void getCurrentUser_Authenticated_ReturnsUser() {
            // given
            setAuthentication(mockOAuth2User);

            // when
            CustomOAuth2User result = jwtAuthHelper.getCurrentUser();

            // then
            assertThat(result).isNotNull();
            assertThat(result.getSub()).isEqualTo("google-sub-123");
            assertThat(result.getName()).isEqualTo("테스트유저");
            assertThat(result.getUserId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("인증되지 않은 경우 null을 반환한다")
        void getCurrentUser_NotAuthenticated_ReturnsNull() {
            // given
            SecurityContextHolder.clearContext();

            // when
            CustomOAuth2User result = jwtAuthHelper.getCurrentUser();

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Principal이 CustomOAuth2User가 아니면 null을 반환한다")
        void getCurrentUser_WrongPrincipalType_ReturnsNull() {
            // given
            Authentication auth = new UsernamePasswordAuthenticationToken("wrongType", null);
            SecurityContextHolder.getContext().setAuthentication(auth);

            // when
            CustomOAuth2User result = jwtAuthHelper.getCurrentUser();

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("isAuthenticated 메서드는")
    class IsAuthenticatedTest {

        @Test
        @DisplayName("인증된 사용자면 true를 반환한다")
        void isAuthenticated_Authenticated_ReturnsTrue() {
            // given
            setAuthentication(mockOAuth2User);

            // when
            boolean result = jwtAuthHelper.isAuthenticated();

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("인증되지 않은 사용자면 false를 반환한다")
        void isAuthenticated_NotAuthenticated_ReturnsFalse() {
            // given
            SecurityContextHolder.clearContext();

            // when
            boolean result = jwtAuthHelper.isAuthenticated();

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getAuthenticatedUser 메서드는")
    class GetAuthenticatedUserTest {

        @Test
        @DisplayName("인증된 사용자 정보를 반환한다")
        void getAuthenticatedUser_Authenticated_ReturnsUser() {
            // given
            setAuthentication(mockOAuth2User);

            // when
            CustomOAuth2User result = jwtAuthHelper.getAuthenticatedUser();

            // then
            assertThat(result).isNotNull();
            assertThat(result.getSub()).isEqualTo("google-sub-123");
        }

        @Test
        @DisplayName("인증되지 않은 경우 UNAUTHORIZED 예외를 던진다")
        void getAuthenticatedUser_NotAuthenticated_ThrowsException() {
            // given
            SecurityContextHolder.clearContext();

            // when & then
            assertThatThrownBy(() -> jwtAuthHelper.getAuthenticatedUser())
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("httpStatus", AuthenticationError.UNAUTHORIZED.getHttpStatus())
                    .hasMessageContaining(AuthenticationError.UNAUTHORIZED.getMessage());
        }
    }

    @Nested
    @DisplayName("getCurrentUserRole 메서드는")
    class GetCurrentUserRoleTest {

        @Test
        @DisplayName("인증된 사용자의 role을 반환한다")
        void getCurrentUserRole_Authenticated_ReturnsRole() {
            // given
            setAuthentication(mockOAuth2User);

            // when
            String result = jwtAuthHelper.getCurrentUserRole();

            // then
            assertThat(result).isEqualTo("ROLE_USER");
        }

        @Test
        @DisplayName("인증되지 않은 경우 null을 반환한다")
        void getCurrentUserRole_NotAuthenticated_ReturnsNull() {
            // given
            SecurityContextHolder.clearContext();

            // when
            String result = jwtAuthHelper.getCurrentUserRole();

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("getCurrentUserSub 메서드는")
    class GetCurrentUserSubTest {

        @Test
        @DisplayName("인증된 사용자의 sub를 반환한다")
        void getCurrentUserSub_Authenticated_ReturnsSub() {
            // given
            setAuthentication(mockOAuth2User);

            // when
            String result = jwtAuthHelper.getCurrentUserSub();

            // then
            assertThat(result).isEqualTo("google-sub-123");
        }

        @Test
        @DisplayName("인증되지 않은 경우 null을 반환한다")
        void getCurrentUserSub_NotAuthenticated_ReturnsNull() {
            // given
            SecurityContextHolder.clearContext();

            // when
            String result = jwtAuthHelper.getCurrentUserSub();

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("getCurrentUserId 메서드는")
    class GetCurrentUserIdTest {

        @Test
        @DisplayName("인증된 사용자의 userId를 반환한다")
        void getCurrentUserId_Authenticated_ReturnsUserId() {
            // given
            setAuthentication(mockOAuth2User);

            // when
            Long result = jwtAuthHelper.getCurrentUserId();

            // then
            assertThat(result).isEqualTo(1L);
        }

        @Test
        @DisplayName("인증되지 않은 경우 null을 반환한다")
        void getCurrentUserId_NotAuthenticated_ReturnsNull() {
            // given
            SecurityContextHolder.clearContext();

            // when
            Long result = jwtAuthHelper.getCurrentUserId();

            // then
            assertThat(result).isNull();
        }
    }

    private void setAuthentication(CustomOAuth2User user) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}