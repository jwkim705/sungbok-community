package com.sungbok.community.service.change.impl;

import com.sungbok.community.dto.AddPostRequestDTO;
import com.sungbok.community.dto.AddPostResponseDTO;
import com.sungbok.community.dto.UpdatePostRequestDTO;
import com.sungbok.community.dto.UpdatePostResponseDTO;
import com.sungbok.community.repository.PostsRepository;
import com.sungbok.community.service.change.ChangePostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChangePostServiceImpl implements ChangePostService {

    private final PostsRepository postRepository;

    @Override
    public AddPostResponseDTO addPost(AddPostRequestDTO requestDTO, Long userId) {
        return null;
    }

    @Override
    public UpdatePostResponseDTO updatePost(Long postId, UpdatePostRequestDTO requestDTO, Long userId) {
        return null;
    }

    @Override
    public void deletePost(Long postId) {

    }
}
