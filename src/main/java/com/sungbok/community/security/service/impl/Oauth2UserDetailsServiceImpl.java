package com.sungbok.community.security.service.impl;

import com.sungbok.community.dto.UpdateUserWithMember;
import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.enums.SocialType;
import com.sungbok.community.enums.UserRole;
import com.sungbok.community.repository.OauthAccountsRepository;
import com.sungbok.community.repository.UserRepository;
import com.sungbok.community.security.model.OAuthAttributes;
import com.sungbok.community.security.model.PrincipalDetails;
import com.sungbok.community.service.change.ChangeUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class Oauth2UserDetailsServiceImpl implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

  private final UserRepository userRepository;
  private final OauthAccountsRepository oauthAccountsRepository;
  private final ChangeUserService changeUserService;

  @Override
  @Transactional
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
    OAuth2User oAuth2User = delegate.loadUser(userRequest);

    String registrationId = userRequest.getClientRegistration().getRegistrationId();
    String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

    OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());

    UserMemberDTO user = saveOrUpdate(attributes, registrationId);

    return new PrincipalDetails(user, attributes.getAttributes());
  }

  /**
   * OAuth 사용자를 저장하거나 업데이트합니다.
   *
   * @param attributes OAuth 제공자로부터 받은 사용자 정보
   * @param registrationId OAuth 제공자 ID (google, kakao, naver)
   * @return 저장되거나 업데이트된 사용자 정보
   */
  private UserMemberDTO saveOrUpdate(OAuthAttributes attributes, String registrationId) {
      SocialType socialType = SocialType.fromCode(registrationId.toUpperCase());

      // 1. 이메일로 기존 사용자 검색
      Optional<UserMemberDTO> existingUser = userRepository.fetchUserWithDetailsByEmail(attributes.getEmail());

      if (existingUser.isPresent()) {
          // 2-A. 기존 사용자: picture 업데이트 + oauth_accounts에 소셜 타입 추가
          UserMemberDTO user = existingUser.get();

          log.info("OAuth 로그인: 기존 사용자 발견 - userId={}, email={}, provider={}",
                  user.getUserId(), user.getEmail(), registrationId);

          // 프로필 사진 업데이트
          UpdateUserWithMember updateReq = new UpdateUserWithMember();
          updateReq.setUserId(user.getUserId());
          updateReq.setEmail(attributes.getEmail());
          updateReq.setName(user.getName()); // 기존 이름 유지
          updateReq.setPicture(attributes.getPicture()); // 프로필 사진만 업데이트
          updateReq.setRole(user.getRole()); // 기존 역할 유지

          // OAuth 계정 정보 저장 (중복 체크 포함)
          if (oauthAccountsRepository.fetchByUserIdAndSocialType(user.getUserId(), socialType).isEmpty()) {
              oauthAccountsRepository.insert(user.getUserId(), socialType);
          }

          return changeUserService.saveOrUpdateUser(updateReq);
      } else {
          // 2-B. 신규 사용자: users + members + oauth_accounts 생성
          log.info("OAuth 로그인: 신규 사용자 생성 - email={}, name={}, provider={}",
                  attributes.getEmail(), attributes.getName(), registrationId);

          UpdateUserWithMember updateReq = new UpdateUserWithMember();
          updateReq.setEmail(attributes.getEmail());
          updateReq.setPassword(null); // OAuth 사용자는 password null
          updateReq.setName(attributes.getName());
          updateReq.setPicture(attributes.getPicture());
          updateReq.setRole(UserRole.GUEST); // 기본 역할

          // 추가 정보(birthdate, gender, address, phoneNumber)는 자동으로 null이 되며,
          // 나중에 프로필 페이지에서 입력할 수 있습니다

          UserMemberDTO newUser = changeUserService.saveOrUpdateUser(updateReq);

          // OAuth 계정 정보 저장
          oauthAccountsRepository.insert(newUser.getUserId(), socialType);

          log.info("OAuth 로그인: 신규 사용자 생성 완료 - userId={}, email={}",
                  newUser.getUserId(), newUser.getEmail());

          return newUser;
      }
  }

}
