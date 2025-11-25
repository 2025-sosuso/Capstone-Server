package com.knu.sosuso.capstone.domain.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("UserRepository 통합 테스트")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("findBySub 메서드는")
    class FindBySubTest {

        @Test
        @DisplayName("sub로 사용자를 조회한다")
        void findBySub_Exists_ReturnsUser() {
            // given
            User user = createUser("google-sub-123", "테스트유저");
            userRepository.save(user);

            // when
            User found = userRepository.findBySub("google-sub-123");

            // then
            assertThat(found).isNotNull();
            assertThat(found.getSub()).isEqualTo("google-sub-123");
            assertThat(found.getName()).isEqualTo("테스트유저");
        }

        @Test
        @DisplayName("존재하지 않는 sub면 null을 반환한다")
        void findBySub_NotExists_ReturnsNull() {
            // when
            User found = userRepository.findBySub("not-exists");

            // then
            assertThat(found).isNull();
        }

        @Test
        @DisplayName("여러 사용자 중 정확한 사용자를 조회한다")
        void findBySub_MultipleUsers_ReturnsCorrectOne() {
            // given
            User user1 = createUser("sub-1", "유저1");
            User user2 = createUser("sub-2", "유저2");
            User user3 = createUser("sub-3", "유저3");
            userRepository.save(user1);
            userRepository.save(user2);
            userRepository.save(user3);

            // when
            User found = userRepository.findBySub("sub-2");

            // then
            assertThat(found).isNotNull();
            assertThat(found.getSub()).isEqualTo("sub-2");
            assertThat(found.getName()).isEqualTo("유저2");
        }
    }

    @Nested
    @DisplayName("save 메서드는")
    class SaveTest {

        @Test
        @DisplayName("새로운 사용자를 저장한다")
        void save_NewUser_SavesSuccessfully() {
            // given
            User user = createUser("new-sub", "새로운유저");

            // when
            User saved = userRepository.save(user);

            // then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getSub()).isEqualTo("new-sub");
        }

        @Test
        @DisplayName("사용자 정보를 업데이트한다")
        void save_ExistingUser_UpdatesSuccessfully() {
            // given
            User user = createUser("test-sub", "원래이름");
            User saved = userRepository.save(user);

            // when
            User found = userRepository.findBySub("test-sub");
            // signIn 메서드로 정보 업데이트 (Google 로그인 시나리오)
            com.knu.sosuso.capstone.global.security.GoogleUserInfo googleUserInfo =
                    new com.knu.sosuso.capstone.global.security.GoogleUserInfo(
                            "test-sub",
                            "updated@example.com",
                            "업데이트된이름",
                            "ROLE_USER",
                            "new-picture.jpg"
                    );
            found.signIn(googleUserInfo);
            userRepository.save(found);

            // then
            User updated = userRepository.findBySub("test-sub");
            assertThat(updated.getName()).isEqualTo("업데이트된이름");
            assertThat(updated.getEmail()).isEqualTo("updated@example.com");
        }
    }

    // ========== Helper Methods ==========

    private User createUser(String sub, String name) {
        return User.builder()
                .sub(sub)
                .email(sub + "@example.com")
                .name(name)
                .role("ROLE_USER")
                .picture("https://example.com/picture.jpg")
                .build();
    }
}
