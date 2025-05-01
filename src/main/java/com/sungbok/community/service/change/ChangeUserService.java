package com.sungbok.community.service.change;

import com.sungbok.community.dto.AddUserRequestDTO;
import com.sungbok.community.dto.AddUserResponseDTO;
import com.sungbok.community.dto.UpdateUserWithMember;
import com.sungbok.community.dto.UserMemberDTO;

public interface ChangeUserService {

    UserMemberDTO signup(AddUserRequestDTO dto);

    UserMemberDTO saveOrUpdateUser(UpdateUserWithMember updateReq);

}
