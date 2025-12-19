package com.sungbok.community.service.change;

import com.sungbok.community.dto.FileUploadRequest;
import com.sungbok.community.dto.FileUploadResponse;

/**
 * 파일 변경 서비스 (CQRS - Command)
 */
public interface ChangeFileService {

    /**
     * Pre-signed Upload URL 생성 및 DB에 PENDING 파일 레코드 생성
     *
     * @param request FileUploadRequest
     * @param userId 업로더 사용자 ID
     * @return FileUploadResponse
     */
    FileUploadResponse createPresignedUploadUrl(FileUploadRequest request, Long userId);

    /**
     * 파일 업로드 완료 처리 (PENDING → ACTIVE)
     * FileUploadedEvent 발행 (비동기 검증 트리거)
     *
     * @param fileId 파일 ID
     * @param userId 업로더 사용자 ID
     */
    void markAsUploaded(Long fileId, Long userId);

    /**
     * 파일 삭제 (소프트 삭제 + OCI 삭제)
     *
     * @param fileId 파일 ID
     * @param userId 삭제를 수행하는 사용자 ID
     */
    void deleteFile(Long fileId, Long userId);
}
