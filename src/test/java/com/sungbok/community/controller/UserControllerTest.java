package com.sungbok.community.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sungbok.community.common.constant.UriConstant;
import com.sungbok.community.dto.AddUserRequestDTO;
import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.enums.UserRole;
import com.sungbok.community.repository.UserRepository;
import com.sungbok.community.security.TenantContext;
import com.sungbok.community.service.change.ChangeUserService;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoSpyBean
    ChangeUserService changeUserService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    UserRepository userRepository;

    /**
     * 각 테스트 전에 TenantContext를 초기화합니다.
     * 테스트용 기본 테넌트(app_id = 1: Default Church)를 설정합니다.
     */
    @BeforeEach
    void setUpTenantContext() {
        TenantContext.setAppId(1L);
    }

    /**
     * 각 테스트 후 TenantContext를 정리합니다.
     */
    @AfterEach
    void cleanupTenantContext() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("회원 ID로 회원 정보조회")
    void findUserWithDetailsById() {
        Optional<UserMemberDTO> oDto = userRepository.fetchUserWithDetailsById(1L);

        UserMemberDTO dto = null;

        if(oDto.isPresent()){
            dto = oDto.get();
        }

        assertNotNull(dto);
    }

    @Test
    @DisplayName("회원 이메일로 회원 정보조회")
    void findUserWithDetailsByEmail() {
        UserMemberDTO dto = userRepository.fetchUserWithDetailsByEmail("admin@example.com").orElse(null);

        assertNotNull(dto);
    }

    @Test
    @DisplayName("회원가입")
    @Transactional
    void signup() throws Exception {

        assertTrue(Mockito.mockingDetails(changeUserService).isSpy());

        AddUserRequestDTO requestDTO = AddUserRequestDTO.builder()
                .name("test")
                .email("a@test.com")
                .password("test!@34%6")
                .birthday(LocalDate.of(1992,7,5))
                .address("주소")
                .deptNm("청년부")
                .role("성도")
                .gender("MALE")
                .phoneNumber("010-1234-5678")
                .nickname("nick")
                .build();

        UserMemberDTO mockedServiceResponse = new UserMemberDTO(
                1L, // appId
                1L, // 테스트용 가짜 userId
                requestDTO.getEmail(),
                requestDTO.getName(),
                "mock_password_not_usually_needed", // DTO에 비밀번호가 꼭 필요 없다면 null 또는 임의값
                requestDTO.getBirthday(),
                requestDTO.getGender(),
                requestDTO.getAddress(),
                requestDTO.getPhoneNumber(),
                null, // picture (테스트에 필요 없다면 null)
                null, // registeredByUserId (테스트에 필요 없다면 null)
                UserRole.USER
        );

        doReturn(mockedServiceResponse)
                .when(changeUserService).signup(any(AddUserRequestDTO.class));

        ResultActions resultActions = mockMvc.perform(
            MockMvcRequestBuilders.post(UriConstant.USERS+"/signup")
                .content(objectMapper.writeValueAsString(requestDTO))
                .contentType(MediaType.APPLICATION_JSON_VALUE));

        MvcResult mvcResult = resultActions
                .andExpect(status().isCreated())
                .andDo(print())
                .andReturn();

        System.out.println("reqponseBody :" + mvcResult.getResponse().getContentAsString());

    }

}