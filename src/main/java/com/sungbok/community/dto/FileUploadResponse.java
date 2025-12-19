package com.sungbok.community.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 파일 업로드 응답 DTO
 * Pre-signed Upload URL 생성 응답
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FileUploadResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * DB에 저장된 파일 ID (PENDING 상태)
     */
    private Long fileId;

    /**
     * Pre-signed Upload URL (10분 만료)
     * 클라이언트가 이 URL로 직접 OCI에 파일 업로드
     */
    private String uploadUrl;

    /**
     * OCI Object Key (예: 1/post/2025/12/uuid.jpg)
     */
    private String objectKey;

    /**
     * URL 만료 시각
     */
    private LocalDateTime expiresAt;

    /**
     * 서명된 MIME 타입 (Content-Type)
     * 클라이언트는 OCI 업로드 시 이 값을 Content-Type 헤더로 사용해야 함
     */
    private String mimeType;
}
