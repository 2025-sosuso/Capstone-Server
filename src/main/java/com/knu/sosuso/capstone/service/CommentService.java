package com.knu.sosuso.capstone.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knu.sosuso.capstone.config.ApiConfig;
import com.knu.sosuso.capstone.domain.Comment;
import com.knu.sosuso.capstone.domain.enums.Emotion;
import com.knu.sosuso.capstone.dto.response.CommentApiResponse;
import com.knu.sosuso.capstone.dto.response.CommentApiResponse.CommentData;
import com.knu.sosuso.capstone.repository.CommentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private static final Logger logger = LoggerFactory.getLogger(CommentService.class);
    private static final String YOUTUBE_COMMENT_API_URL = "https://www.googleapis.com/youtube/v3/commentThreads";
    private static final int MAX_RESULTS_PER_REQUEST = 100;
    private static final Pattern HOUR_PATTERN = Pattern.compile("\\b(\\d{1,2}):(\\d{2}):(\\d{2})\\b");
    private static final Pattern MINUTE_PATTERN = Pattern.compile("\\b(\\d{1,2}):(\\d{2})\\b");

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final ObjectMapper objectMapper;
    private final CommentRepository commentRepository;

    public CommentService(ApiConfig config, RestTemplate restTemplate, CommentRepository commentRepository) {
        this.apiKey = config.getKey();
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
        this.commentRepository = commentRepository;
    }

    public CommentApiResponse getCommentInfo(String videoId) {
        if (videoId == null || videoId.trim().isEmpty()) {
            throw new IllegalArgumentException("비디오 ID는 필수입니다");
        }

        try {
            logger.info("댓글 정보 조회 시작: videoId={}", videoId);

            List<CommentData> allComments = fetchAllComments(videoId.trim());
            allComments.sort(Comparator.comparingInt(CommentData::likeCount).reversed());

            Map<Integer, Integer> hourlyDistribution = analyzeHourlyDistribution(allComments);
            Map<String, Integer> mentionedTimestamp = analyzeMentionedTimestamp(allComments);

            logger.info("댓글 정보 조회 완료: videoId={}, 댓글 수={}", videoId, allComments.size());
            return new CommentApiResponse(hourlyDistribution, mentionedTimestamp, allComments);

        } catch (HttpClientErrorException.Forbidden e) {
            logger.warn("댓글 접근 금지: videoId={}", videoId);
            // 빈 댓글 리스트 반환 (댓글 비활성화는 정상적인 상황)
            return new CommentApiResponse(new HashMap<>(), new HashMap<>(), new ArrayList<>());
        } catch (HttpClientErrorException.NotFound e) {
            logger.warn("비디오를 찾을 수 없음: videoId={}", videoId);
            throw new IllegalArgumentException("존재하지 않는 비디오입니다", e);

        } catch (RestClientException e) {
            logger.error("YouTube API 호출 실패: videoId={}, error={}", videoId, e.getMessage(), e);
            throw new IllegalStateException("댓글 정보를 가져올 수 없습니다", e);

        } catch (Exception e) {
            logger.error("댓글 정보 조회 실패: videoId={}, error={}", videoId, e.getMessage(), e);
            throw new RuntimeException("댓글 정보 조회 중 오류 발생", e);
        }
    }

    /**
     * 댓글 조회 + DB 저장 (통합 메서드)
     */
    public CommentApiResponse getCommentInfoAndSave(String videoId) {
        CommentApiResponse commentResponse = getCommentInfo(videoId);

        try {
            int savedCount = saveCommentsToDb(videoId, commentResponse.allComments());
            logger.info("댓글 DB 저장 완료: videoId={}, 저장된 개수={}", videoId, savedCount);
        } catch (Exception e) {
            logger.error("댓글 DB 저장 실패: videoId={}, error={}", videoId, e.getMessage());
        }

        return commentResponse;
    }

    /**
     * 댓글 리스트를 DB에 저장
     */
    @Transactional
    public int saveCommentsToDb(String videoId, List<CommentData> comments) {
        if (videoId == null || videoId.trim().isEmpty()) {
            throw new IllegalArgumentException("비디오 ID는 필수입니다");
        }

        if (comments == null || comments.isEmpty()) {
            logger.info("저장할 댓글이 없습니다: videoId={}", videoId);
            return 0;
        }

        logger.info("댓글 DB 저장 시작: videoId={}, 댓글수={}", videoId, comments.size());

        try {
            List<Comment> commentsToSave = comments.stream()
                    .map(commentData -> Comment.builder()
                            .videoId(videoId)
                            .commentId(commentData.id())
                            .commentContent(commentData.commentText())
                            .likeCount(commentData.likeCount())
                            .emotion(Emotion.other) // 임시로 other 설정
                            .writer(commentData.authorName())
                            .writtenAt(commentData.publishedAt())
                            .build())
                    .filter(comment -> !commentRepository.existsByCommentId(comment.getCommentId()))
                    .collect(Collectors.toList());

            List<Comment> savedComments = commentRepository.saveAll(commentsToSave);

            logger.info("댓글 DB 저장 완료: videoId={}, 저장={}, 중복 제외={}",
                    videoId, savedComments.size(), comments.size() - savedComments.size());

            return savedComments.size();

        } catch (Exception e) {
            logger.error("댓글 DB 저장 실패: videoId={}, error={}", videoId, e.getMessage(), e);
            throw new RuntimeException("댓글 저장 중 오류 발생", e);
        }
    }

    /**
     * 기존 댓글 삭제 후 새로 저장
     */
    @Transactional
    public int replaceCommentsInDb(String videoId, List<CommentData> comments) {
        logger.info("댓글 교체 시작: videoId={}", videoId);

        // 기존 댓글 삭제
        commentRepository.deleteByVideoId(videoId);

        // 새 댓글 저장
        return saveCommentsToDb(videoId, comments);
    }

    /**
     * DB에서 댓글 조회
     */
    @Transactional(readOnly = true)
    public CommentApiResponse getCommentsFromDb(String videoId) {
        List<Comment> dbComments = commentRepository.findByVideoIdOrderByLikeCountDesc(videoId);

        List<CommentData> commentDataList = dbComments.stream()
                .map(comment -> new CommentData(
                        comment.getCommentId(),
                        comment.getWriter(),
                        comment.getCommentContent(),
                        comment.getLikeCount(),
                        comment.getEmotion().name().toLowerCase(), // enum을 문자열로 변환
                        comment.getWrittenAt()
                ))
                .collect(Collectors.toList());

        Map<Integer, Integer> hourlyDistribution = analyzeHourlyDistribution(commentDataList);
        Map<String, Integer> mentionedTimestamp = analyzeMentionedTimestamp(commentDataList);

        return new CommentApiResponse(hourlyDistribution, mentionedTimestamp, commentDataList);
    }

    private List<CommentData> fetchAllComments(String videoId) {
        List<CommentData> allComments = new ArrayList<>();
        String pageToken = null;
        int pageCount = 0;

        do {
            String apiUrl = buildApiUrl(videoId, pageToken);
            String jsonResponse = callYouTubeApi(apiUrl);

            try {
                JsonNode rootNode = objectMapper.readTree(jsonResponse);
                JsonNode itemsNode = rootNode.path("items");

                if (!itemsNode.isArray() || itemsNode.isEmpty()) {
                    break;
                }
                List<CommentData> pageComments = parseCommentsFromJson(itemsNode);
                allComments.addAll(pageComments);

                pageToken = rootNode.path("nextPageToken").asText();
                if (pageToken.isEmpty()) {
                    pageToken = null;
                }

                pageCount++;

                if (pageCount % 10 == 0) {
                    logger.info("댓글 수집 진행: videoId={}, 페이지={}, 누적={}", videoId, pageCount, allComments.size());
                }
            } catch (Exception e) {
                logger.error("JSON 파싱 실패: videoId={}, error={}", videoId, e.getMessage(), e);
                break;
            }

        } while (isValidPageToken(pageToken));

        return allComments;
    }

    private String buildApiUrl(String videoId, String pageToken) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(YOUTUBE_COMMENT_API_URL)
                .queryParam("part", "snippet")
                .queryParam("maxResults", MAX_RESULTS_PER_REQUEST)
                .queryParam("textFormat", "plainText")
                .queryParam("videoId", videoId)
                .queryParam("key", apiKey);

        if (isValidPageToken(pageToken)) {
            builder.queryParam("pageToken", pageToken);
        }

        return builder.build(false).toUriString();
    }

    private String callYouTubeApi(String apiUrl) {
        return restTemplate.getForObject(apiUrl, String.class);
    }

    private List<CommentData> parseCommentsFromJson(JsonNode itemsNode) {
        List<CommentData> comments = new ArrayList<>();

        for (JsonNode item : itemsNode) {
            CommentData commentData = extractCommentData(item);
            if (commentData != null) {
                comments.add(commentData);
            }
        }

        return comments;
    }

    private CommentData extractCommentData(JsonNode item) {
        try {
            JsonNode snippet = item.path("snippet");
            if (snippet.isMissingNode()) {
                return null;
            }

            JsonNode topLevelComment = snippet.path("topLevelComment");
            if (topLevelComment.isMissingNode()) {
                return null;
            }

            JsonNode commentSnippet = topLevelComment.path("snippet");
            if (commentSnippet.isMissingNode()) {
                return null;
            }

            String commentId = topLevelComment.path("id").asText();
            String authorName = commentSnippet.path("authorDisplayName").asText();
            String commentText = commentSnippet.path("textDisplay").asText();
            int likeCount = commentSnippet.path("likeCount").asInt(0);
            String publishedAt = commentSnippet.path("publishedAt").asText();

            // 추후 변동 예정
            String emotion = "other";

            return new CommentData(commentId, authorName, commentText, likeCount, emotion, publishedAt);

        } catch (Exception e) {
            logger.warn("댓글 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    private Map<Integer, Integer> analyzeHourlyDistribution(List<CommentData> comments) {
        Map<Integer, Integer> hourlyCount = new HashMap<>();

        // 0~23시 초기화
        for (int i = 0; i < 24; i++) {
            hourlyCount.put(i, 0);
        }

        ZoneId koreaZone = ZoneId.of("Asia/Seoul");

        for (CommentData comment : comments) {
            try {
                // YouTube API 시간 형식: "2025-05-24T16:10:53Z" (UTC)
                String timeString = comment.publishedAt();

                ZonedDateTime utcTime = ZonedDateTime.parse(timeString);
                ZonedDateTime koreaTime = utcTime.withZoneSameInstant(koreaZone);

                int hour = koreaTime.getHour();
                hourlyCount.put(hour, hourlyCount.get(hour) + 1);

                if (hourlyCount.values().stream().mapToInt(Integer::intValue).sum() <= 5) {
                    logger.info("시간 변환 예시: UTC={} -> KST={} ({}시)",
                            timeString, koreaTime.toString(), hour);
                }

            } catch (Exception e) {
                logger.warn("댓글 시간 파싱 실패: publishedAt={}, error={}",
                        comment.publishedAt(), e.getMessage());
            }
        }

        logger.info("시간대별 댓글 분포 분석 완료 (한국시간): 총 댓글 수={}", comments.size());
        return hourlyCount;
    }

    private Map<String, Integer> analyzeMentionedTimestamp(List<CommentData> comments) {
        Map<String, Integer> timestampCount = new HashMap<>();

        if (comments == null || comments.isEmpty()) {
            return new LinkedHashMap<>();
        }

        for (CommentData comment : comments) {
            Set<String> uniqueTimestamps = extractAllTimestamps(comment.commentText(), HOUR_PATTERN, MINUTE_PATTERN);

            if (uniqueTimestamps.isEmpty()) {
                continue;
            }

            for (String timestamp : uniqueTimestamps) {
                timestampCount.merge(timestamp, 1, Integer::sum);
            }
        }

        logger.info("시간대 언급 분석 완료: 총 {}개의 서로 다른 시간대가 언급됨", timestampCount.size());

        // 언급 횟수 기준으로 정렬하고 Top 5개 반환
        return timestampCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    // 모든 타임스탬프를 위치 기반으로 추출 (중복 자동 제거)
    private Set<String> extractAllTimestamps(String commentText, Pattern hourPattern, Pattern minutePattern) {
        Set<String> allTimestamps = new HashSet<>();

        // 빈 댓글 처리
        if (commentText == null || commentText.trim().isEmpty()) {
            return allTimestamps;
        }

        // 시:분:초 패턴 처리
        List<int[]> hourRanges = new ArrayList<>();
        Matcher hourMatcher = hourPattern.matcher(commentText);

        while (hourMatcher.find()) {
            try {
                int hours = Integer.parseInt(hourMatcher.group(1));
                int minutes = Integer.parseInt(hourMatcher.group(2));
                int seconds = Integer.parseInt(hourMatcher.group(3));

                // 유효성 검사 (시간은 24시간 제한 추가)
                if (hours <= 23 && minutes <= 59 && seconds <= 59) {
                    String timestamp = String.format("%d:%02d:%02d", hours, minutes, seconds);
                    allTimestamps.add(timestamp);
                    hourRanges.add(new int[]{hourMatcher.start(), hourMatcher.end()});
                }
            } catch (NumberFormatException e) {
                continue;
            }
        }

        // 분:초 패턴 처리
        Matcher minuteMatcher = minutePattern.matcher(commentText);

        while (minuteMatcher.find()) {
            try {
                int minutes = Integer.parseInt(minuteMatcher.group(1));
                int seconds = Integer.parseInt(minuteMatcher.group(2));

                // 유효성 검사 강화 (분도 99분 제한)
                if (minutes <= 99 && seconds <= 59) {
                    int start = minuteMatcher.start();
                    int end = minuteMatcher.end();

                    boolean isOverlapping = false;
                    for (int[] range : hourRanges) {
                        if (!(end <= range[0] || start >= range[1])) {
                            isOverlapping = true;
                            break;
                        }
                    }

                    if (!isOverlapping) {
                        allTimestamps.add(minuteMatcher.group());
                    }
                }
            } catch (NumberFormatException e) {
                continue;
            }
        }

        return allTimestamps;
    }

    private boolean isValidPageToken(String pageToken) {
        return Optional.ofNullable(pageToken)
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .isPresent();
    }
}