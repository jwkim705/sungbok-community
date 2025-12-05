package com.sungbok.community.service.get.impl;

import com.sungbok.community.dto.GetPostResponseDTO;
import com.sungbok.community.dto.GetPostsPageResponseDTO;
import com.sungbok.community.dto.PostSearchVO;
import com.sungbok.community.repository.PostsRepository;
import com.sungbok.community.service.get.GetPostsService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPostsServiceImpl implements GetPostsService {

    private final PostsRepository postsRepository;

    @Override
    public GetPostsPageResponseDTO getPostList(PostSearchVO searchVO) {

        //카테고리 체크

        return postsRepository.fetchAllPosts(searchVO);
    }

    @Override
    @Transactional
    public GetPostResponseDTO getPostById(Long postId, Long userId) {
        // 조회수 증가 (조회하는 사용자가 게시글 작성자가 아닌 경우에만)
        GetPostResponseDTO post = postsRepository.fetchPostById(postId);

        if (Objects.isNull(post)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다.");
        }

        // 자신의 게시글이 아닌 경우에만 조회수 증가
        if (!post.getUserId().equals(userId)) {
            postsRepository.incrementViewCount(postId);
            // 증가된 조회수를 반영
            post.viewCount();
        }

        return post;
    }
}
