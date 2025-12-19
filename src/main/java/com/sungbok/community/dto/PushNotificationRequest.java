package com.sungbok.community.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Expo Push Notification API 요청 DTO (내부 사용)
 * PushNotificationService에서 Expo API 호출 시 사용
 *
 * Expo Push API 문서: https://docs.expo.dev/push-notifications/sending-notifications/
 *
 * @since 0.0.1
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PushNotificationRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 수신자 Expo Push Token 목록
     * 예: ["ExponentPushToken[xxxxxx]", "ExponentPushToken[yyyyyy]"]
     */
    private List<String> to;

    /**
     * 알림 제목
     */
    private String title;

    /**
     * 알림 본문
     */
    private String body;

    /**
     * 추가 데이터 (딥링크 등에 사용)
     * 모바일 앱에서 알림 클릭 시 처리
     * 예: {"postId": 123, "screen": "PostDetail"}
     */
    private Map<String, Object> data;

    /**
     * 알림 우선순위
     * 기본값: "default", 높음: "high"
     */
    private String priority;

    /**
     * 알림 사운드
     * 기본값: "default"
     */
    private String sound;

    /**
     * 배지 숫자 (iOS)
     */
    private Integer badge;
}
