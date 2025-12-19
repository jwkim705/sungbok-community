package com.sungbok.community.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 파일 업로드 완료 이벤트
 * 파일 업로드 완료 후 비동기 검증 트리거
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileUploadedEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 조직 ID (멀티테넌시)
     */
    private Long orgId;

    /**
     * 파일 ID
     */
    private Long fileId;

    /**
     * OCI Object Key
     */
    private String objectKey;

    /**
     * MIME 타입
     */
    private String mimeType;
}
