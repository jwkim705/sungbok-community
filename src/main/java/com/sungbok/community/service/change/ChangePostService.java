package com.sungbok.community.service.change;

import com.sungbok.community.dto.AddPostRequestDTO;
import com.sungbok.community.dto.AddPostResponseDTO;
import com.sungbok.community.dto.UpdatePostRequestDTO;
import com.sungbok.community.dto.UpdatePostResponseDTO;

public interface ChangePostService {

    AddPostResponseDTO addPost(AddPostRequestDTO requestDTO, Long userId);

    UpdatePostResponseDTO updatePost(Long postId, UpdatePostRequestDTO requestDTO, Long userId);

    void deletePost(Long postId);


}
