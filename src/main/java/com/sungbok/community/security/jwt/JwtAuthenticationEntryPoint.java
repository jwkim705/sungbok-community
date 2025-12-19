package com.sungbok.community.security.jwt;

import com.sungbok.community.common.exception.code.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;

/**
 * JWT 인증 실패 시 401 Unauthorized 응답 처리
 * CustomAuthenticationEntryPointHandler를 대체
 *
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.error("인증 실패 - 401 Unauthorized: {} ({})", authException.getMessage(), request.getRequestURI());

        // ProblemDetail 생성 (RFC 7807)
        String traceId = UUID.randomUUID().toString();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "인증이 필요합니다. 유효한 JWT 토큰을 제공해주세요."
        );
        problem.setType(URI.create("https://sungbok.p-e.kr/errors/invalid_token"));
        problem.setTitle(AuthErrorCode.INVALID_TOKEN.name());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now().toString());
        problem.setProperty("traceId", traceId);
        problem.setProperty("code", AuthErrorCode.INVALID_TOKEN.getCode());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        log.error("[traceId={}] JWT authentication failed", traceId);
        objectMapper.writeValue(response.getWriter(), problem);
    }
}
