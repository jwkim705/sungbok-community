package com.sungbok.community.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sungbok.community.common.vo.CommonVO;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.domain.Pageable;

@Getter
@Accessors(chain = true)
@NoArgsConstructor // 기본 생성자 추가
@AllArgsConstructor // 모든 필드를 받는 생성자 추가
@Builder
public class GetPostResponseDTO extends CommonVO {

    @Serial
    private static final long serialVersionUID = 8082615997898082021L;

    private Long postId;

    private String title;

    private String content;

    private String categoryNm;

    private Long userId;

    private String email;

    private String name;

    private int viewCount;

    private int likeCount;

    private boolean isDeleted;

    private List<PostYoutubeDTO> youtube;

    private List<FilesDTO> files;

    public void viewCount() {
        this.viewCount++;
    }

}
