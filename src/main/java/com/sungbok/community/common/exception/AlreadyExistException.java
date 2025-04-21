package com.sungbok.community.common.exception;

import java.io.Serial;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public class AlreadyExistException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 8676021598980993341L;

  private HttpStatusCode code = HttpStatus.CONFLICT;
  private final String message;

  public AlreadyExistException(String message) {
    super(message);
    this.message = message;
  }

  public AlreadyExistException(String message, Throwable cause) {
    super(message, cause);
    this.message = message;
  }

  public AlreadyExistException(HttpStatusCode code, String message) {
    super(message);
    this.code = code;
    this.message = message;
  }
}
