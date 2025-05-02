package com.sungbok.community.security.service.impl;

import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.repository.users.UserRepository;
import com.sungbok.community.security.model.PrincipalDetails;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
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

    UserMemberDTO user = userRepository.findUserWithDetailsByEmailOptional(username).orElseThrow(() -> new UsernameNotFoundException("회원을 찾을 수 없습니다."));
    httpSession.setAttribute("user", user);
    return new PrincipalDetails(user);
  }
}
