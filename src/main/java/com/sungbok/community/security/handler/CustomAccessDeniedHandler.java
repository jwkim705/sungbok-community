package com.sungbok.community.security.handler;

import com.sungbok.community.common.exception.code.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  @Override
  public void handle(@Nullable HttpServletRequest request, HttpServletResponse response,
      @Nullable AccessDeniedException accessDeniedException) throws IOException {

    String errorMessage = accessDeniedException != null ? accessDeniedException.getMessage() : "Access Denied";
    log.info("Fail AccessDeniedHandler Login Message: {}", errorMessage);

    // ProblemDetail 생성 (RFC 7807)
    String traceId = UUID.randomUUID().toString();
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
        HttpStatus.FORBIDDEN,
        AuthErrorCode.ACCESS_DENIED.getMessage()
    );
    problem.setType(URI.create("https://sungbok.p-e.kr/errors/access_denied"));
    problem.setTitle(AuthErrorCode.ACCESS_DENIED.name());
    if (request != null) {
      problem.setInstance(URI.create(request.getRequestURI()));
    }
    problem.setProperty("timestamp", Instant.now().toString());
    problem.setProperty("traceId", traceId);
    problem.setProperty("code", AuthErrorCode.ACCESS_DENIED.getCode());

    response.setStatus(HttpStatus.FORBIDDEN.value());
    response.setCharacterEncoding("UTF-8");
    response.setContentType("application/problem+json");

    if (accessDeniedException != null) {
      log.error("[traceId={}] Access denied: {}", traceId, accessDeniedException.getMessage(), accessDeniedException);
    }

    objectMapper.writeValue(response.getWriter(), problem);
  }
}
