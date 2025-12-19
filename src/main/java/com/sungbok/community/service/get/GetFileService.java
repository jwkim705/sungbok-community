package com.sungbok.community.service.get;

import com.sungbok.community.dto.FilesDTO;

import java.util.List;

/**
 * 파일 조회 서비스 (CQRS - Query)
 */
public interface GetFileService {

    /**
     * 파일 ID로 조회
     *
     * @param fileId 파일 ID
     * @return FilesDTO
     */
    FilesDTO getFileById(Long fileId);

    /**
     * 엔티티의 파일 목록 조회
     *
     * @param relatedEntityId 관련 엔티티 ID
     * @param relatedEntityType 관련 엔티티 타입
     * @return FilesDTO 리스트
     */
    List<FilesDTO> getFilesByEntity(Long relatedEntityId, String relatedEntityType);
}
