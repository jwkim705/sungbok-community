package com.sungbok.community.controller;

import com.sungbok.community.common.dto.OkResponseDTO;
import com.sungbok.community.repository.CommentsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.generated.tables.pojos.Comments;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Comments API 컨트롤러
 * Guest mode / Authenticated: 게시글 댓글 조회
 *
 * @since 0.0.1
 */
@RestController
@RequestMapping("/posts/{postId}/comments")
@RequiredArgsConstructor
@Slf4j
public class CommentsController {

    private final CommentsRepository commentsRepository;

    /**
     * GET /posts/{postId}/comments
     * 게시글의 모든 댓글 조회 (Guest mode / Authenticated)
     *
     * @param postId 게시글 ID
     * @return 댓글 리스트
     */
    @GetMapping
    public ResponseEntity<OkResponseDTO> getCommentsByPostId(
            @PathVariable Long postId) {
        List<Comments> comments = commentsRepository.fetchByPostId(postId);
        return ResponseEntity.ok(
            OkResponseDTO.of(200, "댓글 목록 조회 성공", comments)
        );
    }
}
