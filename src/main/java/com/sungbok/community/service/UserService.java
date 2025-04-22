package com.sungbok.community.service;

import com.sungbok.community.dto.UpdateUserWithMember;
import com.sungbok.community.dto.UserMemberDTO;

public interface UserService {

    UserMemberDTO getUser(Long userId);

    UserMemberDTO getUser(String email);

    UserMemberDTO saveOrUpdateUser(UpdateUserWithMember updateReq);

}
