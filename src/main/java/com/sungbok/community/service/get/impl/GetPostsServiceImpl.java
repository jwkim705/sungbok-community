package com.sungbok.community.service.get.impl;

import com.sungbok.community.dto.GetPostResponseDTO;
import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.repository.MembersRepository;
import com.sungbok.community.repository.PostsRepository;
import com.sungbok.community.repository.UserRepository;
import com.sungbok.community.service.get.GetPostsService;
import com.sungbok.community.service.get.GetUserService;
import lombok.RequiredArgsConstructor;
import org.jooq.generated.tables.pojos.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPostsServiceImpl implements GetPostsService {

    private final PostsRepository postsRepository;


    @Override
    public Page<GetPostResponseDTO> getPostList(Pageable pageable) {
        return null;
    }

    @Override
    public GetPostResponseDTO getPostById(Long postId, Long userId) {
        return null;
    }



}
