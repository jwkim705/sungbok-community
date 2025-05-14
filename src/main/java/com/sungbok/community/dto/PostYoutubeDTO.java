package com.sungbok.community.dto;

import com.sungbok.community.common.vo.CommonVO;
import java.io.Serial;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostYoutubeDTO extends CommonVO {

    @Serial
    private static final long serialVersionUID = 6070575365364697067L;

    private Long postYoutubeId;

    private Long postId;

    private String youtubeVideoId;

    private boolean isDeleted;


}
