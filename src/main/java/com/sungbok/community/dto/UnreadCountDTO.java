package com.sungbok.community.dto;

/**
 * 읽지 않은 알림 개수 응답 DTO
 *
 * @param count 읽지 않은 알림 개수
 * @since 0.0.1
 */
public record UnreadCountDTO(
    int count
) {
}
