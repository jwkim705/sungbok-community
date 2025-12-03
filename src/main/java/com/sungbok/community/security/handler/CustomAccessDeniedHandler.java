package com.sungbok.community.security.handler;

import com.sungbok.community.common.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    response.setStatus(HttpStatus.FORBIDDEN.value());

    ErrorResponseDTO responseDTO = ErrorResponseDTO.of(HttpStatus.FORBIDDEN.value(), errorMessage);
    response.setCharacterEncoding("UTF-8");
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    if (accessDeniedException != null) {
      log.error(accessDeniedException.getMessage(), accessDeniedException);
    }

    objectMapper.writeValue(response.getWriter(), responseDTO);

  }
}
