package com.sungbok.community.common.exception;

import java.io.Serial;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.authentication.BadCredentialsException;

@Getter
public class NotMatchPwdException extends BadCredentialsException {

  @Serial
  private static final long serialVersionUID = 3727302614547532722L;

  private HttpStatusCode code = HttpStatus.UNAUTHORIZED;
  private final String message;

  public NotMatchPwdException(String message) {
    super(message);
    this.message = message;
  }

  public NotMatchPwdException(String message, Throwable cause) {
    super(message, cause);
    this.message = message;
  }

  public NotMatchPwdException(HttpStatusCode code, String message) {
    super(message);
    this.code = code;
    this.message = message;
  }

}
