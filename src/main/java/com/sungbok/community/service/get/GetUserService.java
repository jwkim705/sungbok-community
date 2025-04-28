package com.sungbok.community.service.get;

import com.sungbok.community.dto.UserMemberDTO;

public interface GetUserService {

    UserMemberDTO getUser(Long userId);

    UserMemberDTO getUser(String email);

}
