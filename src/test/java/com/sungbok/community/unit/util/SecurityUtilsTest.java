package com.sungbok.community.unit.util;

import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.security.model.PrincipalDetails;
import com.sungbok.community.util.SecurityUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilsTest {

  @Test
  @DisplayName("유효한 Authentication에서 PrincipalDetails 추출 성공")
  void getPrincipalDetails_withValidAuthentication_shouldReturnPrincipalDetails() {
    // Given
    UserMemberDTO user = createTestUser();
    PrincipalDetails principalDetails = new PrincipalDetails(user);
    Authentication authentication = new UsernamePasswordAuthenticationToken(
        principalDetails, null, principalDetails.getAuthorities()
    );

    // When
    PrincipalDetails result = SecurityUtils.getPrincipalDetails(authentication);

    // Then
    assertNotNull(result);
    assertEquals(principalDetails, result);
  }

  @Test
  @DisplayName("유효한 Authentication에서 UserMemberDTO 추출 성공")
  void getUserFromAuthentication_withValidAuthentication_shouldReturnUserMemberDTO() {
    // Given
    UserMemberDTO user = createTestUser();
    PrincipalDetails principalDetails = new PrincipalDetails(user);
    Authentication authentication = new UsernamePasswordAuthenticationToken(
        principalDetails, null, principalDetails.getAuthorities()
    );

    // When
    UserMemberDTO result = SecurityUtils.getUserFromAuthentication(authentication);

    // Then
    assertNotNull(result);
    assertEquals(user.getUserId(), result.getUserId());
    assertEquals(user.getEmail(), result.getEmail());
  }

  @Test
  @DisplayName("Authentication이 null이면 예외 발생")
  void getPrincipalDetails_withNullAuthentication_shouldThrowException() {
    // When & Then
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> SecurityUtils.getPrincipalDetails(null)
    );

    assertEquals("인증 객체는 필수입니다", exception.getMessage());
  }

  @Test
  @DisplayName("principal이 null이면 예외 발생")
  void getPrincipalDetails_withNullPrincipal_shouldThrowException() {
    // Given
    Authentication authentication = new UsernamePasswordAuthenticationToken(null, null);

    // When & Then
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> SecurityUtils.getPrincipalDetails(authentication)
    );

    assertEquals("인증 주체는 필수입니다", exception.getMessage());
  }

  @Test
  @DisplayName("principal이 잘못된 타입이면 예외 발생")
  void getPrincipalDetails_withWrongPrincipalType_shouldThrowException() {
    // Given - String principal (common for anonymous users)
    Authentication authentication = new UsernamePasswordAuthenticationToken(
        "anonymousUser", null
    );

    // When & Then
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> SecurityUtils.getPrincipalDetails(authentication)
    );

    assertTrue(exception.getMessage().contains("인증 주체는 PrincipalDetails 인스턴스여야 하지만"));
    assertTrue(exception.getMessage().contains("String"));
  }

  @Test
  @DisplayName("getUserFromAuthentication - Authentication null이면 예외 발생")
  void getUserFromAuthentication_withNullAuthentication_shouldThrowException() {
    // When & Then
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> SecurityUtils.getUserFromAuthentication(null)
    );

    assertEquals("인증 객체는 필수입니다", exception.getMessage());
  }

  @Test
  @DisplayName("getUserFromAuthentication - principal null이면 예외 발생")
  void getUserFromAuthentication_withNullPrincipal_shouldThrowException() {
    // Given
    Authentication authentication = new UsernamePasswordAuthenticationToken(null, null);

    // When & Then
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> SecurityUtils.getUserFromAuthentication(authentication)
    );

    assertEquals("인증 주체는 필수입니다", exception.getMessage());
  }

  @Test
  @DisplayName("getUserFromAuthentication - principal 타입 불일치시 예외 발생")
  void getUserFromAuthentication_withWrongPrincipalType_shouldThrowException() {
    // Given
    Authentication authentication = new UsernamePasswordAuthenticationToken(
        "anonymousUser", null
    );

    // When & Then
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> SecurityUtils.getUserFromAuthentication(authentication)
    );

    assertTrue(exception.getMessage().contains("인증 주체는 PrincipalDetails 인스턴스여야 하지만"));
  }

  private UserMemberDTO createTestUser() {
    List<Long> roleIds = List.of(1L);
    return new UserMemberDTO(
        1L, // orgId
        1L, // userId
        "test@example.com",
        "Test User",
        "password",
        LocalDate.of(1990, 1, 1),
        "M",
        "Test Address",
        "010-1234-5678",
        null, // picture
        null, // registeredByUserId
        1L,  // appTypeId
        roleIds // roleIds
    );
  }
}
