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
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPointHandler implements AuthenticationEntryPoint {

  private final MappingJackson2HttpMessageConverter defaultJacksonConverter;

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws ServletException, IOException {

    log.info("Fail Authentication EntryPoint Message: {}", authException.getMessage());

    response.setStatus(HttpStatus.UNAUTHORIZED.value());

    ErrorResponseDTO responseDTO = ErrorResponseDTO.of(HttpStatus.UNAUTHORIZED.value(), authException.getMessage());
    response.setCharacterEncoding("UTF-8");
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    log.error(authException.getMessage(), authException);

    defaultJacksonConverter.getObjectMapper().writeValue(response.getWriter(), responseDTO);
  }
}
