package com.sungbok.community.service.change.impl;

import com.sungbok.community.common.exception.AlreadyExistException;
import com.sungbok.community.dto.AddUserRequestDTO;
import com.sungbok.community.dto.UpdateUserWithMember;
import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.enums.UserRole;
import com.sungbok.community.repository.MembersRepository;
import com.sungbok.community.repository.UserRepository;
import com.sungbok.community.service.change.ChangeUserService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jooq.generated.tables.pojos.Members;
import org.jooq.generated.tables.pojos.Users;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class ChangeUserServiceImpl implements ChangeUserService {

    private final UserRepository userRepository;
    private final MembersRepository membersRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public UserMemberDTO signup(AddUserRequestDTO dto) {

        if(Objects.nonNull(userRepository.findUserWithDetailsByEmail(dto.getEmail()))){
            throw new AlreadyExistException("이미 가입한 회원입니다.");
        }

        Users user = userRepository.save(dto);
        Members member = membersRepository.save(dto,user);

        return UserMemberDTO.builder()
                .user(user)
                .member(member)
                .role(UserRole.findByCode(member.getRole()))
                .build();
    }


    @Override
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

        return userRepository.findUserWithDetailsById(finalUserId)
             .orElseThrow(() -> new RuntimeException("Failed to fetch final user/member state after save/update for ID: " + finalUserId));
    }

    @Override
    public void deleteUser(Long userId) {
        userRepository.deleteUser(userId);
    }
}
