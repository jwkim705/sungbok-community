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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPointHandler implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  @Override
  public void commence(@Nullable HttpServletRequest request, HttpServletResponse response, @Nullable AuthenticationException authException) throws IOException {

    String errorMessage = authException != null ? authException.getMessage() : "Unauthorized";
    log.info("Fail Authentication EntryPoint Message: {}", errorMessage);

    response.setStatus(HttpStatus.UNAUTHORIZED.value());

    ErrorResponseDTO responseDTO = ErrorResponseDTO.of(HttpStatus.UNAUTHORIZED.value(), errorMessage);
    response.setCharacterEncoding("UTF-8");
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    if (authException != null) {
      log.error(authException.getMessage(), authException);
    }

    objectMapper.writeValue(response.getWriter(), responseDTO);
  }
}
