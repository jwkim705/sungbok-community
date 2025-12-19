package com.sungbok.community.service.change.impl;

import com.sungbok.community.common.exception.AuthorizationException;
import com.sungbok.community.common.exception.ResourceNotFoundException;
import com.sungbok.community.common.exception.ValidationException;
import com.sungbok.community.common.exception.code.AuthErrorCode;
import com.sungbok.community.common.exception.code.ResourceErrorCode;
import com.sungbok.community.common.exception.code.ValidationErrorCode;
import com.sungbok.community.config.OciStorageProperties;
import com.sungbok.community.dto.FileUploadRequest;
import com.sungbok.community.dto.FileUploadResponse;
import com.sungbok.community.dto.event.FileUploadedEvent;
import com.sungbok.community.repository.FilesRepository;
import com.sungbok.community.security.TenantContext;
import com.sungbok.community.service.FileValidationService;
import com.sungbok.community.service.OciStorageService;
import com.sungbok.community.service.change.ChangeFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.generated.tables.pojos.Files;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 파일 변경 서비스 구현체 (CQRS - Command)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChangeFileServiceImpl implements ChangeFileService {

    private final FilesRepository filesRepository;
    private final OciStorageService ociStorageService;
    private final FileValidationService fileValidationService;
    private final ApplicationEventPublisher eventPublisher;
    private final OciStorageProperties ociStorageProperties;

    @Override
    public FileUploadResponse createPresignedUploadUrl(FileUploadRequest request, Long userId) {
        // 1. 파일 메타데이터 검증 (MIME 타입, 파일 크기)
        fileValidationService.validateMimeType(request.getMimeType(), null);
        fileValidationService.validateFileSize(request.getFileSize(), request.getMimeType(), null);

        // 2. 파일명 정제
        String sanitizedFilename = fileValidationService.sanitizeFilename(request.getOriginalFilename());

        // 3. Object Key 생성
        Long orgId = TenantContext.getRequiredOrgId();
        String objectKey = ociStorageService.buildObjectKey(
                orgId, request.getRelatedEntityType(), sanitizedFilename);

        // 4. DB에 PENDING 파일 레코드 생성
        Files file = new Files();
        file.setRelatedEntityId(request.getRelatedEntityId());
        file.setRelatedEntityType(request.getRelatedEntityType());
        file.setOriginalFilename(request.getOriginalFilename());
        file.setStoredFilename(objectKey.substring(objectKey.lastIndexOf('/') + 1));
        file.setFilePath(objectKey);
        file.setFileSize(request.getFileSize());
        file.setMimeType(request.getMimeType());
        file.setUploaderId(userId);
        file.setStatus("PENDING");
        file.setCreatedAt(LocalDateTime.now());
        file.setModifiedAt(LocalDateTime.now());

        Files savedFile = filesRepository.insert(file);

        // 5. Pre-signed Upload URL 생성
        Duration expiration = Duration.ofSeconds(ociStorageProperties.getPresignedUrlExpiration());
        String uploadUrl = ociStorageService.generatePresignedUploadUrl(
                objectKey,
                request.getMimeType(),
                request.getFileSize(),
                expiration
        );

        log.info("Pre-signed Upload URL 생성: fileId={}, objectKey={}", savedFile.getFileId(), objectKey);

        return FileUploadResponse.builder()
                .fileId(savedFile.getFileId())
                .uploadUrl(uploadUrl)
                .objectKey(objectKey)
                .expiresAt(LocalDateTime.now().plus(expiration))
                .mimeType(request.getMimeType())  // 프론트엔드에 서명된 Content-Type 전달
                .build();
    }

    @Override
    public void markAsUploaded(Long fileId, Long userId) {
        // 1. 파일 조회 (권한 확인)
        Files file = filesRepository.fetchById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ResourceErrorCode.NOT_FOUND,
                        Map.of("fileId", fileId)
                ));

        if (!file.getUploaderId().equals(userId)) {
            throw new AuthorizationException(AuthErrorCode.ACCESS_DENIED);
        }

        // 2. uploaded_at 업데이트 (PENDING → ACTIVE)
        int affected = filesRepository.updateUploadedAt(fileId);
        if (affected == 0) {
            log.warn("파일 업로드 완료 처리 실패: fileId={}, 이미 처리되었거나 PENDING 상태가 아님", fileId);
            throw new ValidationException(
                    ValidationErrorCode.FAILED,
                    Map.of("fileId", fileId, "reason", "이미 처리된 파일이거나 PENDING 상태가 아닙니다")
            );
        }

        // 3. FileUploadedEvent 발행 (비동기 검증 트리거)
        FileUploadedEvent event = FileUploadedEvent.builder()
                .orgId(TenantContext.getRequiredOrgId())
                .fileId(fileId)
                .objectKey(file.getFilePath())
                .mimeType(file.getMimeType())
                .build();

        eventPublisher.publishEvent(event);

        log.info("파일 업로드 완료 처리: fileId={}, event 발행", fileId);
    }

    @Override
    public void deleteFile(Long fileId, Long userId) {
        // 1. 소프트 삭제 (권한 확인 포함)
        int affected = filesRepository.softDelete(fileId, userId);

        if (affected == 0) {
            throw new ResourceNotFoundException(
                    ResourceErrorCode.NOT_FOUND,
                    Map.of("fileId", fileId)
            );
        }

        // 2. OCI 파일 삭제 (비동기)
        Files file = filesRepository.fetchById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ResourceErrorCode.NOT_FOUND,
                        Map.of("fileId", fileId)
                ));

        CompletableFuture.runAsync(() -> {
            try {
                ociStorageService.deleteFile(file.getFilePath());
                log.info("OCI 파일 삭제 완료: fileId={}, objectKey={}", fileId, file.getFilePath());
            } catch (Exception e) {
                log.error("OCI 파일 삭제 실패: fileId={}, objectKey={}", fileId, file.getFilePath(), e);
            }
        });

        log.info("파일 삭제 완료: fileId={}", fileId);
    }
}
