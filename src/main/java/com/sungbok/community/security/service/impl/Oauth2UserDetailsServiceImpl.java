package com.sungbok.community.security.service.impl;

import com.sungbok.community.dto.UpdateUserWithMember;
import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.security.model.OAuthAttributes;
import com.sungbok.community.security.model.PrincipalDetails;
import com.sungbok.community.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.generated.tables.pojos.Users;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class Oauth2UserDetailsServiceImpl implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

  private final UserService userService;
  private final HttpSession httpSession;

  @Override
  @Transactional
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
    OAuth2User oAuth2User = delegate.loadUser(userRequest);

    String registrationId = userRequest.getClientRegistration().getRegistrationId();
    String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

    OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());

    UserMemberDTO user = saveOrUpdate(attributes);

    PrincipalDetails details = new PrincipalDetails(user,attributes.getAttributes());
    httpSession.setAttribute("user", details);

    return details;
  }

  private UserMemberDTO saveOrUpdate(OAuthAttributes attributes) {
      UserMemberDTO user = userService.getUser(attributes.getEmail());

      UpdateUserWithMember updateUserWithMember = new UpdateUserWithMember();
      updateUserWithMember.setUserId(user.getUserId());
      updateUserWithMember.setEmail(attributes.getEmail());
      updateUserWithMember.setName(attributes.getName());
      updateUserWithMember.setPicture(attributes.getPicture());

      return userService.saveOrUpdateUser(updateUserWithMember);
  }

}
