package com.sungbok.community.controller;

import com.sungbok.community.common.constant.UriConstant;
import com.sungbok.community.dto.*;
import com.sungbok.community.security.model.PrincipalDetails;
import com.sungbok.community.service.change.ChangePostService;
import com.sungbok.community.service.get.GetPostsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(UriConstant.POSTS)
@RequiredArgsConstructor
public class PostController {

    private final GetPostsService getPostsService;
    private final ChangePostService changePostService;

    @PutMapping
    public ResponseEntity<AddPostResponseDTO> addPost(@RequestBody @Valid AddPostRequestDTO addPostRequest,
                                                         Authentication authentication) {
        PrincipalDetails principalDetails =  (PrincipalDetails) authentication.getPrincipal();
        UserMemberDTO user = principalDetails.getUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(changePostService.addPost(addPostRequest, user.getUserId()));
    }

    @GetMapping
    public ResponseEntity<Page<GetPostResponseDTO>> getPostList(Pageable pageable) {
        return ResponseEntity.ok(getPostsService.getPostList(pageable));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<GetPostResponseDTO> getPostById(@PathVariable("postId") Long postId,
                                                          Authentication authentication) {
        PrincipalDetails principalDetails =  (PrincipalDetails) authentication.getPrincipal();
        UserMemberDTO user = principalDetails.getUser();
        return ResponseEntity.ok(getPostsService.getPostById(postId,user.getUserId()));
    }

    @PatchMapping("/{postId}")
    public ResponseEntity<UpdatePostResponseDTO> updatePost(
            @RequestBody @Valid UpdatePostRequestDTO updatePostRequest,
            @PathVariable("postId") Long postId,
            Authentication authentication) {
        PrincipalDetails principalDetails =  (PrincipalDetails) authentication.getPrincipal();
        UserMemberDTO user = principalDetails.getUser();
        return ResponseEntity.ok(changePostService.updatePost(postId, updatePostRequest, user.getUserId()));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable("postId") Long postId) {
        changePostService.deletePost(postId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
