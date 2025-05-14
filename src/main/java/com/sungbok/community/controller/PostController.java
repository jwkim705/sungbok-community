package com.sungbok.community.controller;

import com.sungbok.community.common.constant.UriConstant;
import com.sungbok.community.dto.AddPostRequestDTO;
import com.sungbok.community.dto.AddPostResponseDTO;
import com.sungbok.community.dto.GetPostResponseDTO;
import com.sungbok.community.dto.GetPostsPageResponseDTO;
import com.sungbok.community.dto.PostSearchVO;
import com.sungbok.community.dto.UpdatePostRequestDTO;
import com.sungbok.community.dto.UpdatePostResponseDTO;
import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.security.model.PrincipalDetails;
import com.sungbok.community.service.change.ChangePostService;
import com.sungbok.community.service.get.GetPostsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(UriConstant.POSTS)
@RequiredArgsConstructor
public class PostController {

    private final GetPostsService getPostsService;
    private final ChangePostService changePostService;

    @GetMapping
    public ResponseEntity<GetPostsPageResponseDTO> getPostList(PostSearchVO searchVO) {
        searchVO.validate();
        return ResponseEntity.ok(getPostsService.getPostList(searchVO));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<GetPostResponseDTO> getPostById(@PathVariable("postId") Long postId,
        Authentication authentication) {
        PrincipalDetails principalDetails =  (PrincipalDetails) authentication.getPrincipal();
        UserMemberDTO user = principalDetails.getUser();
        return ResponseEntity.ok(getPostsService.getPostById(postId,user.getUserId()));
    }

    @PutMapping
    public ResponseEntity<AddPostResponseDTO> addPost(@RequestBody @Valid AddPostRequestDTO addPostRequest,
                                                         Authentication authentication) {
        PrincipalDetails principalDetails =  (PrincipalDetails) authentication.getPrincipal();
        UserMemberDTO user = principalDetails.getUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(changePostService.addPost(addPostRequest, user.getUserId()));
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
    public ResponseEntity<Void> deletePost(@PathVariable("postId") Long postId,
                                          Authentication authentication) {
        PrincipalDetails principalDetails =  (PrincipalDetails) authentication.getPrincipal();
        UserMemberDTO user = principalDetails.getUser();
        changePostService.deletePost(postId, user.getUserId());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
