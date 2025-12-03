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

    response.setStatus(HttpStatus.UNAUTHORIZED.value());

    ErrorResponseDTO responseDTO = ErrorResponseDTO.of(HttpStatus.UNAUTHORIZED.value(), exception.getMessage());
    response.setCharacterEncoding("UTF-8");
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    log.error(exception.getMessage(), exception);

    objectMapper.writeValue(response.getWriter(), responseDTO);

  }

}
