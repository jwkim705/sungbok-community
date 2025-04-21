package com.sungbok.community.common.dto;

import com.rsupport.shuttlecock.common.vo.AbstractObject;
import java.io.Serial;
import lombok.Getter;

@Getter
public class OkResponseDTO extends AbstractObject {

  @Serial
  private static final long serialVersionUID = 5975254627376566119L;

  private final int code;
  private String message;
  private Object data;

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
}
