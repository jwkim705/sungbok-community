package com.sungbok.community.service;

import com.sungbok.community.dto.AddUserRequestDTO;
import com.sungbok.community.dto.AddUserResponseDTO;
import com.sungbok.community.dto.UpdateUserWithMember;
import com.sungbok.community.dto.UserMemberDTO;

public interface UserService {

    UserMemberDTO getUser(Long userId);

    UserMemberDTO getUser(String email);

    AddUserResponseDTO signup(AddUserRequestDTO dto);

    UserMemberDTO saveOrUpdateUser(UpdateUserWithMember updateReq);

}
