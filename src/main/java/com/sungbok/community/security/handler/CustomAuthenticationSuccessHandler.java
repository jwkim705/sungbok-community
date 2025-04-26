package com.sungbok.community.security.handler;

import com.sungbok.community.security.model.PrincipalDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

  private final MappingJackson2HttpMessageConverter defaultJacksonConverter;
  private final HttpSession httpSession;

  @Value("${front.url}")
  private String frontUrl;

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request,
                                      HttpServletResponse response,
                                      Authentication authentication) throws IOException {


    PrincipalDetails user =  (PrincipalDetails) authentication.getPrincipal();

    log.info("SuccessHandler loginUser: {}", defaultJacksonConverter.getObjectMapper().writeValueAsString(user.getUser()));
    httpSession.setAttribute("user", user);
    UsernamePasswordAuthenticationToken result = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    result.setDetails(user);
    SecurityContextHolder.getContext().setAuthentication(result);

    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.sendRedirect(
        UriComponentsBuilder.fromUriString(frontUrl+"/login")
            .queryParam("code", HttpStatus.OK.value())
            .queryParam("message", HttpStatus.OK.name())
            .queryParam("data", defaultJacksonConverter.getObjectMapper().writeValueAsString(user.getUser()))
            .build()
            .encode(StandardCharsets.UTF_8)
            .toUriString()

    );

  }
}
