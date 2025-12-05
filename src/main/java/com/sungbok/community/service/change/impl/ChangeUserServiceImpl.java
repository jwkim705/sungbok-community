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

        if(userRepository.fetchUserWithDetailsByEmail(dto.getEmail()).isPresent()){
            throw new AlreadyExistException("이미 가입한 회원입니다.");
        }

        // Users 엔티티 생성 및 저장
        Users user = new Users();
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setIsDeleted(false);
        Users savedUser = userRepository.insert(user);

        // Members 엔티티 생성 및 저장
        Members member = new Members();
        member.setUserId(savedUser.getId());
        member.setName(dto.getName());
        member.setBirthdate(dto.getBirthday());
        member.setGender(dto.getGender());
        member.setAddress(dto.getAddress());
        member.setPhoneNumber(dto.getPhoneNumber());
        member.setRole(StringUtils.isNotBlank(dto.getRole()) ? dto.getRole() : "GUEST");
        // registeredByUserId가 null이면 자신의 ID를 등록자로 설정 (자가 등록)
        member.setRegisteredByUserId(
            dto.getRegisteredByUserId() != null
                ? dto.getRegisteredByUserId()
                : savedUser.getId()
        );
        member.setIsDeleted(false);
        Members savedMember = membersRepository.insert(member);

        return UserMemberDTO.builder()
                .user(savedUser)
                .member(savedMember)
                .role(UserRole.fromCode(savedMember.getRole()))
                .build();
    }


    @Override
    public UserMemberDTO saveOrUpdateUser(UpdateUserWithMember updateReq) {
        Long userId = updateReq.getUserId();
        Long finalUserId; // To store the ID for final fetch

        if (userId != null && userId > 0) {
            finalUserId = userId;

            Users existingUser = userRepository.fetchById(userId)
                .orElseThrow(() -> new RuntimeException("User not found for update with ID: " + userId));

            if (updateReq.getEmail() != null) {
                existingUser.setEmail(updateReq.getEmail());
            }
            if (StringUtils.isNotBlank(updateReq.getPassword())) {
                existingUser.setPassword(passwordEncoder.encode(updateReq.getPassword()));
            }
            userRepository.update(existingUser);

            Members existingMember = membersRepository.fetchByUserId(userId)
                 .orElseThrow(() -> new RuntimeException("Member not found for user ID: " + userId)); // Or handle if member might not exist

            if (updateReq.getName() != null) {
                existingMember.setName(updateReq.getName());
            }
            if (updateReq.getPicture() != null) {
                existingMember.setPicture(updateReq.getPicture());
            }
            membersRepository.update(existingMember);



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
            newUser.setIsDeleted(false);

            Users savedUser = userRepository.insert(newUser);
            finalUserId = savedUser.getId();

            Members newMember = new Members();
            newMember.setUserId(finalUserId);
            newMember.setName(updateReq.getName());
            newMember.setPicture(updateReq.getPicture());
            newMember.setRole(updateReq.getRole() != null ? updateReq.getRole().getCode() : "GUEST");
            newMember.setIsDeleted(false);
            membersRepository.insert(newMember);
        }

        return userRepository.fetchUserWithDetailsById(finalUserId)
             .orElseThrow(() -> new RuntimeException("Failed to fetch final user/member state after save/update for ID: " + finalUserId));
    }

    @Override
    public void deleteUser(Long userId) {
        userRepository.softDelete(userId);
    }
}
