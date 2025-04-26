package com.sungbok.community.security.handler;

import com.sungbok.community.common.dto.OkResponseDTO;
import com.sungbok.community.security.model.PrincipalDetails;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

  private final MappingJackson2HttpMessageConverter defaultJacksonConverter;

  @Override
  public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {
    if (authentication != null && authentication.getDetails() != null) {

      PrincipalDetails user =  (PrincipalDetails) authentication.getPrincipal();
      log.info("Success logout User: {}", defaultJacksonConverter.getObjectMapper().writeValueAsString(user.getUser()));

      request.getSession().invalidate();
      SecurityContextHolder.clearContext();

      OkResponseDTO responseDTO = OkResponseDTO.of(HttpStatus.OK.value(), HttpStatus.OK.name(), user.getUser());
      response.setCharacterEncoding("UTF-8");
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);

      defaultJacksonConverter.getObjectMapper().writeValue(response.getWriter(), responseDTO);
    }


  }
}
