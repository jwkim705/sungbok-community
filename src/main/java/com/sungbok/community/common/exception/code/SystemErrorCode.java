package com.sungbok.community.common.exception.code;

import com.sungbok.community.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 시스템 에러 코드
 * 예상치 못한 서버 내부 에러 (500) 정의
 *
 * @since 1.1.0
 */
@Getter
@RequiredArgsConstructor
public enum SystemErrorCode implements ErrorCode {

    INTERNAL_ERROR("SYS_001", HttpStatus.INTERNAL_SERVER_ERROR, "내부 서버 오류가 발생했습니다"),
    FILE_UPLOAD_FAILED("SYS_002", HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다"),
    FILE_DELETE_FAILED("SYS_003", HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제에 실패했습니다"),
    FILE_DOWNLOAD_FAILED("SYS_004", HttpStatus.INTERNAL_SERVER_ERROR, "파일 다운로드에 실패했습니다");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;
}
