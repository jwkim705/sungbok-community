package com.sungbok.community.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Accessors(chain = true)
@NoArgsConstructor // 기본 생성자 추가
@AllArgsConstructor // 모든 필드를 받는 생성자 추가
@Builder
public class UpdatePostResponseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -2382583091496994088L;

    private Long postId;

    private String title;

    private String content;

    private String categoryId;

    private String categoryNm;

    private Long userId;

    private int viewCount;

    private int likeCount;

    private boolean isDeleted;

    private LocalDateTime createdAt;

    private long createdBy;

    private LocalDateTime modifiedAt;

    private long modifiedBy;

}
