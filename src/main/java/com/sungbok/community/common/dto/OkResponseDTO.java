package com.sungbok.community.common.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;

@Getter
@Builder
public class OkResponseDTO implements Serializable {

  @Serial
  private static final long serialVersionUID = 5975254627376566119L;

  private final int code;
  private @Nullable String message;
  private @Nullable Object data;

  public OkResponseDTO(int code) {
    this.code = code;
  }

  public OkResponseDTO(int code, String message, Object data) {
    this.code = code;
    this.data = data;
    this.message = message;
  }

  public OkResponseDTO(int code, String message) {
    this.code = code;
    this.message = message;
  }

  public static OkResponseDTO of(final int code) {
    return new OkResponseDTO(code);
  }

  public static OkResponseDTO of(final int code, final String message) {
    return new OkResponseDTO(code, message);
  }

  public static OkResponseDTO of(
    final int code,
    final String message,
    final Object data
  ) {
    return new OkResponseDTO(code, message, data);
  }

  /**
   * 단순 성공 응답 (200 OK, 메시지 없음)
   */
  public static OkResponseDTO success() {
    return new OkResponseDTO(HttpStatus.OK.value());
  }

  /**
   * 메시지와 함께 성공 응답
   */
  public static OkResponseDTO success(String message) {
    return new OkResponseDTO(HttpStatus.OK.value(), message);
  }

  /**
   * 메시지와 데이터와 함께 성공 응답
   */
  public static OkResponseDTO success(String message, Object data) {
    return new OkResponseDTO(HttpStatus.OK.value(), message, data);
  }

  /**
   * 삭제 성공 응답 (삭제 건수 포함)
   */
  public static OkResponseDTO deleted(int count) {
    return new OkResponseDTO(
      HttpStatus.OK.value(),
      "Successfully deleted",
      Map.of("deletedCount", count)
    );
  }

  /**
   * 삭제 성공 응답 (리소스명 + 삭제 건수)
   */
  public static OkResponseDTO deleted(String resourceName, int count) {
    return new OkResponseDTO(
      HttpStatus.OK.value(),
      resourceName + " successfully deleted",
      Map.of("deletedCount", count)
    );
  }
}
