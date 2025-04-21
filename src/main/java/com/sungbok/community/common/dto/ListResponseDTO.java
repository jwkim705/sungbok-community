package com.sungbok.community.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.http.HttpStatus;

@Getter
@Setter
@ToString
public class ListResponseDTO<T extends Serializable> extends OkResponseDTO {

  @Serial
  private static final long serialVersionUID = 1587701251675364754L;

  @Schema(description = "반환 될 정보 리스트")
  private List<T> list;

  public ListResponseDTO(HttpStatus status) {
    super(status.value(), status.getReasonPhrase());
  }

  public ListResponseDTO(List<T> list) {
    super(HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase());
    this.list = list;
  }
}
