package com.sungbok.community.security.service.impl;

import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.repository.UserRepository;
import com.sungbok.community.security.model.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  public @NonNull UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
    UserMemberDTO user = userRepository.fetchUserByEmailForAuthentication(username)
            .orElseThrow(() -> new UsernameNotFoundException("회원을 찾을 수 없습니다."));
    return new PrincipalDetails(user);
  }
}
