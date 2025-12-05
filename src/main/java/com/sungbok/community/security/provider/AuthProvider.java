package com.sungbok.community.security.provider;

import org.springframework.stereotype.Component;

@Component
public class AuthProvider {

  public String[] ignoreListDefaultEndpoints() {

    return new String[] {
      "/v3/api-docs/**",
      "/swagger-ui.html",
      "/swagger-ui/**",
      "/h2-console/**"
    };
  }

  public String[] whiteListDefaultEndpoints() {

    return new String[] {
      // 정적 리소스만 포함 (필요시)
    };
  }

}
