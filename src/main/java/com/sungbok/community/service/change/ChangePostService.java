package com.sungbok.community.service.change;

import com.sungbok.community.dto.AddPostRequestDTO;
import com.sungbok.community.dto.AddPostResponseDTO;
import com.sungbok.community.dto.UpdatePostRequestDTO;
import com.sungbok.community.dto.UpdatePostResponseDTO;

public interface ChangePostService {

    /**
     * 새 게시글을 추가합니다.
     *
     * @param addPostRequest 추가할 게시글 정보
     * @param userId 게시글 작성자 ID
     * @return 추가된 게시글 정보
     */
    AddPostResponseDTO addPost(AddPostRequestDTO addPostRequest, Long userId);

    /**
     * 게시글을 수정합니다.
     *
     * @param postId 수정할 게시글 ID
     * @param updatePostRequest 수정할 게시글 정보
     * @param userId 수정 요청자 ID
     * @return 수정된 게시글 정보
     */
    UpdatePostResponseDTO updatePost(Long postId, UpdatePostRequestDTO updatePostRequest, Long userId);

    /**
     * 게시글을 삭제합니다.
     *
     * @param postId 삭제할 게시글 ID
     * @param userId 삭제 요청자 ID
     */
    void deletePost(Long postId, Long userId);
}
