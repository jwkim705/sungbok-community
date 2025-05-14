package com.sungbok.community.service.get;

import com.sungbok.community.dto.GetPostResponseDTO;
import com.sungbok.community.dto.GetPostsPageResponseDTO;
import com.sungbok.community.dto.PostSearchVO;

public interface GetPostsService {

    GetPostsPageResponseDTO getPostList(PostSearchVO searchVO);

    GetPostResponseDTO getPostById(Long postId, Long userId);





}
