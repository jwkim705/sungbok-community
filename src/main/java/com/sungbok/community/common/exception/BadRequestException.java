package com.sungbok.community.common.exception;

import java.io.Serial;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;

public class BadRequestException extends HttpClientErrorException {

  @Serial
  private static final long serialVersionUID = 7935081411605334047L;

  public BadRequestException(HttpStatusCode code) {
    super(code);
  }

  public BadRequestException(HttpStatusCode code, String message) {
    super(code, message);
  }
}
