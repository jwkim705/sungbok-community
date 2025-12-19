package com.sungbok.community.util;

import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.security.model.PrincipalDetails;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;

/**
 * Spring Security Authentication에서 사용자 정보를 추출하는 유틸리티 클래스
 *
 * @since 0.0.1
 */
public class SecurityUtils {

  /** 유틸리티 클래스는 인스턴스화 불가 */
  private SecurityUtils() {
    throw new UnsupportedOperationException("유틸리티 클래스는 인스턴스화할 수 없습니다");
  }

  /**
   * Authentication에서 PrincipalDetails 추출 (null 검증 및 타입 체크 수행)
   *
   * @param authentication 인증 객체
   * @return PrincipalDetails (null 아님)
   * @throws IllegalArgumentException authentication이 null이거나, principal이 PrincipalDetails 타입이 아닌 경우
   */
  public static @NonNull PrincipalDetails getPrincipalDetails(Authentication authentication) {
    if (authentication == null) {
      throw new IllegalArgumentException("인증 객체는 필수입니다");
    }

    Object principal = authentication.getPrincipal();
    if (principal == null) {
      throw new IllegalArgumentException("인증 주체는 필수입니다");
    }

    if (!(principal instanceof PrincipalDetails)) {
      throw new IllegalArgumentException(
          "인증 주체는 PrincipalDetails 인스턴스여야 하지만, 실제 타입은: "
              + principal.getClass().getName()
      );
    }

    return (PrincipalDetails) principal;
  }

  /**
   * Authentication에서 UserMemberDTO 직접 추출 (getPrincipalDetails + getUser)
   *
   * @param authentication 인증 객체
   * @return UserMemberDTO (null 아님)
   * @throws IllegalArgumentException authentication이 null이거나, principal이 PrincipalDetails 타입이 아닌 경우
   */
  public static @NonNull UserMemberDTO getUserFromAuthentication(Authentication authentication) {
    PrincipalDetails principalDetails = getPrincipalDetails(authentication);
    return principalDetails.getUser();
  }
}
