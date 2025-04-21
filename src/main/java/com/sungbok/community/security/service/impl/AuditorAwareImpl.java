package com.sungbok.community.security.service.impl;

import com.rsupport.shuttlecock.security.model.PrincipalDetails;
import com.rsupport.shuttlecock.security.model.SecurityUserItem;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuditorAwareImpl implements AuditorAware<Long> {

  @Override
  public Optional<Long> getCurrentAuditor() {
    Authentication authentication = SecurityContextHolder
      .getContext()
      .getAuthentication();

    if (Objects.isNull(authentication) || !authentication.isAuthenticated()) {
      return Optional.empty();
    }

    Object principal = authentication.getPrincipal();

    if (principal.equals("anonymousUser")) {
      return Optional.empty();
    }

    if(principal instanceof PrincipalDetails userAdapter) {
      SecurityUserItem securityUserItem = userAdapter.getSecurityUserItem();
      return Optional.ofNullable(securityUserItem.getUserId());
    } else {
      return Optional.empty();
    }

  }
}
