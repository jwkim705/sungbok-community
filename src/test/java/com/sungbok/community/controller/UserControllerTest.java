package com.sungbok.community.controller;

import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.repository.users.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserControllerTest {

    private static final Logger log = LoggerFactory.getLogger(UserControllerTest.class);
    @Autowired
    UserRepository userRepository;

    @Test
    @DisplayName("회원 ID로 회원 정보조회")
    void findUserWithDetailsById() {
        UserMemberDTO dto = userRepository.findUserWithDetailsById(1L).get();

        log.info("DTO: {}", dto);

        assertNotNull(dto);
    }

    @Test
    @DisplayName("회원 이메일로 회원 정보조회")
    void findUserWithDetailsByEmail() {
        UserMemberDTO dto = userRepository.findUserWithDetailsByEmail("admin@example.com").get();

        assertNotNull(dto);
    }

}