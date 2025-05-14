package com.sungbok.community.dto;

import com.sungbok.community.common.vo.CommonVO;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentsDTO extends CommonVO {

    @Serial
    private static final long serialVersionUID = -6185912561538892793L;

    private Long commentId;

    private Long postId;

    private String userId;

    private String userNm;

    private String content;

    private Long parentCommentId;

    private boolean isDeleted;

}
