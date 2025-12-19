package com.sungbok.community.repository;

import com.sungbok.community.security.TenantContext;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.generated.tables.daos.FilesDao;
import org.jooq.generated.tables.pojos.Files;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.sungbok.community.repository.util.JooqTenantConditionUtils.orgIdCondition;
import static org.jooq.generated.Tables.FILES;

/**
 * 파일 데이터 접근 Repository
 * 하이브리드 DAO + DSL 패턴 사용
 */
@Repository
public class FilesRepository {

    private final DSLContext dslContext;
    private final FilesDao dao;

    public FilesRepository(DSLContext dslContext, Configuration configuration) {
        this.dslContext = dslContext;
        this.dao = new FilesDao(configuration);
    }

    /**
     * 파일 ID로 조회 (org_id 자동 필터링)
     *
     * @param fileId 파일 ID
     * @return 파일 Optional (없으면 빈 Optional)
     */
    public Optional<Files> fetchById(Long fileId) {
        return dslContext.selectFrom(FILES)
                .where(orgIdCondition(FILES.ORG_ID))
                .and(FILES.FILE_ID.eq(fileId))
                .and(FILES.IS_DELETED.eq(false))
                .fetchOptionalInto(Files.class);
    }

    /**
     * 관련 엔티티로 파일 목록 조회 (org_id 자동 필터링)
     *
     * @param relatedEntityId 관련 엔티티 ID
     * @param relatedEntityType 관련 엔티티 타입 (post, comment 등)
     * @return 파일 리스트
     */
    public List<Files> fetchByEntityIdAndType(Long relatedEntityId, String relatedEntityType) {
        return dslContext.selectFrom(FILES)
                .where(orgIdCondition(FILES.ORG_ID))
                .and(FILES.RELATED_ENTITY_ID.eq(relatedEntityId))
                .and(FILES.RELATED_ENTITY_TYPE.eq(relatedEntityType))
                .and(FILES.IS_DELETED.eq(false))
                .orderBy(FILES.CREATED_AT.desc())
                .fetchInto(Files.class);
    }

    /**
     * 파일을 삽입합니다 (PENDING 상태)
     * org_id는 TenantContext에서 자동 설정
     *
     * @param file 삽입할 파일 엔티티
     * @return 삽입된 파일 (모든 필드 포함)
     */
    public Files insert(Files file) {
        // TenantContext에서 org_id 가져오기
        Long orgId = TenantContext.getRequiredOrgId();
        file.setOrgId(orgId);  // 강제로 현재 테넌트 설정

        dao.insert(file);  // DAO 패턴 (RETURNING 자동 처리)
        return file;
    }

    /**
     * 파일 상태를 업데이트합니다 (PENDING → ACTIVE/VERIFIED/REJECTED)
     * org_id 자동 필터링
     *
     * @param fileId 파일 ID
     * @param status 새 상태
     * @return 영향받은 행 수
     */
    public int updateStatus(Long fileId, String status) {
        return dslContext.update(FILES)
                .set(FILES.STATUS, status)
                .set(FILES.MODIFIED_AT, LocalDateTime.now())
                .where(orgIdCondition(FILES.ORG_ID))
                .and(FILES.FILE_ID.eq(fileId))
                .execute();
    }

    /**
     * 업로드 완료 시각을 기록합니다 (PENDING → ACTIVE)
     * org_id 자동 필터링
     *
     * @param fileId 파일 ID
     * @return 영향받은 행 수
     */
    public int updateUploadedAt(Long fileId) {
        return dslContext.update(FILES)
                .set(FILES.UPLOADED_AT, LocalDateTime.now())
                .set(FILES.STATUS, "ACTIVE")
                .set(FILES.MODIFIED_AT, LocalDateTime.now())
                .where(orgIdCondition(FILES.ORG_ID))
                .and(FILES.FILE_ID.eq(fileId))
                .and(FILES.STATUS.eq("PENDING"))  // PENDING 상태만 업데이트
                .execute();
    }

    /**
     * 동영상 메타데이터를 업데이트합니다 (FFmpeg 검증 결과)
     * org_id 자동 필터링
     *
     * @param fileId 파일 ID
     * @param duration 재생 시간 (초)
     * @param resolution 해상도 (예: 1920x1080)
     * @param codec 코덱 (예: h264)
     * @return 영향받은 행 수
     */
    public int updateVideoMetadata(Long fileId, Double duration, String resolution, String codec) {
        return dslContext.update(FILES)
                .set(FILES.DURATION, duration)
                .set(FILES.RESOLUTION, resolution)
                .set(FILES.CODEC, codec)
                .set(FILES.STATUS, "VERIFIED")  // 검증 완료
                .set(FILES.MODIFIED_AT, LocalDateTime.now())
                .where(orgIdCondition(FILES.ORG_ID))
                .and(FILES.FILE_ID.eq(fileId))
                .execute();
    }

    /**
     * 파일을 소프트 삭제합니다
     * org_id 자동 필터링 (사용자 소유 확인)
     *
     * @param fileId 파일 ID
     * @param userId 삭제를 수행하는 사용자 ID (권한 확인용)
     * @return 영향받은 행 수
     */
    public int softDelete(Long fileId, Long userId) {
        return dslContext.update(FILES)
                .set(FILES.IS_DELETED, true)
                .set(FILES.MODIFIED_AT, LocalDateTime.now())
                .where(orgIdCondition(FILES.ORG_ID))
                .and(FILES.FILE_ID.eq(fileId))
                .and(FILES.UPLOADER_ID.eq(userId))  // 업로더만 삭제 가능
                .execute();
    }
}
