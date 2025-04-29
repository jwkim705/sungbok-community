package com.sungbok.community.service.get.impl;

import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.repository.member.MembersRepository;
import com.sungbok.community.repository.users.UserRepository;
import com.sungbok.community.service.get.GetUserService;
import lombok.RequiredArgsConstructor;
import org.jooq.generated.tables.pojos.Users;
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
        return userRepository.findUserWithDetailsById(userId)
                .orElseThrow(() -> new RuntimeException("User or Member details not found for ID: " + userId));
    }

    @Override
    public UserMemberDTO getUser(String email) {
        Users user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        // Use the JOIN method from UserRepository to get the DTO
        return userRepository.findUserWithDetailsById(user.getId())
             .orElseThrow(() -> new RuntimeException("User or Member details not found for ID: " + user.getId() + " after finding user by email."));
    }

}
