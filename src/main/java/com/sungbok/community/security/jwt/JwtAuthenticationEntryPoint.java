package com.sungbok.community.security.jwt;

import com.sungbok.community.common.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * JWT 인증 실패 시 401 Unauthorized 응답 처리
 * CustomAuthenticationEntryPointHandler를 대체
 *
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.error("인증 실패 - 401 Unauthorized: {} ({})", authException.getMessage(), request.getRequestURI());

        ErrorResponseDTO errorResponse = ErrorResponseDTO.of(
                HttpStatus.UNAUTHORIZED.value(),
                "인증이 필요합니다. 유효한 JWT 토큰을 제공해주세요.",
                null
        );

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
