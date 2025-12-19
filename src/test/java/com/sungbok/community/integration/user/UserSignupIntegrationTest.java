package com.sungbok.community.integration.user;

import com.sungbok.community.common.constant.UriConstant;
import com.sungbok.community.dto.AddUserRequestDTO;
import com.sungbok.community.support.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 사용자 가입 통합 테스트
 * POST /users/signup 엔드포인트를 테스트합니다.
 */
@Transactional
class UserSignupIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("회원가입 - 유효한 요청 - 201 Created")
    void testSignup_WithValidRequest_ShouldReturn201() throws Exception {
        // 준비: 회원가입 요청 DTO
        AddUserRequestDTO requestDTO = AddUserRequestDTO.builder()
                .name("테스트회원")
                .email("newuser@test.com")
                .password("test!@34%6")
                .birthday(LocalDate.of(1992, 7, 5))
                .address("주소")
                .deptNm("청년부")
                .gender("MALE")
                .phoneNumber("010-1234-5678")
                .nickname("닉네임")
                .build();

        // 실행: 회원가입 API 호출
        Long orgId = testDataManager.getTestOrgId();
        MvcResult result = mockMvc.perform(
                        post(UriConstant.USERS + "/signup")
                                .header("X-Org-Id", orgId)
                                .content(objectMapper.writeValueAsString(requestDTO))
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                // 검증: 201 Created 응답
                .andExpect(status().isCreated())
                .andReturn();

        // 응답 확인 (로그)
        log.info("회원가입 응답: {}", result.getResponse().getContentAsString());
    }
}
