package com.sungbok.community.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 파일 검증 결과 DTO (내부 사용)
 * FFmpeg 검증 결과를 담는 DTO
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileValidationResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 검증 성공 여부
     */
    private boolean valid;

    /**
     * 실패 사유 (검증 실패 시)
     */
    private String errorMessage;

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
}
