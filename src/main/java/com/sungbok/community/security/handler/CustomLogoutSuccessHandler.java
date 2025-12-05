package com.sungbok.community.security.handler;

import com.sungbok.community.common.dto.OkResponseDTO;
import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

  private final ObjectMapper objectMapper;

  @Override
  public void onLogoutSuccess(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable Authentication authentication) throws IOException {
    UserMemberDTO userMember = SecurityUtils.getUserFromAuthentication(authentication);
    log.info("Success logout User: {}", objectMapper.writeValueAsString(userMember));

    OkResponseDTO responseDTO = OkResponseDTO.of(
        HttpStatus.OK.value(),
        "Logout successful. Use POST /auth/logout to invalidate JWT tokens.",
        userMember
    );
    if (response != null) {
      response.setCharacterEncoding("UTF-8");
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      objectMapper.writeValue(response.getWriter(), responseDTO);
    }
  }
}
