package com.sungbok.community.common.exception.handler;

import com.rsupport.shuttlecock.common.dto.ErrorResponseDTO;
import com.sungbok.community.common.exception.AlreadyExistException;
import com.sungbok.community.common.exception.DataNotFoundException;
import com.sungbok.community.common.exception.NotFoundException;
import com.sungbok.community.common.exception.NotMatchPwdException;
import com.sungbok.community.common.exception.UnAuthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

@Slf4j
@RestControllerAdvice
public class ErrorHandlerAdvice {

  @ExceptionHandler(DataNotFoundException.class)
  public ResponseEntity<ErrorResponseDTO> handleDataNotFoundException(DataNotFoundException exception) {
    ErrorResponseDTO response = ErrorResponseDTO.of(exception.getCode().value(), exception.getMessage());
    log.error(exception.getMessage(), exception);
    return ResponseEntity.status(exception.getCode().value()).body(response);
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ErrorResponseDTO> handleNotFoundException(NotFoundException exception) {
    ErrorResponseDTO response = ErrorResponseDTO.of(exception.getCode().value(), exception.getMessage());
    log.error(exception.getMessage(), exception);
    return ResponseEntity.status(exception.getCode().value()).body(response);
  }

  @ExceptionHandler(UsernameNotFoundException.class)
  public ResponseEntity<ErrorResponseDTO> handleNotFoundException(UsernameNotFoundException exception) {
    ErrorResponseDTO response = ErrorResponseDTO.of(HttpStatus.NOT_FOUND.value(), exception.getMessage());
    log.error(exception.getMessage(), exception);
    return ResponseEntity.status(HttpStatus.NOT_FOUND.value()).body(response);
  }

  @ExceptionHandler(UnAuthorizedException.class)
  public ResponseEntity<ErrorResponseDTO> handleUnAuthorizedException(UnAuthorizedException exception) {
    ErrorResponseDTO response = ErrorResponseDTO.of(exception.getCode().value(), exception.getMessage());
    log.error(exception.getMessage(), exception);
    return ResponseEntity.status(exception.getCode().value()).body(response);
  }

  @ExceptionHandler(AlreadyExistException.class)
  public ResponseEntity<ErrorResponseDTO> handleAlreadyExistException(AlreadyExistException exception) {
    ErrorResponseDTO response = ErrorResponseDTO.of(exception.getCode().value(), exception.getMessage());
    log.error(exception.getMessage(), exception);
    return ResponseEntity.status(exception.getCode().value()).body(response);
  }

  @ExceptionHandler(BindException.class)
  public ResponseEntity<ErrorResponseDTO> handleMethodArgumentNotValidException(BindException exception) {
    BindingResult bindingResult = exception.getBindingResult();
    StringBuilder stringBuilder = new StringBuilder();
    for (FieldError fieldError : bindingResult.getFieldErrors()) {
      stringBuilder.append(fieldError.getField()).append(":");
      stringBuilder.append(fieldError.getDefaultMessage());
      stringBuilder.append(", ");
    }
    ErrorResponseDTO response = ErrorResponseDTO.of(HttpStatus.BAD_REQUEST.value(), exception.getMessage());
    log.error(exception.getMessage(), exception, stringBuilder);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler({AuthenticationException.class, AccessDeniedException.class})
  public ResponseEntity<ErrorResponseDTO> handleAuthenticationException(AuthenticationException exception) {
    ErrorResponseDTO response = ErrorResponseDTO.of(HttpStatus.UNAUTHORIZED.value(), exception.getMessage());
    log.error(exception.getMessage(), exception);
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
  }

  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<ErrorResponseDTO> handleNoHandlerFoundException(NoHandlerFoundException exception) {
    ErrorResponseDTO response = ErrorResponseDTO.of(HttpStatus.NOT_FOUND.value(), exception.getMessage(),exception.getRequestHeaders());
    log.error(exception.getMessage(), exception);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponseDTO> handleException(Exception exception) {
    ErrorResponseDTO response = ErrorResponseDTO.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getMessage());
    log.error(exception.getMessage(), exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }

  @ExceptionHandler(NotMatchPwdException.class)
  public ResponseEntity<ErrorResponseDTO> handleNotMatchPwdException(NotMatchPwdException exception) {
    ErrorResponseDTO response = ErrorResponseDTO.of(HttpStatus.UNAUTHORIZED.value(), exception.getMessage(),exception.getMessage());
    log.error(exception.getMessage(), exception);
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
  }
}
