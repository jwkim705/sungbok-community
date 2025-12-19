package com.sungbok.community.service;

import com.sungbok.community.common.exception.ValidationException;
import com.sungbok.community.common.exception.code.ValidationErrorCode;
import com.sungbok.community.config.OciStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 파일 메타데이터 검증 서비스
 * MIME 타입, 파일 크기, 파일명 검증
 * 확장 가능한 검증 로직 (커스텀 파라미터 지원)
 *
 * @since 0.0.1
 */
@Slf4j
@Service
public class FileValidationService {

    private final OciStorageProperties ociStorageProperties;

    /**
     * 정확한 MIME 타입 (와일드카드 없음)
     * O(1) 조회를 위한 Set
     */
    private final Set<String> exactMimeTypes;

    /**
     * 와일드카드 MIME 타입 접두사
     * 예: "image/*" → "image/"
     */
    private final List<String> wildcardPrefixes;

    private static final Pattern SANITIZE_PATTERN = Pattern.compile("[^a-zA-Z0-9._\\-가-힣]");
    private static final int MAX_FILENAME_LENGTH = 255;

    /**
     * 생성자: MIME 타입 최적화를 위해 정확한 타입과 와일드카드 분리
     */
    public FileValidationService(OciStorageProperties ociStorageProperties) {
        this.ociStorageProperties = ociStorageProperties;

        // MIME 타입 최적화: 정확한 타입과 와일드카드 분리
        List<String> allowedTypes = ociStorageProperties.getAllowedMimeTypes();

        this.exactMimeTypes = new HashSet<>();
        this.wildcardPrefixes = new ArrayList<>();

        if (allowedTypes != null) {
            for (String type : allowedTypes) {
                if (type.endsWith("*")) {
                    // "image/*" → "image/" 로 저장
                    wildcardPrefixes.add(type.substring(0, type.length() - 1));
                } else {
                    exactMimeTypes.add(type);
                }
            }
        }

        log.info("[파일 검증] MIME 타입 초기화: 정확={}, 와일드카드={}",
                exactMimeTypes.size(), wildcardPrefixes.size());
    }

    /**
     * 파일 크기 검증 (확장 가능)
     *
     * @param size 파일 크기 (바이트)
     * @param mimeType MIME 타입
     * @param customMaxSize 커스텀 최대 크기 (선택사항, null이면 기본 규칙 적용)
     * @throws ValidationException 크기 초과 시
     */
    public void validateFileSize(long size, String mimeType, Long customMaxSize) {
        long maxSize;

        if (customMaxSize != null) {
            // 커스텀 최대 크기 사용
            maxSize = customMaxSize;
        } else {
            // 기본 규칙: 동영상 100MB, 기타 10MB
            if (mimeType.startsWith("video/")) {
                maxSize = ociStorageProperties.getMaxFileSize().getVideo();
            } else {
                maxSize = ociStorageProperties.getMaxFileSize().getDefaultSize();
            }
        }

        if (size > maxSize) {
            long maxSizeMB = maxSize / 1024 / 1024;
            log.warn("파일 크기 초과: size={}, maxSize={}MB", size, maxSizeMB);
            throw new ValidationException(
                    ValidationErrorCode.FILE_TOO_LARGE,
                    Map.of("maxSize", maxSizeMB + "MB", "actualSize", size + " bytes")
            );
        }
    }

    /**
     * MIME 타입 검증 (확장 가능)
     * O(1) 정확한 타입 조회 + O(m) 와일드카드 검사 (m은 와일드카드 개수, 보통 5개 이하)
     *
     * @param mimeType MIME 타입
     * @param customAllowedTypes 커스텀 허용 목록 (선택사항, null이면 전역 설정 사용)
     * @throws ValidationException 허용되지 않은 타입일 시
     */
    public void validateMimeType(String mimeType, List<String> customAllowedTypes) {
        // 커스텀 허용 목록이 있으면 기존 O(n) 방식 사용
        if (customAllowedTypes != null) {
            boolean isAllowed = customAllowedTypes.stream()
                    .anyMatch(allowed -> {
                        if (allowed.endsWith("*")) {
                            String prefix = allowed.substring(0, allowed.length() - 1);
                            return mimeType.startsWith(prefix);
                        }
                        return mimeType.equals(allowed);
                    });

            if (!isAllowed) {
                log.warn("허용되지 않은 MIME 타입: {}", mimeType);
                throw new ValidationException(
                        ValidationErrorCode.INVALID_FILE_TYPE,
                        Map.of("mimeType", mimeType, "allowedTypes", customAllowedTypes)
                );
            }
            return;
        }

        // 전역 설정 사용 시 최적화된 O(1) 조회
        if (exactMimeTypes.isEmpty() && wildcardPrefixes.isEmpty()) {
            return;  // 제한 없음
        }

        // O(1) 정확한 MIME 타입 조회
        if (exactMimeTypes.contains(mimeType)) {
            return;  // 허용
        }

        // O(m) 와일드카드 검사 (m은 와일드카드 개수, 보통 5개 이하)
        for (String prefix : wildcardPrefixes) {
            if (mimeType.startsWith(prefix)) {
                return;  // 허용
            }
        }

        // 불허
        log.warn("허용되지 않은 MIME 타입: {}", mimeType);
        throw new ValidationException(
                ValidationErrorCode.INVALID_FILE_TYPE,
                Map.of("mimeType", mimeType,
                        "exactAllowed", exactMimeTypes.size(),
                        "wildcardAllowed", wildcardPrefixes.size())
        );
    }

