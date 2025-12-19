package com.sungbok.community.service.get.impl;

import com.sungbok.community.common.exception.ResourceNotFoundException;
import com.sungbok.community.common.exception.code.ResourceErrorCode;
import com.sungbok.community.config.OciStorageProperties;
import com.sungbok.community.dto.FilesDTO;
import com.sungbok.community.repository.FilesRepository;
import com.sungbok.community.service.OciStorageService;
import com.sungbok.community.service.get.GetFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.generated.tables.pojos.Files;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 파일 조회 서비스 구현체 (CQRS - Query)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetFileServiceImpl implements GetFileService {

    private final FilesRepository filesRepository;
    private final OciStorageService ociStorageService;
    private final OciStorageProperties ociStorageProperties;

    @Override
    public FilesDTO getFileById(Long fileId) {
        Files file = filesRepository.fetchById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ResourceErrorCode.NOT_FOUND,
                        Map.of("fileId", fileId)
                ));

        return toDTO(file);
    }

    @Override
    public List<FilesDTO> getFilesByEntity(Long relatedEntityId, String relatedEntityType) {
        List<Files> files = filesRepository.fetchByEntityIdAndType(relatedEntityId, relatedEntityType);

        return files.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * POJO를 DTO로 변환 (CDN URL 또는 Download URL 추가)
     */
    private FilesDTO toDTO(Files file) {
        FilesDTO.FilesDTOBuilder builder = FilesDTO.builder()
                .fileId(file.getFileId())
                .relatedEntityId(file.getRelatedEntityId())
                .relatedEntityType(file.getRelatedEntityType())
                .originalFilename(file.getOriginalFilename())
                .storedFilename(file.getStoredFilename())
                .filePath(file.getFilePath())
                .fileSize(file.getFileSize())
                .mimeType(file.getMimeType())
                .uploaderId(file.getUploaderId())
                .status(file.getStatus())
                .uploadedAt(file.getUploadedAt())
                .duration(file.getDuration())
                .resolution(file.getResolution())
                .codec(file.getCodec())
                .isDeleted(file.getIsDeleted());

        // CDN URL 또는 Download URL 추가
        String cdnUrl = ociStorageService.buildCdnUrl(file.getFilePath());
        if (cdnUrl != null) {
            builder.cdnUrl(cdnUrl);
        } else {
            // Pre-signed Download URL (1시간 만료)
            builder.downloadUrl(ociStorageService.generatePresignedDownloadUrl(
                    file.getFilePath(), Duration.ofHours(1)));
        }

        return builder.build();
    }
}
