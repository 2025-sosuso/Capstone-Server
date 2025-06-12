package com.knu.sosuso.capstone.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knu.sosuso.capstone.ai.dto.AIAnalysisResponse;
import com.knu.sosuso.capstone.config.ApiConfig;
import com.knu.sosuso.capstone.domain.Comment;
import com.knu.sosuso.capstone.domain.Video;
import com.knu.sosuso.capstone.dto.response.CommentApiResponse;
import com.knu.sosuso.capstone.dto.response.CommentApiResponse.CommentData;
import com.knu.sosuso.capstone.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class CommentService {

    private static final String YOUTUBE_COMMENT_API_URL = "https://www.googleapis.com/youtube/v3/commentThreads";
    private static final Pattern HOUR_PATTERN = Pattern.compile("\\b(\\d{1,2}):(\\d{2}):(\\d{2})\\b");
    private static final Pattern MINUTE_PATTERN = Pattern.compile("\\b(\\d{1,2}):(\\d{2})\\b");
    private static final int MAX_RESULTS_PER_REQUEST = 100;
    private static final int MAX_TOTAL_COMMENTS = 300;

    private final ApiConfig apiConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final CommentRepository commentRepository;

    /**
     * 관련도순으로 댓글 가져오기 (YouTube API 호출)
     */
    public List<CommentData> fetchAllComments(String apiVideoId) {
        List<CommentData> allComments = new ArrayList<>();
        String pageToken = null;
        int pageCount = 0;

        try {
            do {
                String apiUrl = buildApiUrl(apiVideoId, pageToken);
                String jsonResponse = callYouTubeApi(apiUrl);
                JsonNode rootNode = objectMapper.readTree(jsonResponse);
                JsonNode itemsNode = rootNode.path("items");

                if (!itemsNode.isArray() || itemsNode.isEmpty()) {
                    break;
                }

                List<CommentData> pageComments = parseCommentsFromJson(itemsNode);
                allComments.addAll(pageComments);

                if (allComments.size() >= MAX_TOTAL_COMMENTS) {
                    log.info("댓글 수집 제한 도달: apiVideoId={}, 수집된 댓글 수={}", apiVideoId, allComments.size());
                    allComments = allComments.subList(0, Math.min(allComments.size(), MAX_TOTAL_COMMENTS));
                    break;
                }

                pageToken = rootNode.path("nextPageToken").asText();
                if (pageToken.isEmpty()) {
                    pageToken = null;
                }

                pageCount++;
                if (pageCount % 10 == 0) {
                    log.info("댓글 수집 진행: apiVideoId={}, 페이지={}, 누적={}", apiVideoId, pageCount, allComments.size());
                }

            } while (isValidPageToken(pageToken));

        } catch (HttpClientErrorException.Forbidden e) {
            // 댓글이 비활성화된 경우
            if (e.getResponseBodyAsString().contains("commentsDisabled")) {
                log.info("댓글이 비활성화된 영상: apiVideoId={}", apiVideoId);
                return new ArrayList<>(); // 빈 리스트 반환
            }
            throw e; // 다른 403 에러는 재던지기
        } catch (Exception e) {
            log.error("댓글 수집 실패: apiVideoId={}, error={}", apiVideoId, e.getMessage());
            throw new RuntimeException("댓글 수집 중 오류 발생", e);
        }

        log.info("댓글 수집 완료: apiVideoId={}, 총 댓글 수={}", apiVideoId, allComments.size());
        return allComments;
    }

    /**
     * 클라이언트용 댓글 응답 생성 (백엔드 분석)
     */
    public CommentApiResponse processCommentsForClient(List<CommentData> allComments) {
        if (allComments == null || allComments.isEmpty()) {
            return new CommentApiResponse(new HashMap<>(), new HashMap<>(), new ArrayList<>());
        }

        // 관련도 순서 그대로 유지
        List<CommentData> relevanceOrderedComments = new ArrayList<>(allComments);

        // 백엔드 분석 데이터 생성
        Map<Integer, Integer> commentHistogram = analyzeCommentHistogram(relevanceOrderedComments);
        Map<String, Integer> popularTimestamps = analyzePopularTimestamps(relevanceOrderedComments);

        log.info("클라이언트용 댓글 처리 완료: 댓글 수={}", relevanceOrderedComments.size());
        return new CommentApiResponse(commentHistogram, popularTimestamps, relevanceOrderedComments);
    }

    /**
     * AI 분석용 댓글 추출
     */
    public Map<String, String> extractCommentsForAI(List<CommentData> allComments) {
        if (allComments == null || allComments.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, String> commentsForAI = new HashMap<>();

        for (CommentData commentData : allComments) {
            commentsForAI.put(commentData.id(), commentData.commentText());
        }

        log.info("AI 분석용 댓글 추출: 전체={}, 추출={}", allComments.size(), commentsForAI.size());
        return commentsForAI;
    }

    /**
     * 댓글을 DB에 저장 (sentiment는 null)
     */
    @Transactional
    public void saveCommentsToDb(List<CommentData> comments, Video video) {
        if (comments == null || comments.isEmpty()) {
            log.info("저장할 댓글이 없습니다: apiVideoId={}", video.getApiVideoId());
        }

        log.info("댓글 DB 저장 시작: apiVideoId={}, 댓글수={}", video.getApiVideoId(), Objects.requireNonNull(comments).size());

        try {
            List<Comment> commentsToSave = comments.stream()
                    .map(commentData -> Comment.builder()
                            .video(video)
                            .apiCommentId(commentData.id())
                            .commentContent(commentData.commentText())
                            .likeCount(commentData.likeCount())
                            .sentimentType(null) // AI 분석 전이므로 null
                            .writer(commentData.authorName())
                            .writtenAt(commentData.publishedAt())
                            .build())
                    .filter(comment -> !commentRepository.existsByApiCommentId(comment.getApiCommentId()))
                    .collect(Collectors.toList());

            List<Comment> savedComments = commentRepository.saveAll(commentsToSave);

            log.info("댓글 DB 저장 완료: apiVideoId={}, 저장={}, 중복 제외={}",
                    video.getApiVideoId(), savedComments.size(), comments.size() - savedComments.size());

        } catch (Exception e) {
            log.error("댓글 DB 저장 실패: apiVideoId={}, error={}", video.getApiVideoId(), e.getMessage(), e);
            throw new RuntimeException("댓글 저장 중 오류 발생", e);
        }
    }

    /**
     * AI 분석 결과로 댓글 업데이트
     */
    @Transactional
    public void updateCommentsWithAnalysis(AIAnalysisResponse analysisResponse) {
        try {
            List<Comment> comments = commentRepository.findAllByVideoId(analysisResponse.videoId());

            for (Comment comment : comments) {
                if (analysisResponse.sentimentComments().containsKey(comment.getApiCommentId())) {
                    comment.setSentimentType(analysisResponse.sentimentComments().get(comment.getApiCommentId()));
                }
            }

            commentRepository.saveAll(comments);
            log.info("댓글 감정 분석 결과 업데이트 완료: videoId={}, 업데이트된 댓글 수={}",
                    analysisResponse.videoId(), comments.size());
        } catch (Exception e) {
            log.error("댓글 감정 분석 업데이트 실패: videoId={}, error={}",
                    analysisResponse.videoId(), e.getMessage());
            throw e;
        }
    }

    /**
     * 비디오 ID로 댓글 삭제 (1일 지난 데이터 삭제 시 사용)
     */
    @Transactional
    public void deleteCommentsByVideoId(Long videoId) {
        try {
            commentRepository.deleteByVideoId(videoId);
            log.info("댓글 삭제 완료: videoId={}", videoId);
        } catch (Exception e) {
            log.error("댓글 삭제 실패: videoId={}, error={}", videoId, e.getMessage());
            throw new RuntimeException("댓글 삭제 중 오류 발생", e);
        }
    }

    /**
     * DB에서 댓글 조회
     */
    @Transactional(readOnly = true)
    public CommentApiResponse getCommentsFromDb(Long videoId) {
        List<Comment> dbComments = commentRepository.findByVideoIdOrderByIdAsc(videoId);

        List<CommentData> commentDataList = dbComments.stream()
                .map(comment -> new CommentData(
                        comment.getApiCommentId(),
                        comment.getWriter(),
                        comment.getCommentContent(),
                        comment.getLikeCount(),
                        comment.getSentimentType() != null ? comment.getSentimentType().name().toLowerCase() : null,
                        comment.getWrittenAt()
                ))
                .collect(Collectors.toList());

        Map<Integer, Integer> commentHistogram = analyzeCommentHistogram(commentDataList);
        Map<String, Integer> popularTimestamps = analyzePopularTimestamps(commentDataList);

        return new CommentApiResponse(commentHistogram, popularTimestamps, commentDataList);
    }

    /**
     * 시간대별 댓글 분포 분석
     */
    public Map<Integer, Integer> analyzeCommentHistogram(List<CommentData> comments) {
        Map<Integer, Integer> hourlyCount = new HashMap<>();

        // 0~23시 초기화
        for (int i = 0; i < 24; i++) {
            hourlyCount.put(i, 0);
        }

        ZoneId koreaZone = ZoneId.of("Asia/Seoul");

        for (CommentData comment : comments) {
            try {
                String timeString = comment.publishedAt();
                ZonedDateTime utcTime = ZonedDateTime.parse(timeString);
                ZonedDateTime koreaTime = utcTime.withZoneSameInstant(koreaZone);

                int hour = koreaTime.getHour();
                hourlyCount.put(hour, hourlyCount.get(hour) + 1);

            } catch (Exception e) {
                log.warn("댓글 시간 파싱 실패: publishedAt={}, error={}",
                        comment.publishedAt(), e.getMessage());
            }
        }

        log.info("시간대별 댓글 분포 분석 완료 (한국시간): 총 댓글 수={}", comments.size());
        return hourlyCount;
    }

    /**
     * 타임스탬프 언급 분석
     */
    public Map<String, Integer> analyzePopularTimestamps(List<CommentData> comments) {
        Map<String, Integer> timestampCount = new HashMap<>();

        if (comments == null || comments.isEmpty()) {
            return new LinkedHashMap<>();
        }

        for (CommentData comment : comments) {
            Set<String> uniqueTimestamps = extractAllTimestamps(comment.commentText());

            if (uniqueTimestamps.isEmpty()) {
                continue;
            }

            for (String timestamp : uniqueTimestamps) {
                timestampCount.merge(timestamp, 1, Integer::sum);
            }
        }

        log.info("시간대 언급 분석 완료: 총 {}개의 서로 다른 시간대가 언급됨", timestampCount.size());

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

    private String buildApiUrl(String apiVideoId, String pageToken) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(YOUTUBE_COMMENT_API_URL)
                .queryParam("part", "snippet")
                .queryParam("maxResults", MAX_RESULTS_PER_REQUEST)
                .queryParam("textFormat", "plainText")
                .queryParam("order", "relevance")
                .queryParam("videoId", apiVideoId)
                .queryParam("key", apiConfig.getKey());

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

            String sentiment = null; // 초기값

            return new CommentData(commentId, authorName, commentText, likeCount, sentiment, publishedAt);

        } catch (Exception e) {
            log.warn("댓글 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    private Set<String> extractAllTimestamps(String commentText) {
        Set<String> allTimestamps = new HashSet<>();

        if (commentText == null || commentText.trim().isEmpty()) {
            return allTimestamps;
        }

        // 시:분:초 패턴 처리
        List<int[]> hourRanges = new ArrayList<>();
        Matcher hourMatcher = CommentService.HOUR_PATTERN.matcher(commentText);

        while (hourMatcher.find()) {
            try {
                int hours = Integer.parseInt(hourMatcher.group(1));
                int minutes = Integer.parseInt(hourMatcher.group(2));
                int seconds = Integer.parseInt(hourMatcher.group(3));

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
        Matcher minuteMatcher = CommentService.MINUTE_PATTERN.matcher(commentText);

        while (minuteMatcher.find()) {
            try {
                int minutes = Integer.parseInt(minuteMatcher.group(1));
                int seconds = Integer.parseInt(minuteMatcher.group(2));

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