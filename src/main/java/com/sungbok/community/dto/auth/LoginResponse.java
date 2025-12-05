package com.sungbok.community.dto.auth;

import com.sungbok.community.dto.UserMemberDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 로그인 응답 DTO
 * 사용자 정보 + JWT 토큰
 *
 * @since 0.0.1
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1234567890123456789L;

    /**
     * 로그인한 사용자 정보
     */
    private UserMemberDTO user;

    /**
     * JWT 토큰 (Access + Refresh)
     */
    private TokenResponse tokens;
}