    /**
     * 경로 순회 공격 패턴 검증
     * 13가지 위험 패턴 검사
     *
     * @param filename 파일명
     * @return 위험한 패턴 포함 여부
     */
    private boolean containsPathTraversal(String filename) {
        if (filename == null) {
            return true;
        }

        // 위험한 패턴들
        String[] dangerousPatterns = {
                "..",           // 상위 디렉토리
                "./",           // 현재 디렉토리
                "../",          // 상위 디렉토리 경로
                "..\\",         // Windows 상위 디렉토리
                ".\\",          // Windows 현재 디렉토리
                "/",            // 절대 경로
                "\\",           // Windows 절대 경로
                "\0",           // Null byte injection
                "%00",          // URL-encoded null byte
                "%2e%2e",       // URL-encoded ..
                "%2f",          // URL-encoded /
                "%5c"           // URL-encoded \
        };

        String lowerFilename = filename.toLowerCase();
        for (String pattern : dangerousPatterns) {
            if (lowerFilename.contains(pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 파일명 길이 제한 적용 (확장자 보존)
     *
     * @param filename 파일명
     * @param maxLength 최대 길이
     * @return 제한된 파일명
     */
    private String limitFilenameLength(String filename, int maxLength) {
        if (filename.length() <= maxLength) {
            return filename;
        }

        int dotIndex = filename.lastIndexOf('.');

        // 확장자 없음 → 단순 자르기
        if (dotIndex == -1 || dotIndex == 0) {
            return filename.substring(0, maxLength);
        }

        String extension = filename.substring(dotIndex);
        String nameWithoutExt = filename.substring(0, dotIndex);

        // 확장자가 너무 긴 경우 (예: 250자)
        if (extension.length() >= maxLength) {
            return filename.substring(0, maxLength);
        }

        // 이름 부분만 제한
        int maxNameLength = maxLength - extension.length();
        return nameWithoutExt.substring(0, Math.max(1, maxNameLength)) + extension;
    }

    /**
     * 파일명 정제
     * - 경로 순회 공격 방어 (13가지 패턴)
     * - 특수문자 제거 (한글, 영문, 숫자, ., _, - 만 허용)
     * - 길이 제한: 255자 (확장자 보존)
     *
     * @param filename 원본 파일명
     * @return 정제된 파일명
     * @throws ValidationException 잘못된 파일명인 경우
     */
    public String sanitizeFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new ValidationException(
                    ValidationErrorCode.INVALID_FILE_NAME,
                    Map.of("reason", "파일명이 비어있습니다")
            );
        }

        String trimmed = filename.trim();

        // 1. 경로 순회 공격 검증
        if (containsPathTraversal(trimmed)) {
            throw new ValidationException(
                    ValidationErrorCode.INVALID_FILE_NAME,
                    Map.of("filename", filename,
                            "reason", "경로 순회 패턴 감지")
            );
        }

        // 2. 특수문자 제거 (한글, 영문, 숫자, ., _, - 만 허용)
        String sanitized = SANITIZE_PATTERN.matcher(trimmed).replaceAll("_");

        // 3. 연속된 밑줄 제거
        sanitized = sanitized.replaceAll("_{2,}", "_");

        // 4. 시작/끝 밑줄 제거
        sanitized = sanitized.replaceAll("^_+|_+$", "");

        // 5. 길이 제한 (확장자 보존)
        sanitized = limitFilenameLength(sanitized, MAX_FILENAME_LENGTH);

        // 6. 최종 검증
        if (sanitized.isEmpty() || sanitized.equals(".")) {
            throw new ValidationException(
                    ValidationErrorCode.INVALID_FILE_NAME,
                    Map.of("filename", filename,
                            "reason", "정제 후 파일명이 유효하지 않음")
            );
        }

        log.debug("[파일명 정제] {} → {}", filename, sanitized);
        return sanitized;
    }
}
