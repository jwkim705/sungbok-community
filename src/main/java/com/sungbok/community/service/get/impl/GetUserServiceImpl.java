package com.sungbok.community.service.get.impl;

import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.repository.MembersRepository;
import com.sungbok.community.repository.UserRepository;
import com.sungbok.community.service.get.GetUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetUserServiceImpl implements GetUserService {

    private final UserRepository userRepository;
    private final MembersRepository membersRepository;

    @Override
    public UserMemberDTO getUser(Long userId) {
        return userRepository.fetchUserWithDetailsById(userId)
                .orElseThrow(() -> new RuntimeException("User or Member details not found for ID: " + userId));
    }

    @Override
    public UserMemberDTO getUser(String email) {
        // 최적화: 2개 쿼리(fetchByEmail + fetchUserWithDetailsById)를 1개로 통합
        return userRepository.fetchUserWithDetailsByEmail(email)
            .orElseThrow(() -> new RuntimeException("User or Member details not found for email: " + email));
    }

}
