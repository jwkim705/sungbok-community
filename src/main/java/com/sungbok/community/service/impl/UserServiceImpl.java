package com.sungbok.community.service.impl;

import com.sungbok.community.dto.UpdateUserWithMember;
import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.repository.member.MembersRepository;
import com.sungbok.community.repository.users.UserRepository;
import com.sungbok.community.service.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jooq.generated.tables.pojos.Members;
import org.jooq.generated.tables.pojos.Users;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final MembersRepository membersRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserMemberDTO getUser(Long userId) {
        return userRepository.findUserWithMemberById(userId)
                .orElseThrow(() -> new RuntimeException("User or Member details not found for ID: " + userId));
    }

    @Override
    @Transactional(readOnly = true)
    public UserMemberDTO getUser(String email) {
        Users user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        // Use the JOIN method from UserRepository to get the DTO
        return userRepository.findUserWithMemberById(user.getId())
             .orElseThrow(() -> new RuntimeException("User or Member details not found for ID: " + user.getId() + " after finding user by email."));
    }

    @Override
    @Transactional
    public UserMemberDTO saveOrUpdateUser(UpdateUserWithMember updateReq) {
        Long userId = updateReq.getUserId();
        Long finalUserId; // To store the ID for final fetch

        if (userId != null && userId > 0) {
            finalUserId = userId;

            Users existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found for update with ID: " + userId));

            if (updateReq.getEmail() != null) {
                existingUser.setEmail(updateReq.getEmail());
            }
            if (StringUtils.isNotBlank(updateReq.getPassword())) {
                existingUser.setPassword(passwordEncoder.encode(updateReq.getPassword()));
            }
            userRepository.updateUsingStore(existingUser);

            Members existingMember = membersRepository.findByUserId(userId)
                 .orElseThrow(() -> new RuntimeException("Member not found for user ID: " + userId)); // Or handle if member might not exist

            if (updateReq.getName() != null) {
                existingMember.setName(updateReq.getName());
            }
            if (updateReq.getPicture() != null) {
                existingMember.setPicture(updateReq.getPicture());
            }
            membersRepository.updateUsingStore(existingMember);

        } else {
            if (updateReq.getEmail() == null) {
                 throw new IllegalArgumentException("Email is required for new users.");
            }

            Users newUser = new Users();
            newUser.setEmail(updateReq.getEmail());

            if (StringUtils.isNotBlank(updateReq.getPassword())) {
                newUser.setPassword(passwordEncoder.encode(updateReq.getPassword()));
            } else {
                newUser.setPassword(null);
            }

            Users savedUser = userRepository.save(newUser);
            finalUserId = savedUser.getId();

            Members newMember = new Members();
            newMember.setUserId(finalUserId);
            newMember.setName(updateReq.getName());
            newMember.setPicture(updateReq.getPicture());
            membersRepository.save(newMember);
        }

        return userRepository.findUserWithMemberById(finalUserId)
             .orElseThrow(() -> new RuntimeException("Failed to fetch final user/member state after save/update for ID: " + finalUserId));
    }
}
