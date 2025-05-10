package com.sungbok.community.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor // 기본 생성자 추가
@AllArgsConstructor // 모든 필드를 받는 생성자 추가
@Builder
public class AddUserRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 148925951199919627L;

    @NotBlank(message = "이메일은 필수 입력 값입니다.")
    @Pattern(regexp = "^(?:\\w+\\.?)*\\w+@(?:\\w+\\.)+\\w+$", message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @NotBlank
    @Pattern(regexp = "(?=.*[0-9])(?=.*[a-zA-Z])(?=.*\\W)(?=\\S+$).{8,16}", message = "비밀번호는 8~16자 영문 대 소문자, 숫자, 특수문자를 사용하세요.")
    private String password;

    @NotBlank
    @Size(min = 2, max = 10, message = "이름은 2자 이상 50자 이하로 입력해주세요.")
    private String name;

    @Pattern(regexp = "^[ㄱ-ㅎ가-힣a-z0-9-_]{2,10}$", message = "닉네임은 특수문자를 제외한 2~10자리여야 합니다.")
    private String nickname;

    @Past(message = "생년월일은 현재 날짜보다 이전이어야 합니다.")
    private LocalDate birthday;

    @NotBlank
    @Size(max = 10, message = "성별 값은 10자 이하로 입력해주세요.")
    private String gender;

    @Size(max = 255, message = "주소는 255자 이하로 입력해주세요.")
    private String address;

    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "휴대폰 번호 형식이 올바르지 않습니다. (예: 010-1234-5678)")
    private String phoneNumber;

    private String deptNm;

    private String role;

    @Positive(message = "등록자 ID는 양수여야 합니다.")
    private Long registeredByUserId;

}
