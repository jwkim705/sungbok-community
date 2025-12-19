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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationFailHandler implements AuthenticationFailureHandler {

  private final ObjectMapper objectMapper;

  @Override
  public void onAuthenticationFailure(@Nullable HttpServletRequest request, HttpServletResponse response,
      AuthenticationException exception) throws IOException {

    log.info("Fail AuthenticationFailHandler Login Message: {}", exception.getMessage());

    // ProblemDetail 생성 (RFC 7807)
    String traceId = UUID.randomUUID().toString();
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
        HttpStatus.UNAUTHORIZED,
        AuthErrorCode.INVALID_CREDENTIALS.getMessage()
    );
    problem.setType(URI.create("https://sungbok.p-e.kr/errors/invalid_credentials"));
    problem.setTitle(AuthErrorCode.INVALID_CREDENTIALS.name());
    if (request != null) {
      problem.setInstance(URI.create(request.getRequestURI()));
    }
    problem.setProperty("timestamp", Instant.now().toString());
    problem.setProperty("traceId", traceId);
    problem.setProperty("code", AuthErrorCode.INVALID_CREDENTIALS.getCode());

    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setCharacterEncoding("UTF-8");
    response.setContentType("application/problem+json");
    log.error("[traceId={}] Authentication failed: {}", traceId, exception.getMessage(), exception);

    objectMapper.writeValue(response.getWriter(), problem);
  }

}
