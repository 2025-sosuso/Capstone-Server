package com.knu.sosuso.capstone.ai.service;

import com.knu.sosuso.capstone.ai.dto.AIAnalysisRequest;
import com.knu.sosuso.capstone.ai.dto.AIAnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

@Slf4j
@RequiredArgsConstructor
@Service
public class AnalysisService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String FASTAPI_URL = "https://935f-34-125-218-69.ngrok-free.app/analyze";

    static {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                    }
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * AI에 분석 요청
     * @param aiAnalysisRequest
     * @return
     */
    public AIAnalysisResponse requestAnalysis(AIAnalysisRequest aiAnalysisRequest) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<AIAnalysisRequest> entity = new HttpEntity<>(aiAnalysisRequest, headers);

        try {
            log.info("FastAPI로 AI 분석 결과 요청 중");
            ResponseEntity<AIAnalysisResponse> aiAnalysisResponse = restTemplate.postForEntity(
                    FASTAPI_URL,
                    entity,
                    AIAnalysisResponse.class
            );
            log.info("FastAPI 응답 수신 상태: {}", aiAnalysisResponse.getStatusCode());

            if (aiAnalysisResponse.getStatusCode() == HttpStatus.OK && aiAnalysisResponse.getBody() != null) {
                return aiAnalysisResponse.getBody();
            } else {
                throw new RuntimeException("FastAPI request failed: " + aiAnalysisResponse.getStatusCode());
            }

        } catch (Exception e) {
            log.error("FastAPI 요청 중 예외 발생", e);
            throw new RuntimeException("FastAPI 호출 실패", e);
        }
    }
}