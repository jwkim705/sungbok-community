package com.sungbok.community.dto;

import com.sungbok.community.common.vo.PageRequestVO;
import java.io.Serial;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostSearchVO extends PageRequestVO {

  @Serial
  private static final long serialVersionUID = 9103272123216937992L;

  private String category;

  private String userId;

}
