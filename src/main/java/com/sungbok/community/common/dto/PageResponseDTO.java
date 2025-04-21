package com.sungbok.community.common.dto;

import com.rsupport.shuttlecock.common.vo.PageVO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.Getter;
import lombok.ToString;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;

@Getter
@ToString
public class PageResponseDTO<T extends Serializable> extends OkResponseDTO {

  @Serial
  private static final long serialVersionUID = -5426752195787246641L;

  @Schema(description = "현제 페이지 표시될 record List")
  private List<T> list;

  private PageVO pageVO;

  public PageResponseDTO(HttpStatus status) {
    super(status.value(), status.getReasonPhrase());
  }

  public PageResponseDTO(Page<T> page) {
    super(HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase());
    this.list = page.getContent();
    this.pageVO = new PageVO(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
  }

  public PageResponseDTO(List<T> list) {
    super(HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase());
    this.list = list;
    this.pageVO = new PageVO(0, list.size(), (long) list.size(), 1);
  }

}
