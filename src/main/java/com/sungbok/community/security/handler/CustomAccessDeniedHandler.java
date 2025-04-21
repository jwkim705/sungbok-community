package com.sungbok.community.security.handler;

import com.sungbok.community.common.dto.ErrorResponseDTO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

  private final MappingJackson2HttpMessageConverter defaultJacksonConverter;

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response,
      AccessDeniedException accessDeniedException) throws IOException, ServletException {

    log.info("Fail AccessDeniedHandler Login Message: {}", accessDeniedException.getMessage());

    response.setStatus(HttpStatus.FORBIDDEN.value());

    ErrorResponseDTO responseDTO = ErrorResponseDTO.of(HttpStatus.FORBIDDEN.value(), accessDeniedException.getMessage());
    response.setCharacterEncoding("UTF-8");
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    log.error(accessDeniedException.getMessage(), accessDeniedException);

    defaultJacksonConverter.getObjectMapper().writeValue(response.getWriter(), responseDTO);

  }
}
