package com.sungbok.community.security.util;

import com.sungbok.community.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 필터 레벨 에러 응답 유틸리티
 * JwtAuthenticationFilter 및 TenantResolver에서 RFC 7807 ProblemDetail 형식의 에러 응답 생성
 *
 * ⚠️ 중요: 필터 레벨에서는 GlobalExceptionHandler를 사용할 수 없으므로,
 * 직접 ProblemDetail 형식의 JSON을 작성해서 응답합니다.
 *
 * @since 1.1.0
 */
@Slf4j
public class FilterErrorResponseUtil {

    private static final String ERROR_BASE_URL = "https://sungbok.p-e.kr/errors/";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * RFC 7807 ProblemDetail 형식의 에러 응답 작성
     *
     * @param response    HttpServletResponse
     * @param errorCode   ErrorCode
     * @param requestURI  요청 URI
     * @throws IOException JSON 작성 실패
     */
    public static void writeErrorResponse(
            HttpServletResponse response,
            ErrorCode errorCode,
            String requestURI
    ) throws IOException {
        String traceId = generateOrGetTraceId();

        // RFC 7807 ProblemDetail 구조
        Map<String, Object> problemDetail = new HashMap<>();
        problemDetail.put("type", ERROR_BASE_URL + errorCode.name().toLowerCase());
        problemDetail.put("title", errorCode.name());
        problemDetail.put("status", errorCode.getHttpStatus().value());
        problemDetail.put("detail", errorCode.getMessage());
        problemDetail.put("instance", requestURI);
        problemDetail.put("timestamp", Instant.now().toString());
        problemDetail.put("traceId", traceId);
        problemDetail.put("code", errorCode.getCode());

        // HTTP 응답 설정
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType("application/problem+json; charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(problemDetail));
        response.getWriter().flush();

        log.warn("[traceId={}] Filter error response: code={}, path={}",
                traceId, errorCode.getCode(), requestURI);
    }

    /**
     * TraceId 생성 또는 MDC에서 가져오기
     *
     * @return TraceId
     */
    private static String generateOrGetTraceId() {
        String traceId = MDC.get("traceId");
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
            MDC.put("traceId", traceId);
        }
        return traceId;
    }
}
