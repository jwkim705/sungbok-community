package com.sungbok.community.common.exception;

import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public class SystemException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = -6916499826206129515L;

  private HttpStatusCode code = HttpStatus.INTERNAL_SERVER_ERROR;
  private final String message;

  public SystemException(String message) {
    super(message);
    this.message = message;
  }

  public SystemException(String message, Throwable throwable) {
    super(message, throwable);
    this.message = message;
  }

  public SystemException(HttpStatusCode code, String message) {
    super(message);
    this.code = code;
    this.message = message;
  }


}
