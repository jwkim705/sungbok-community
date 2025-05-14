package com.sungbok.community.service.change.impl;

import com.sungbok.community.dto.AddPostRequestDTO;
import com.sungbok.community.dto.AddPostResponseDTO;
import com.sungbok.community.dto.UpdatePostRequestDTO;
import com.sungbok.community.dto.UpdatePostResponseDTO;
import com.sungbok.community.repository.PostsRepository;
import com.sungbok.community.service.change.ChangePostService;
import com.sungbok.community.service.get.GetPostsService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.jooq.generated.tables.pojos.Posts;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class ChangePostServiceImpl implements ChangePostService {

    private final PostsRepository postsRepository;
    private final GetPostsService getPostsService;

    @Override
    public AddPostResponseDTO addPost(AddPostRequestDTO addPostRequest, Long userId) {
        // 새 게시글 생성
        Posts post = new Posts();
        post.setTitle(addPostRequest.getTitle());
        post.setContent(addPostRequest.getContent());
        
        // 카테고리 ID 설정 - 카테고리 이름으로 ID를 조회해야 할 수도 있음
        post.setCategoryNm(addPostRequest.getCategory());
        post.setUserId(userId);
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setIsDeleted(false);
        post.setCreatedBy(userId);
        post.setModifiedBy(userId);
        post.setCreatedAt(LocalDateTime.now());
        post.setModifiedAt(LocalDateTime.now());

        // 게시글 저장
        Posts savedPost = postsRepository.savePost(post);
        if (savedPost == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "게시글 저장에 실패했습니다.");
        }

        // 생성된 게시글을 DTO로 변환하여 반환
        return AddPostResponseDTO.builder()
                .postId(savedPost.getPostId())
                .title(savedPost.getTitle())
                .content(savedPost.getContent())
                .categoryNm(savedPost.getCategoryNm())
                .userId(savedPost.getUserId())
                .viewCount(savedPost.getViewCount())
                .likeCount(savedPost.getLikeCount())
                .isDeleted(savedPost.getIsDeleted())
                .createdAt(savedPost.getCreatedAt())
                .createdBy(savedPost.getCreatedBy())
                .modifiedAt(savedPost.getModifiedAt())
                .modifiedBy(savedPost.getModifiedBy())
                .build();
    }

    @Override
    public UpdatePostResponseDTO updatePost(Long postId, UpdatePostRequestDTO updatePostRequest, Long userId) {
        // 게시글 권한 확인
        if (!postsRepository.isPostOwnedByUser(postId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "게시글 수정 권한이 없습니다.");
        }

        // 수정할 게시글 생성
        Posts post = new Posts();
        post.setPostId(postId);
        post.setTitle(updatePostRequest.getTitle());
        post.setContent(updatePostRequest.getContent());
        post.setCategoryNm(updatePostRequest.getCategory());
        post.setUserId(userId);
        post.setModifiedBy(userId);

        // 게시글 업데이트
        boolean updated = postsRepository.updatePost(post);
        if (!updated) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없거나 수정에 실패했습니다.");
        }

        // 수정된 게시글 정보 조회하여 반환
        return UpdatePostResponseDTO
            .of(getPostsService.getPostById(postId, userId));
    }

    @Override
    public void deletePost(Long postId, Long userId) {
        // 게시글 권한 확인
        if (!postsRepository.isPostOwnedByUser(postId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "게시글 삭제 권한이 없습니다.");
        }
        
        boolean deleted = postsRepository.deletePost(postId, userId);
        if (!deleted) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없거나 삭제에 실패했습니다.");
        }
    }
}