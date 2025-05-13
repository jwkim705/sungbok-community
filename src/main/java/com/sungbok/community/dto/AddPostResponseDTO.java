package com.sungbok.community.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jooq.generated.tables.pojos.PostYoutube;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Accessors(chain = true)
@NoArgsConstructor // 기본 생성자 추가
@AllArgsConstructor // 모든 필드를 받는 생성자 추가
@Builder
public class AddPostResponseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -9029952365383610203L;

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
