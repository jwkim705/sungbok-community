package com.sungbok.community.controller;

import com.sungbok.community.dto.FileUploadRequest;
import com.sungbok.community.dto.FileUploadResponse;
import com.sungbok.community.dto.FilesDTO;
import com.sungbok.community.security.model.PrincipalDetails;
import com.sungbok.community.service.change.ChangeFileService;
import com.sungbok.community.service.get.GetFileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 파일 REST API 컨트롤러
 */
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Slf4j
public class FilesController {

    private final ChangeFileService changeFileService;
    private final GetFileService getFileService;

    /**
     * POST /api/files/upload-presigned
     * Pre-signed Upload URL 생성 (파일 메타데이터 전송)
     *
     * @param userDetails 인증된 사용자 정보 (JWT)
     * @param request FileUploadRequest
     * @return FileUploadResponse (uploadUrl, fileId, objectKey, expiresAt)
     */
    @PostMapping("/upload-presigned")
    public ResponseEntity<FileUploadResponse> createPresignedUploadUrl(
            @AuthenticationPrincipal PrincipalDetails userDetails,
            @Valid @RequestBody FileUploadRequest request
    ) {
        Long userId = userDetails.getUser().getUserId();
        FileUploadResponse response = changeFileService.createPresignedUploadUrl(request, userId);

        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/files/{fileId}/uploaded
     * 파일 업로드 완료 알림 (클라이언트가 OCI 업로드 완료 후 호출)
     *
     * @param userDetails 인증된 사용자 정보 (JWT)
     * @param fileId 파일 ID
     * @return 성공 응답
     */
    @PutMapping("/{fileId}/uploaded")
    public ResponseEntity<Void> markAsUploaded(
            @AuthenticationPrincipal PrincipalDetails userDetails,
            @PathVariable Long fileId
    ) {
        Long userId = userDetails.getUser().getUserId();
        changeFileService.markAsUploaded(fileId, userId);

        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/files/{fileId}
     * 파일 정보 조회
     *
     * @param userDetails 인증된 사용자 정보 (JWT)
     * @param fileId 파일 ID
     * @return FilesDTO (CDN URL 또는 Download URL 포함)
     */
    @GetMapping("/{fileId}")
    public ResponseEntity<FilesDTO> getFileById(
            @AuthenticationPrincipal PrincipalDetails userDetails,
            @PathVariable Long fileId
    ) {
        FilesDTO file = getFileService.getFileById(fileId);

        return ResponseEntity.ok(file);
    }

    /**
     * GET /api/files
     * 엔티티의 파일 목록 조회
     *
     * @param userDetails 인증된 사용자 정보 (JWT)
     * @param relatedEntityId 관련 엔티티 ID
     * @param relatedEntityType 관련 엔티티 타입 (post, comment 등)
     * @return FilesDTO 리스트
     */
    @GetMapping
    public ResponseEntity<List<FilesDTO>> getFilesByEntity(
            @AuthenticationPrincipal PrincipalDetails userDetails,
            @RequestParam Long relatedEntityId,
            @RequestParam String relatedEntityType
    ) {
        List<FilesDTO> files = getFileService.getFilesByEntity(relatedEntityId, relatedEntityType);

        return ResponseEntity.ok(files);
    }

    /**
     * DELETE /api/files/{fileId}
     * 파일 삭제 (소프트 삭제 + OCI 삭제)
     *
     * @param userDetails 인증된 사용자 정보 (JWT)
     * @param fileId 파일 ID
     * @return 성공 응답
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(
            @AuthenticationPrincipal PrincipalDetails userDetails,
            @PathVariable Long fileId
    ) {
        Long userId = userDetails.getUser().getUserId();
        changeFileService.deleteFile(fileId, userId);

        return ResponseEntity.noContent().build();
    }
}
