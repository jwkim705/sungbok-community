package com.sungbok.community.common.exception;

import java.io.Serial;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public class UnAuthorizedException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 3824661093355980858L;

  private HttpStatusCode code = HttpStatus.UNAUTHORIZED;
  private final String message;

  public UnAuthorizedException(String message) {
    super(message);
    this.message = message;
  }

  public UnAuthorizedException(String message, Throwable cause) {
    super(message, cause);
    this.message = message;
  }

  public UnAuthorizedException(HttpStatusCode code, String message) {
    super(message);
    this.code = code;
    this.message = message;
  }
}
