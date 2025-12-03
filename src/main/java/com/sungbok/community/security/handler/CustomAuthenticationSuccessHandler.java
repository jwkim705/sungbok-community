package com.sungbok.community.security.handler;

import com.sungbok.community.common.dto.OkResponseDTO;
import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

  private final ObjectMapper objectMapper;

  @Override
  public void onAuthenticationSuccess(@Nullable HttpServletRequest request,
                                      HttpServletResponse response,
                                      Authentication authentication) throws IOException {

    UserMemberDTO userMember = SecurityUtils.getUserFromAuthentication(authentication);

    log.info("SuccessHandler loginUser: {}", objectMapper.writeValueAsString(userMember));

    OkResponseDTO responseDto = OkResponseDTO.of(
            HttpStatus.OK.value(),
            HttpStatus.OK.name(),             // 응답 메시지
            userMember              // 응답 데이터 (UserMemberDTO)
    );

    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    try (PrintWriter writer = response.getWriter()) {
      String jsonResponse = objectMapper.writeValueAsString(responseDto);
      writer.write(jsonResponse);
      writer.flush();
    } catch (IOException e) {
      log.error("JSON 응답 작성 중 오류 발생: {}", e.getMessage(), e);
      if (!response.isCommitted()) {
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "응답 작성 중 오류가 발생했습니다.");
      }
    }
  }
}
