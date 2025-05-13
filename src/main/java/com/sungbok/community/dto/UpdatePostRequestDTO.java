package com.sungbok.community.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Accessors(chain = true)
@NoArgsConstructor // 기본 생성자 추가
@AllArgsConstructor // 모든 필드를 받는 생성자 추가
@Builder
public class UpdatePostRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -6796414860189283375L;

    private String title;

    private String content;

    private String category;

}
