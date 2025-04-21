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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationFailHandler implements AuthenticationFailureHandler {

  private final MappingJackson2HttpMessageConverter defaultJacksonConverter;

  @Override
  public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException exception) throws IOException, ServletException {

    log.info("Fail AuthenticationFailHandler Login Message: {}", exception.getMessage());

    response.setStatus(HttpStatus.UNAUTHORIZED.value());

    ErrorResponseDTO responseDTO = ErrorResponseDTO.of(HttpStatus.UNAUTHORIZED.value(), exception.getMessage());
    response.setCharacterEncoding("UTF-8");
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    log.error(exception.getMessage(), exception);

    defaultJacksonConverter.getObjectMapper().writeValue(response.getWriter(), responseDTO);
  }

}
