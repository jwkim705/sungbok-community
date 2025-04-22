package com.sungbok.community.service.impl;

import com.sungbok.community.dto.UpdateMember;
import com.sungbok.community.dto.UpdateUser;
import com.sungbok.community.dto.UpdateUserWithMember;
import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.repository.member.MembersRepository;
import com.sungbok.community.repository.users.UserRepository;
import com.sungbok.community.service.UserService;
import lombok.RequiredArgsConstructor;
import org.jooq.generated.tables.pojos.Members;
import org.jooq.generated.tables.pojos.Users;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final MembersRepository membersRepository;

    @Override
    public UserMemberDTO getUser(Long userId) {

        return null;
    }

    @Override
    public UserMemberDTO getUser(String email) {
        return null;
    }

    @Override
    public UserMemberDTO saveOrUpdateUser(UpdateUserWithMember updateReq) {

        Optional<Users> user = userRepository.findById(updateReq.getUserId());
        if (user.isPresent()) {
            UpdateUser updateUser = UpdateUser.builder()
                    .id(user.get().getId())
                    .email(updateReq.getEmail())
                    .password(updateReq.getPassword())
                    .build();
            int result = userRepository.updateOauthLogin(updateUser);
            if (result > 0) {

                Optional<Members> member = membersRepository.findByUserId(user.get().getId());
                if (member.isPresent()) {
                    UpdateMember updateMember = UpdateMember.builder()
                            .id(member.get().getId())
                            .picture(updateReq.getPicture())
                            .build();
                }
            }

        }

        return null;
    }
}
