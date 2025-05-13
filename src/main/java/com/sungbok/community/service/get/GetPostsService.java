package com.sungbok.community.service.get;

import com.sungbok.community.dto.GetPostResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GetPostsService {

    Page<GetPostResponseDTO> getPostList(Pageable pageable);

    GetPostResponseDTO getPostById(Long postId, Long userId);





}
