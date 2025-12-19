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
public class FilesDTO extends CommonVO {

    @Serial
    private static final long serialVersionUID = -7971881140782238236L;

    private Long fileId;

    private Long relatedEntityId;

    private String relatedEntityType;

    private String originalFilename;

    private String storedFilename;

    private String filePath;

    private Long fileSize;

    private String mimeType;

    private Long uploaderId;

    private Boolean isDeleted;

    /**
     * 파일 상태 (PENDING, ACTIVE, VERIFIED, REJECTED)
     */
    private String status;

    /**
     * 업로드 완료 시각
     */
    private LocalDateTime uploadedAt;

    /**
     * 동영상 재생 시간 (초)
     */
    private Double duration;

    /**
     * 동영상 해상도 (예: 1920x1080)
     */
    private String resolution;

    /**
     * 동영상 코덱 (예: h264)
     */
    private String codec;

    /**
     * Cloudflare CDN URL (읽기 전용, public 파일용)
     */
    private String cdnUrl;

    /**
     * Pre-signed Download URL (읽기 전용, private 파일용, 1시간 만료)
     */
    private String downloadUrl;

}
