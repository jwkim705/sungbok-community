package com.sungbok.community.common.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

@Getter
@Setter
public class PageRequestVO extends AbstractObject{
  @Serial
  private static final long serialVersionUID = 2884087881478889291L;

  @Schema(example = "1")
  private int page;

  @Schema(example = "10")
  private int size;

  @Schema(example = "후기")
  private String search;

  @Schema(example = "createdAt")
  private String sort;

  @Schema(example = "desc")
  private String direction;

  public void validate() {
      if (page < 1) {
          page = 1;
      }
      if (size < 1) {
          size = 10;
      }
      if (size > 100) {
          throw new IllegalArgumentException("Page size must not be greater than 100!");
      }
      if (!StringUtils.hasText(sort)) {
          throw new IllegalArgumentException("Sort must not be null!");
      }
      if (!StringUtils.hasText(direction)) {
          throw new IllegalArgumentException("Direction must not be null!");
      }
  }

  public Pageable toPageable() {
      return PageRequest.of(page - 1, size);
  }
}
