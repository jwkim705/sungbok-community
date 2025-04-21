package com.sungbok.community.security.service.impl;

import com.rsupport.shuttlecock.entity.User;
import com.rsupport.shuttlecock.security.model.PrincipalDetails;
import com.rsupport.shuttlecock.security.model.SecurityUserItem;
import com.rsupport.shuttlecock.service.GetUserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

  private final GetUserService getUserService;
  private final HttpSession httpSession;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    User user = getUserService.validateReturnUser(username);
    httpSession.setAttribute("user", user);
    return new PrincipalDetails(user, SecurityUserItem.of(user));
  }
}
