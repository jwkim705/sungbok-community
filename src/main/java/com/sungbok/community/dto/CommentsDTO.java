package com.sungbok.community.dto;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentsDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -6185912561538892793L;

    private Long commentId;

    private Long postId;

    private String userId;

    private String userNm;

    private String content;

    private Long parentCommentId;

    private boolean isDeleted;

    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime modifiedAt;
    private Long modifiedBy;

}
