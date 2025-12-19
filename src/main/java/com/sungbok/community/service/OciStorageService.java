package com.sungbok.community.service;

import com.sungbok.community.common.exception.SystemException;
import com.sungbok.community.common.exception.code.SystemErrorCode;
import com.sungbok.community.config.OciStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * OCI Object Storage 서비스 (S3 호환 API)
 * Pre-signed URL 생성, 파일 삭제, 다운로드
 *
 * @since 0.0.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OciStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final OciStorageProperties ociStorageProperties;

    /**
     * S3 작업 실행 헬퍼 (예외 처리 공통화)
     *
     * @param operationName 작업 이름 (로깅용)
     * @param operation 실행할 S3 작업
     * @param errorCode 실패 시 에러 코드
     * @return 작업 결과
     * @param <T> 반환 타입
     * @throws SystemException S3 작업 실패 시
     */
    private <T> T executeS3Operation(
            String operationName,
            Supplier<T> operation,
            SystemErrorCode errorCode
    ) {
        try {
            T result = operation.get();
            log.info("[OCI S3] {} 성공", operationName);
            return result;
        } catch (Exception e) {
            log.error("[OCI S3] {} 실패", operationName, e);
            throw new SystemException(errorCode, e);
        }
    }

    /**
     * void 반환 S3 작업 실행 헬퍼
     *
     * @param operationName 작업 이름 (로깅용)
     * @param operation 실행할 S3 작업
     * @param errorCode 실패 시 에러 코드
     * @throws SystemException S3 작업 실패 시
     */
    private void executeS3OperationVoid(
            String operationName,
            Runnable operation,
            SystemErrorCode errorCode
    ) {
        try {
            operation.run();
            log.info("[OCI S3] {} 성공", operationName);
        } catch (Exception e) {
            log.error("[OCI S3] {} 실패", operationName, e);
            throw new SystemException(errorCode, e);
        }
    }

    /**
     * 업로드용 Pre-signed URL 생성
     *
     * @param objectKey Object Storage 키
     * @param contentType Content-Type
     * @param contentLength Content-Length (사용 안 함, 호환성 유지)
     * @param expiration 만료 시간
     * @return Pre-signed Upload URL
     */
    public String generatePresignedUploadUrl(String objectKey, String contentType,
                                              long contentLength, Duration expiration) {
        return executeS3Operation(
                "Pre-signed Upload URL 생성 (key=" + objectKey + ")",
                () -> {
                    PutObjectRequest putRequest = PutObjectRequest.builder()
                            .bucket(ociStorageProperties.getBucketName())
                            .key(objectKey)
                            .contentType(contentType)
                            .build();

                    PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                            .putObjectRequest(putRequest)
                            .signatureDuration(expiration)
                            .build();

                    return s3Presigner.presignPutObject(presignRequest).url().toString();
                },
                SystemErrorCode.FILE_UPLOAD_FAILED
        );
    }

    /**
     * 다운로드용 Pre-signed URL 생성
     *
     * @param objectKey Object Storage 키
     * @param expiration 만료 시간
     * @return Pre-signed Download URL
     */
    public String generatePresignedDownloadUrl(String objectKey, Duration expiration) {
        return executeS3Operation(
                "Pre-signed Download URL 생성 (key=" + objectKey + ")",
                () -> {
                    GetObjectRequest getRequest = GetObjectRequest.builder()
                            .bucket(ociStorageProperties.getBucketName())
                            .key(objectKey)
                            .build();

                    GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                            .getObjectRequest(getRequest)
                            .signatureDuration(expiration)
                            .build();

                    return s3Presigner.presignGetObject(presignRequest).url().toString();
                },
                SystemErrorCode.FILE_DOWNLOAD_FAILED
        );
    }

    /**
     * 파일 삭제
     *
     * @param objectKey Object Storage 키
     */
    public void deleteFile(String objectKey) {
        executeS3OperationVoid(
                "파일 삭제 (key=" + objectKey + ")",
                () -> {
                    DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                            .bucket(ociStorageProperties.getBucketName())
                            .key(objectKey)
                            .build();

                    s3Client.deleteObject(deleteRequest);
                },
                SystemErrorCode.FILE_DELETE_FAILED
        );
    }

    /**
     * 부분 다운로드 (Magic Number 검증용)
     * Range: bytes=0-1048575 (첫 1MB)
     *
     * @param objectKey Object Storage 키
     * @param startByte 시작 바이트
     * @param endByte 끝 바이트
     * @return InputStream
     */
    public InputStream downloadPartial(String objectKey, long startByte, long endByte) {
        return executeS3Operation(
                "파일 부분 다운로드 (key=" + objectKey + ", range=" + startByte + "-" + endByte + ")",
                () -> {
                    String range = String.format("bytes=%d-%d", startByte, endByte);
                    GetObjectRequest getRequest = GetObjectRequest.builder()
                            .bucket(ociStorageProperties.getBucketName())
                            .key(objectKey)
                            .range(range)
                            .build();

                    return s3Client.getObject(getRequest);
                },
                SystemErrorCode.FILE_DOWNLOAD_FAILED
        );
    }

    /**
     * 전체 파일 다운로드 (FFmpeg Probing용)
     *
     * @param objectKey Object Storage 키
     * @return InputStream
     */
    public InputStream downloadFull(String objectKey) {
        return executeS3Operation(
                "파일 다운로드 (key=" + objectKey + ")",
                () -> {
                    GetObjectRequest getRequest = GetObjectRequest.builder()
                            .bucket(ociStorageProperties.getBucketName())
                            .key(objectKey)
                            .build();

                    return s3Client.getObject(getRequest);
                },
                SystemErrorCode.FILE_DOWNLOAD_FAILED
        );
    }

    /**
     * 파일 존재 확인
     *
     * @param objectKey Object Storage 키
     * @return 존재 여부
     */
    public boolean objectExists(String objectKey) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(ociStorageProperties.getBucketName())
                    .key(objectKey)
                    .build();

            s3Client.headObject(headRequest);
            return true;

        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.error("파일 존재 확인 실패: objectKey={}", objectKey, e);
            throw new SystemException(SystemErrorCode.INTERNAL_ERROR, e);
        }
    }

    /**
     * Object Key 생성
     * 형식: {orgId}/{entityType}/{yyyy}/{MM}/{uuid}.{ext}
     * 예시: 1/post/2025/12/550e8400-e29b-41d4-a716-446655440000.jpg
     *
     * @param orgId 조직 ID
     * @param entityType 엔티티 타입 (post, comment, user_profile 등)
     * @param filename 원본 파일명 (확장자 추출용)
     * @return Object Key
     */
    public String buildObjectKey(Long orgId, String entityType, String filename) {
        LocalDateTime now = LocalDateTime.now();
        String year = now.format(DateTimeFormatter.ofPattern("yyyy"));
        String month = now.format(DateTimeFormatter.ofPattern("MM"));

        // UUID 생성
        String uuid = UUID.randomUUID().toString();

        // 파일 확장자 추출
        String extension = "";
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            extension = filename.substring(dotIndex); // .jpg
        }

        // {orgId}/{entityType}/{yyyy}/{MM}/{uuid}.{ext}
        String objectKey = String.format("%d/%s/%s/%s/%s%s",
                orgId, entityType, year, month, uuid, extension);

        log.debug("Object Key 생성: {}", objectKey);
        return objectKey;
    }

    /**
     * CDN URL 생성
     *
     * @param objectKey Object Storage 키
     * @return CDN URL
     */
    public String buildCdnUrl(String objectKey) {
        OciStorageProperties.Cdn cdnConfig = ociStorageProperties.getCdn();
        if (cdnConfig != null && cdnConfig.isEnabled()) {
            String domain = cdnConfig.getDomain();
            if (domain != null && !domain.isEmpty()) {
                return String.format("https://%s/%s", domain, objectKey);
            }
        }
        // CDN 비활성화 시에는 Pre-signed URL을 사용해야 함
        return null;
    }
}
