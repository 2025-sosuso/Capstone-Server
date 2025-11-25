package com.knu.sosuso.capstone.domain.channel.repository;

import com.knu.sosuso.capstone.domain.auth.User;
import com.knu.sosuso.capstone.domain.auth.UserRepository;
import com.knu.sosuso.capstone.domain.channel.entity.FavoriteChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("FavoriteChannelRepository 통합 테스트")
class FavoriteChannelRepositoryTest {

    @Autowired
    private FavoriteChannelRepository favoriteChannelRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        favoriteChannelRepository.deleteAll();
        userRepository.deleteAll();

        testUser = createAndSaveUser("test-sub-123", "테스트유저");
    }

    @Nested
    @DisplayName("existsByUserIdAndApiChannelId 메서드는")
    class ExistsByUserIdAndApiChannelIdTest {

        @Test
        @DisplayName("사용자가 채널을 구독했으면 true를 반환한다")
        void existsByUserAndChannel_Exists_ReturnsTrue() {
            // given
            FavoriteChannel favorite = createFavoriteChannel(testUser, "channel-123", "테스트채널");
            favoriteChannelRepository.save(favorite);

            // when
            boolean exists = favoriteChannelRepository.existsByUserIdAndApiChannelId(
                    testUser.getId(),
                    "channel-123"
            );

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("구독하지 않았으면 false를 반환한다")
        void existsByUserAndChannel_NotExists_ReturnsFalse() {
            // when
            boolean exists = favoriteChannelRepository.existsByUserIdAndApiChannelId(
                    testUser.getId(),
                    "not-subscribed-channel"
            );

            // then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("findByUserIdAndApiChannelId 메서드는")
    class FindByUserIdAndApiChannelIdTest {

        @Test
        @DisplayName("사용자의 관심 채널을 조회한다")
        void findByUserAndChannel_Exists_ReturnsChannel() {
            // given
            FavoriteChannel favorite = createFavoriteChannel(testUser, "channel-123", "테스트채널");
            favoriteChannelRepository.save(favorite);

            // when
            Optional<FavoriteChannel> found = favoriteChannelRepository.findByUserIdAndApiChannelId(
                    testUser.getId(),
                    "channel-123"
            );

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getApiChannelId()).isEqualTo("channel-123");
            assertThat(found.get().getApiChannelName()).isEqualTo("테스트채널");
        }

        @Test
        @DisplayName("존재하지 않으면 빈 Optional을 반환한다")
        void findByUserAndChannel_NotExists_ReturnsEmpty() {
            // when
            Optional<FavoriteChannel> found = favoriteChannelRepository.findByUserIdAndApiChannelId(
                    testUser.getId(),
                    "not-exists"
            );

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByUserId 메서드는")
    class FindByUserIdTest {

        @Test
        @DisplayName("사용자의 모든 관심 채널을 조회한다")
        void findByUserId_WithMultipleChannels_ReturnsAll() {
            // given
            FavoriteChannel channel1 = createFavoriteChannel(testUser, "channel-1", "채널1");
            FavoriteChannel channel2 = createFavoriteChannel(testUser, "channel-2", "채널2");
            FavoriteChannel channel3 = createFavoriteChannel(testUser, "channel-3", "채널3");
            favoriteChannelRepository.saveAll(List.of(channel1, channel2, channel3));

            // when
            List<FavoriteChannel> found = favoriteChannelRepository.findByUserId(testUser.getId());

            // then
            assertThat(found).hasSize(3);
            assertThat(found).extracting(FavoriteChannel::getApiChannelId)
                    .containsExactlyInAnyOrder("channel-1", "channel-2", "channel-3");
        }

        @Test
        @DisplayName("관심 채널이 없으면 빈 리스트를 반환한다")
        void findByUserId_NoChannels_ReturnsEmpty() {
            // when
            List<FavoriteChannel> found = favoriteChannelRepository.findByUserId(testUser.getId());

            // then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("다른 사용자의 관심 채널은 조회되지 않는다")
        void findByUserId_OtherUserChannels_NotIncluded() {
            // given
            User otherUser = createAndSaveUser("other-sub", "다른유저");
            FavoriteChannel otherFavorite = createFavoriteChannel(otherUser, "channel-999", "다른채널");
            favoriteChannelRepository.save(otherFavorite);

            // when
            List<FavoriteChannel> found = favoriteChannelRepository.findByUserId(testUser.getId());

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByIdAndUserId 메서드는")
    class FindByIdAndUserIdTest {

        @Test
        @DisplayName("ID와 사용자 ID로 관심 채널을 조회한다")
        void findByIdAndUserId_Exists_ReturnsChannel() {
            // given
            FavoriteChannel favorite = createFavoriteChannel(testUser, "channel-123", "테스트채널");
            FavoriteChannel saved = favoriteChannelRepository.save(favorite);

            // when
            Optional<FavoriteChannel> found = favoriteChannelRepository.findByIdAndUserId(
                    saved.getId(),
                    testUser.getId()
            );

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(saved.getId());
        }

        @Test
        @DisplayName("다른 사용자의 채널은 조회되지 않는다")
        void findByIdAndUserId_OtherUser_ReturnsEmpty() {
            // given
            User otherUser = createAndSaveUser("other-sub", "다른유저");
            FavoriteChannel otherFavorite = createFavoriteChannel(otherUser, "channel-999", "다른채널");
            FavoriteChannel saved = favoriteChannelRepository.save(otherFavorite);

            // when
            Optional<FavoriteChannel> found = favoriteChannelRepository.findByIdAndUserId(
                    saved.getId(),
                    testUser.getId()
            );

            // then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 ID면 빈 Optional을 반환한다")
        void findByIdAndUserId_NotExists_ReturnsEmpty() {
            // when
            Optional<FavoriteChannel> found = favoriteChannelRepository.findByIdAndUserId(
                    999L,
                    testUser.getId()
            );

            // then
            assertThat(found).isEmpty();
        }
    }

    // ========== Helper Methods ==========

    private User createAndSaveUser(String sub, String name) {
        User user = User.builder()
                .sub(sub)
                .email(sub + "@example.com")
                .name(name)
                .role("ROLE_USER")
                .picture("https://example.com/picture.jpg")
                .build();
        return userRepository.save(user);
    }

    private FavoriteChannel createFavoriteChannel(User user, String channelId, String channelName) {
        return FavoriteChannel.builder()
                .user(user)
                .apiChannelId(channelId)
                .apiChannelName(channelName)
                .apiChannelThumbnail("https://example.com/channel.jpg")
                .build();
    }
}