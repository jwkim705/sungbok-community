package com.sungbok.community.dto;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostYoutubeDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 6070575365364697067L;

    private Long postYoutubeId;

    private Long postId;

    private String youtubeVideoId;

    private boolean isDeleted;

    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime modifiedAt;
    private Long modifiedBy;


}
