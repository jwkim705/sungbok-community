package com.sungbok.community.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 파일 업로드 요청 DTO
 * Pre-signed Upload URL 생성 요청
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileUploadRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 원본 파일명
     */
    @NotBlank(message = "파일명은 필수입니다")
    private String originalFilename;

    /**
     * MIME 타입 (예: image/jpeg, video/mp4)
     */
    @NotBlank(message = "MIME 타입은 필수입니다")
    private String mimeType;

    /**
     * 파일 크기 (바이트)
     */
    @NotNull(message = "파일 크기는 필수입니다")
    @Min(value = 1, message = "파일 크기는 1바이트 이상이어야 합니다")
    private Long fileSize;

    /**
     * 관련 엔티티 ID (게시글 ID, 댓글 ID 등)
     */
    @NotNull(message = "관련 엔티티 ID는 필수입니다")
    private Long relatedEntityId;

    /**
     * 관련 엔티티 타입 (post, comment, user_profile 등)
     */
    @NotBlank(message = "관련 엔티티 타입은 필수입니다")
    private String relatedEntityType;
}
