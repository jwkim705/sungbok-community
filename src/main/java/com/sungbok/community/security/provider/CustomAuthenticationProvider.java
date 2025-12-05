package com.sungbok.community.security.provider;

import com.sungbok.community.common.exception.NotMatchPwdException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationProvider implements AuthenticationProvider {

  private final UserDetailsService userDetailsService;

  private final BCryptPasswordEncoder passwordEncoder;

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    String username = authentication.getName();
    String password = Objects.requireNonNull(authentication.getCredentials()).toString();

    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

    if (passwordEncoder.matches(password, userDetails.getPassword())) {
      return new UsernamePasswordAuthenticationToken(userDetails, password, userDetails.getAuthorities());
    } else {
      throw new NotMatchPwdException(HttpStatus.UNAUTHORIZED, "암호가 일치하지 않습니다.");
    }
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return authentication.equals(UsernamePasswordAuthenticationToken.class);
  }
}
