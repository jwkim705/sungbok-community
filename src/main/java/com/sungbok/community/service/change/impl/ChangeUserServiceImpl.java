package com.sungbok.community.service.change.impl;

import com.sungbok.community.common.exception.AlreadyExistException;
import com.sungbok.community.common.exception.DataNotFoundException;
import com.sungbok.community.dto.AddUserRequestDTO;
import com.sungbok.community.dto.UpdateUserWithMember;
import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.repository.MembersRepository;
import com.sungbok.community.repository.MembershipRolesRepository;
import com.sungbok.community.repository.OrganizationsRepository;
import com.sungbok.community.repository.RolesRepository;
import com.sungbok.community.repository.UserRepository;
import com.sungbok.community.security.TenantContext;
import com.sungbok.community.service.change.ChangeUserService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jooq.generated.tables.pojos.Memberships;
import org.jooq.generated.tables.pojos.Organizations;
import org.jooq.generated.tables.pojos.Roles;
import org.jooq.generated.tables.pojos.Users;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ChangeUserServiceImpl implements ChangeUserService {

    private final UserRepository userRepository;
    private final MembersRepository membersRepository;
    private final RolesRepository rolesRepository;
    private final MembershipRolesRepository membershipRolesRepository;
    private final OrganizationsRepository organizationsRepository;
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

        // Memberships 엔티티 생성 및 저장
        Long orgId = TenantContext.getRequiredOrgId();
        Memberships membership = new Memberships();
        membership.setOrgId(orgId);
        membership.setUserId(savedUser.getId());
        membership.setName(dto.getName());
        membership.setBirthdate(dto.getBirthday());
        membership.setGender(dto.getGender());
        membership.setAddress(dto.getAddress());
        membership.setPhoneNumber(dto.getPhoneNumber());
        // registeredByUserId가 null이면 자신의 ID를 등록자로 설정 (자가 등록)
        membership.setRegisteredByUserId(
            dto.getRegisteredByUserId() != null
                ? dto.getRegisteredByUserId()
                : savedUser.getId()
        );
        membership.setStatus("APPROVED");
        membership.setIsDeleted(false);
        Memberships savedMembership = membersRepository.insert(membership);

        // 기본 역할 할당 (level=1)
        Roles defaultRole = rolesRepository.fetchByOrgIdAndLevel(orgId, 1)
                .orElseThrow(() -> new DataNotFoundException(
                    String.format("조직 ID %d에 level=1 기본 역할이 없습니다", orgId)
                ));

        membershipRolesRepository.assignRole(
            savedMembership.getId(),
            defaultRole.getId(),
            true,  // primary
            savedUser.getId()
        );

        // appTypeId 가져오기
        Organizations org = organizationsRepository.fetchById(orgId)
                .orElseThrow(() -> new DataNotFoundException(
                    String.format("조직 ID %d를 찾을 수 없습니다", orgId)
                ));

        // UserMemberDTO 반환
        return UserMemberDTO.builder()
                .user(savedUser)
                .membership(savedMembership)
                .roleIds(List.of(defaultRole.getId()))
                .appTypeId(org.getAppTypeId())
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

            Memberships existingMembership = membersRepository.fetchByUserId(userId)
                 .orElseThrow(() -> new RuntimeException("Member not found for user ID: " + userId)); // Or handle if member might not exist

            if (updateReq.getName() != null) {
                existingMembership.setName(updateReq.getName());
            }
            if (updateReq.getPicture() != null) {
                existingMembership.setPicture(updateReq.getPicture());
            }
            membersRepository.update(existingMembership);



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

            Long orgId = TenantContext.getRequiredOrgId();
            Memberships newMembership = new Memberships();
            newMembership.setOrgId(orgId);
            newMembership.setUserId(finalUserId);
            newMembership.setName(updateReq.getName());
            newMembership.setPicture(updateReq.getPicture());
            newMembership.setStatus("APPROVED");
            newMembership.setIsDeleted(false);
            Memberships savedMembership = membersRepository.insert(newMembership);

            // 기본 역할 할당 (level=1)
            Roles defaultRole = rolesRepository.fetchByOrgIdAndLevel(orgId, 1)
                    .orElseThrow(() -> new DataNotFoundException(
                        String.format("조직 ID %d에 level=1 기본 역할이 없습니다", orgId)
                    ));

            membershipRolesRepository.assignRole(
                savedMembership.getId(),
                defaultRole.getId(),
                true,
                savedUser.getId()
            );
        }

        return userRepository.fetchUserWithDetailsById(finalUserId)
             .orElseThrow(() -> new RuntimeException("Failed to fetch final user/member state after save/update for ID: " + finalUserId));
    }

    @Override
    public void deleteUser(Long userId) {
        userRepository.softDelete(userId);
    }
}
