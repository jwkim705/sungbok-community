package com.sungbok.community.enums;

import lombok.Getter;

/**
 * 알림 타입 Enum
 *
 * <p>푸시 알림의 종류를 정의합니다.
 */
@Getter
public enum NotificationType {
    /**
     * 멤버십 승인 알림
     */
    MEMBERSHIP_APPROVED("멤버십 승인"),

    /**
     * 멤버십 거절 알림
     */
    MEMBERSHIP_REJECTED("멤버십 거절"),

    /**
     * 게시글 댓글 알림 (향후 구현)
     */
    POST_COMMENT("게시글 댓글"),

    /**
     * 게시글 좋아요 알림 (향후 구현)
     */
    POST_LIKE("게시글 좋아요"),

    /**
     * 관리자 공지 알림 (향후 구현)
     */
    ADMIN_ANNOUNCEMENT("관리자 공지");

    /**
     * -- GETTER --
     *  알림 타입의 한글 설명을 반환합니다.
     *
     * @return 알림 타입 설명
     */
    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

}
