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
    };
  }

  public String[] whiteListDefaultEndpoints() {

    return new String[] {
      "/api/v1/**",
    };
  }

}
