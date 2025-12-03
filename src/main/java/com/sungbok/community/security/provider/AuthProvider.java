package com.sungbok.community.security.provider;

import org.springframework.stereotype.Component;

@Component
public class AuthProvider {

  public String[] ignoreListDefaultEndpoints() {

    return new String[] {
      "/api-docs/**",
      "/swagger-ui/**",
      "/swagger.html",
      "/h2-console/**",
      "/api/login",
      "/api/logout",
    };
  }

  public String[] whiteListDefaultEndpoints() {

    return new String[] {
      // 정적 리소스만 포함 (필요시)
    };
  }

}
