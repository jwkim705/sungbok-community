package com.sungbok.community.common.exception.handler;

import com.sungbok.community.common.exception.*;
import com.sungbok.community.common.exception.code.AuthErrorCode;
import com.sungbok.community.common.exception.code.SystemErrorCode;
import com.sungbok.community.common.exception.code.ValidationErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * RFC 7807 ProblemDetail 기반 전역 예외 핸들러
 *
 * 2025 Best Practice 완전 적용:
 * 1. 매직 스트링 금지 - ErrorCode Interface + 도메인 Enum 사용
 * 2. 보안 강화 - Profile 기반 Stack Trace 마스킹 (dev/prod)
 * 3. TraceId 필수 - MDC 활용, 로그와 에러 응답 연결
 * 4. `code` 필드 필수 - 프론트엔드 분기 처리 핵심
 * 5. Validation 통일 - fieldErrors 구조 표준화
 *
 * @since 1.1.0
 */
@RestControllerAdvice
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final String ERROR_BASE_URL = "https://sungbok.p-e.kr/errors/";

    private final Environment environment;  // Profile 체크용

    /**
     * 1. BaseException (비즈니스 예외) - 우리가 의도한 예외
     * 가장 높은 우선순위로 처리
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ProblemDetail> handleBaseException(
        BaseException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problem = createProblemDetail(ex, request);
        String traceId = (String) problem.getProperties().get("traceId");
        logException(ex, traceId, request);
        return ResponseEntity.status(ex.getHttpStatus()).body(problem);
    }

    /**
     * 2. Validation 예외 (⭐ Best Practice 5: fieldErrors 구조 통일)
     * @Valid 검증 실패 시 발생
     */
    @ExceptionHandler({BindException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<ProblemDetail> handleValidationException(
        BindException ex,
        HttpServletRequest request
    ) {
        // 필드별 에러를 Map으로 변환
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        ErrorCode errorCode = ValidationErrorCode.FAILED;
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, errorCode.getMessage()
        );
        setProblemDetailProperties(problem, errorCode, request);

        // ⭐ fieldErrors는 별도 프로퍼티로 추가
        if (!fieldErrors.isEmpty()) {
            problem.setProperty("fieldErrors", fieldErrors);
        }

        String traceId = (String) problem.getProperties().get("traceId");
        logValidationException(ex, traceId, request);
        return ResponseEntity.badRequest().body(problem);
    }

    /**
     * 3. Spring Security 인증 예외
     * Spring Security의 AuthenticationException을 우리의 ErrorCode로 변환
     */
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleSpringAuthenticationException(
        org.springframework.security.core.AuthenticationException ex,
        HttpServletRequest request
    ) {
        ErrorCode errorCode = AuthErrorCode.INVALID_CREDENTIALS;
        ProblemDetail problem = createProblemDetailFromErrorCode(errorCode, request);
        String traceId = (String) problem.getProperties().get("traceId");
        logSecurityException(ex, traceId, request);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    /**
     * 4. Spring Security 인가 예외
     * AccessDeniedException을 우리의 ErrorCode로 변환
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDeniedException(
        AccessDeniedException ex,
        HttpServletRequest request
    ) {
        ErrorCode errorCode = AuthErrorCode.ACCESS_DENIED;
        ProblemDetail problem = createProblemDetailFromErrorCode(errorCode, request);
        String traceId = (String) problem.getProperties().get("traceId");
        logSecurityException(ex, traceId, request);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    /**
     * 5. IllegalArgumentException (많이 사용됨)
     * 잘못된 인자로 인한 예외 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgumentException(
        IllegalArgumentException ex,
        HttpServletRequest request
    ) {
        ErrorCode errorCode = ValidationErrorCode.INVALID_FORMAT;
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, ex.getMessage()  // 원본 메시지 사용
        );
        setProblemDetailProperties(problem, errorCode, request);

        String traceId = (String) problem.getProperties().get("traceId");
        logException(ex, traceId, request);
        return ResponseEntity.badRequest().body(problem);
    }

    /**
     * 6. ⭐ 예상치 못한 예외 (Best Practice 2: 보안 강화 - Stack Trace 마스킹)
     * 최후의 방어선 - 모든 예외를 포착
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(
        Exception ex,
        HttpServletRequest request
    ) {
        ErrorCode errorCode = SystemErrorCode.INTERNAL_ERROR;
        ProblemDetail problem = createProblemDetailFromErrorCode(errorCode, request);

        // ⭐⭐ 운영 환경에서는 민감한 정보 숨김 (보안)
        if (isProductionMode()) {
            // 운영: 사용자에게는 generic 메시지만 노출
            problem.setDetail("일시적인 서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        } else {
            // 개발: 디버깅을 위해 상세 정보 노출
            problem.setDetail(ex.getMessage());
            problem.setProperty("exceptionType", ex.getClass().getName());
            // Stack trace는 로그에만 기록 (응답에는 절대 포함 X)
        }

        String traceId = (String) problem.getProperties().get("traceId");
        logCriticalException(ex, traceId, request);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    // ========== 헬퍼 메서드 ==========

    /**
     * BaseException으로부터 ProblemDetail 생성
     */
    private ProblemDetail createProblemDetail(BaseException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            ex.getHttpStatus(), ex.getMessage()
        );
        setProblemDetailProperties(problem, ex.getErrorCode(), request);

        // 추가 상세 정보 (예: 리소스 ID 등)
        if (ex.getDetails() != null && !ex.getDetails().isEmpty()) {
            problem.setProperty("errors", ex.getDetails());
        }

        return problem;
    }

    /**
     * ErrorCode로부터 ProblemDetail 생성
     */
    private ProblemDetail createProblemDetailFromErrorCode(ErrorCode errorCode, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            errorCode.getHttpStatus(), errorCode.getMessage()
        );
        setProblemDetailProperties(problem, errorCode, request);
        return problem;
    }

    /**
     * ⭐⭐ ProblemDetail 표준 필드 설정
     * Best Practice 4: code 필드 필수 포함 (프론트엔드 분기 처리용)
     */
    private void setProblemDetailProperties(
        ProblemDetail problem,
        ErrorCode errorCode,
        HttpServletRequest request
    ) {
        String traceId = generateOrGetTraceId();

        // RFC 7807 표준 필드
        problem.setType(URI.create(ERROR_BASE_URL + errorCode.name().toLowerCase()));
        problem.setTitle(errorCode.name());  // Enum 이름 (예: "INVALID_TOKEN")
        problem.setInstance(URI.create(request.getRequestURI()));

        // 확장 필드 (커스텀)
        problem.setProperty("timestamp", Instant.now().toString());
        problem.setProperty("traceId", traceId);  // ⭐ Best Practice 3
        problem.setProperty("code", errorCode.getCode());  // ⭐⭐ Best Practice 4: 프론트엔드 핵심!
    }

    /**
     * ⭐ Best Practice 3: TraceId 생성 (로그와 에러 응답 연결)
     * MDC에서 가져오거나 새로 생성
     */
    private String generateOrGetTraceId() {
        String traceId = MDC.get("traceId");
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
            MDC.put("traceId", traceId);
        }
        return traceId;
    }

    /**
     * ⭐ Best Practice 2: Profile 체크 (보안)
     * 운영 환경 여부 확인
     */
    private boolean isProductionMode() {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }

    // ========== 로깅 메서드 ==========

    /**
     * 일반 예외 로그 (ERROR 레벨)
     */
    private void logException(Throwable ex, String traceId, HttpServletRequest request) {
        log.error("[traceId={}] Exception: type={}, path={}, message={}",
            traceId, ex.getClass().getSimpleName(), request.getRequestURI(), ex.getMessage(), ex);
    }

    /**
     * Validation 예외 로그 (WARN 레벨)
     */
    private void logValidationException(BindException ex, String traceId, HttpServletRequest request) {
        log.warn("[traceId={}] Validation failed: path={}, errors={}",
            traceId, request.getRequestURI(), ex.getFieldErrors());
    }

    /**
     * Security 예외 로그 (WARN 레벨)
     */
    private void logSecurityException(Exception ex, String traceId, HttpServletRequest request) {
        log.warn("[traceId={}] Security exception: type={}, path={}, message={}",
            traceId, ex.getClass().getSimpleName(), request.getRequestURI(), ex.getMessage());
    }

    /**
     * 시스템 예외 로그 (ERROR 레벨, Stack Trace 포함)
     * ⭐ 예상치 못한 예외는 반드시 ERROR로 기록
     */
    private void logCriticalException(Exception ex, String traceId, HttpServletRequest request) {
        log.error("[traceId={}] Unexpected exception: type={}, path={}, message={}",
            traceId, ex.getClass().getName(), request.getRequestURI(), ex.getMessage(), ex);
    }
}
