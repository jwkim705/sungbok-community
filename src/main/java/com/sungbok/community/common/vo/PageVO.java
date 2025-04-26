package com.sungbok.community.common.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PageVO extends AbstractObject{
  @Serial
  private static final long serialVersionUID = 2884087881478889291L;

  @Schema(example = "0", description = "현재 페이지")
  private Integer page;

  @Schema(example = "20", description = "페이지당 표시 record 수")
  private Integer pageSize;

  @Schema(example = "0", description = "전체 record 수")
  private Long totalCounts;

  @Schema(example = "1", description = "전체 페이지 수")
  private Integer totalPages;

  public PageVO(Integer page, Integer pageSize, Long totalCounts, Integer totalPages) {
    this.page = page + 1;
    this.pageSize = pageSize;
    this.totalCounts = totalCounts;
    this.totalPages = totalPages;
  }
}
