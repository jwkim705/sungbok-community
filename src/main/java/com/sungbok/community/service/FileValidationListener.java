package com.sungbok.community.service;

import com.sungbok.community.dto.FileValidationResult;
import com.sungbok.community.dto.event.FileUploadedEvent;
import com.sungbok.community.repository.FilesRepository;
import com.sungbok.community.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.InputStream;

/**
 * 파일 검증 리스너
 * 파일 업로드 완료 후 비동기 검증 수행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileValidationListener {

    private final FFmpegService ffmpegService;
    private final FilesRepository filesRepository;
    private final OciStorageService ociStorageService;

    /**
     * 파일 업로드 완료 이벤트 리스너
     * 비동기로 파일 검증 수행
     * - Magic Number 검증 (실제 MIME 타입 확인)
     * - 동영상인 경우 FFmpeg로 메타데이터 추출
     *
     * @param event FileUploadedEvent
     */
    @Async
    @EventListener
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFileUploadedEvent(FileUploadedEvent event) {
        log.info("파일 검증 시작: fileId={}, objectKey={}", event.getFileId(), event.getObjectKey());

        // 멀티테넌시: Worker에서 TenantContext 설정 필요
        TenantContext.setOrgId(event.getOrgId());

        try {
            // 1. Magic Number 검증 (OCI에서 첫 1MB 다운로드)
            boolean validMagicNumber = validateMagicNumber(event.getObjectKey(), event.getMimeType());
            if (!validMagicNumber) {
                log.warn("Magic Number 검증 실패: fileId={}, objectKey={}", event.getFileId(), event.getObjectKey());
                filesRepository.updateStatus(event.getFileId(), "REJECTED");
                return;
            }

            // 2. 동영상인 경우 메타데이터 추출
            if (event.getMimeType().startsWith("video/")) {
                FileValidationResult result = ffmpegService.extractVideoMetadata(event.getObjectKey());

                if (result.isValid()) {
                    filesRepository.updateVideoMetadata(
                            event.getFileId(),
                            result.getDuration(),
                            result.getResolution(),
                            result.getCodec()
                    );
                    log.info("동영상 검증 완료: fileId={}, duration={}, resolution={}, codec={}",
                            event.getFileId(), result.getDuration(), result.getResolution(), result.getCodec());
                } else {
                    log.warn("동영상 검증 실패: fileId={}, reason={}", event.getFileId(), result.getErrorMessage());
                    filesRepository.updateStatus(event.getFileId(), "REJECTED");
                }
            } else {
                // 3. 이미지/문서는 Magic Number 검증만으로 충분 (VERIFIED)
                filesRepository.updateStatus(event.getFileId(), "VERIFIED");
                log.info("파일 검증 완료: fileId={}", event.getFileId());
            }

        } catch (Exception e) {
            log.error("파일 검증 중 오류 발생: fileId={}", event.getFileId(), e);
            filesRepository.updateStatus(event.getFileId(), "REJECTED");
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Magic Number 검증 (실제 MIME 타입 확인)
     * OCI에서 첫 1MB를 다운로드하여 Apache Tika로 검증
     *
     * @param objectKey OCI Object Key
     * @param declaredMimeType 선언된 MIME 타입 (클라이언트 제공)
     * @return 검증 성공 여부
     */
    private boolean validateMagicNumber(String objectKey, String declaredMimeType) {
        try (InputStream inputStream = ociStorageService.downloadPartial(objectKey, 0, 1048575)) {
            // Apache Tika로 실제 MIME 타입 감지
            Tika tika = new Tika();
            String detectedMimeType = tika.detect(inputStream);

            // 선언된 MIME 타입과 감지된 MIME 타입 비교
            boolean matches = declaredMimeType.equals(detectedMimeType) ||
                    (declaredMimeType.startsWith("video/") && detectedMimeType.startsWith("video/")) ||
                    (declaredMimeType.startsWith("image/") && detectedMimeType.startsWith("image/"));

            if (!matches) {
                log.warn("MIME 타입 불일치: declared={}, detected={}", declaredMimeType, detectedMimeType);
            }

            return matches;

        } catch (Exception e) {
            log.error("Magic Number 검증 실패: objectKey={}", objectKey, e);
            return false;
        }
    }
}
