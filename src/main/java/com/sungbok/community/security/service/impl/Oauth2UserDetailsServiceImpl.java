package com.sungbok.community.security.service.impl;

import com.sungbok.community.security.model.OAuthAttributes;
import com.sungbok.community.security.model.PrincipalDetails;
import com.sungbok.community.security.model.SecurityUserItem;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Configuration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional(readOnly = true)
public class Oauth2UserDetailsServiceImpl implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

  private final UsersDao usersDao;
  private final HttpSession httpSession;

    public Oauth2UserDetailsServiceImpl(UsersDao usersDao, HttpSession httpSession, Configuration configuration) {
        this.usersDao = new UsersDao(configuration);
        this.httpSession = httpSession;
    }

    @Override
  @Transactional
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
    OAuth2User oAuth2User = delegate.loadUser(userRequest);

    String registrationId = userRequest.getClientRegistration().getRegistrationId();
    String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

    OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());

    Users user = saveOrUpdate(attributes);

    PrincipalDetails details = new PrincipalDetails(user,attributes.getAttributes(), SecurityUserItem.of(user));
    httpSession.setAttribute("user", details);

    return details;
  }

  private Users saveOrUpdate(OAuthAttributes attributes) {
      Users user = usersDao.findById(attributes).findOneByEmail(attributes.getEmail())
              .map(entity -> entity.update(attributes.getName(), attributes.getPicture()))
              .orElse(attributes.toEntity(attributes.getPicture()));

      return usersDao.insert(user);
  }

}
