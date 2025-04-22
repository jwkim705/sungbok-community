package com.sungbok.community.security.service.impl;

import com.sungbok.community.repository.users.UserRepository;
import com.sungbok.community.security.model.PrincipalDetails;
import com.sungbok.community.security.model.SecurityUserItem;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.jooq.generated.tables.pojos.Users;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

  private final UserRepository userRepository;
  private final HttpSession httpSession;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

    Users user = userRepository.findByEmail(username);
    httpSession.setAttribute("user", user);
    return new PrincipalDetails(user, SecurityUserItem.of(user));
  }
}
