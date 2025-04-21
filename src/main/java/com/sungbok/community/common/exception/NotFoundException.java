package com.sungbok.community.common.exception;

import java.io.Serial;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public class NotFoundException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 350566929011547901L;

  private HttpStatusCode code = HttpStatus.NOT_FOUND;
  private final String message;

  public NotFoundException(String message) {
    super(message);
    this.message = message;
  }

  public NotFoundException(String message, Throwable cause) {
    super(message, cause);
    this.message = message;
  }

  public NotFoundException(HttpStatusCode code, String message) {
    super(message);
    this.code = code;
    this.message = message;
  }
}
