package com.sungbok.community.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 감사 로그 자동 기록 어노테이션
 * AOP로 메서드 실행 시 자동으로 감사 로그 생성
 *
 * 사용 예시:
 * @Auditable(action = "POST_DELETE", resourceType = "post")
 * public void deletePost(Long postId) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /**
     * 작업 타입 (필수)
     * 예: USER_LOGIN, POST_DELETE, ROLE_CHANGE
     */
    String action();

    /**
     * 리소스 타입 (선택)
     * 예: post, user, role
     */
    String resourceType() default "";
}
