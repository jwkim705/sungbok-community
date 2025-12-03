package com.sungbok.community.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

@Getter
@Setter
@NoArgsConstructor
public class ErrorResponseDTO implements Serializable {

  @Serial
  private static final long serialVersionUID = -5325921320607581776L;

  @Schema(description = "Error Code")
  private int code;

  @Schema(description = "Error Message")
  private @Nullable String message;

  @Schema(description = "Error Item")
  private @Nullable Object errors;

  private ErrorResponseDTO(int code) {
    this.code = code;
  }

  private ErrorResponseDTO(int code, String message, Object errors) {
    this.code = code;
    this.message = message;
    this.errors = errors;
  }

  private ErrorResponseDTO(int code, String message) {
    this.code = code;
    this.message = message;
  }

  public static ErrorResponseDTO of(final int code) {
    return new ErrorResponseDTO(code);
  }

  public static ErrorResponseDTO of(final int code, final String message) {
    return new ErrorResponseDTO(code, message);
  }

  public static ErrorResponseDTO of(
    final int code,
    final String message,
    final Object errors
  ) {
    return new ErrorResponseDTO(code, message, errors);
  }
}
