package com.test.prac.dto;

import com.test.prac.entity.UserAccountEntity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 회원가입 시 프론트엔드 -> 백엔드 데이터 전달용 DTO
@Getter
@Builder
@NoArgsConstructor // 기본 생성자 주입
@AllArgsConstructor // @Builder가 전체 생성자를 요구할 경우를 위한 대비
public class UserRegisterRequestDTO {

	@NotBlank(message = "ID를 필수로 입력해야 합니다.")
	@Pattern(regexp = "^(?=.*[A-Za-z])\\S{4,20}$",
			 message = "ID를 공백 없이 4자 이상 20자 이하로 입력해야 합니다.")
	private String id;

	@NotBlank(message = "비밀번호를 필수로 입력해야 합니다.")
	@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)\\S{8,}$",
			 message = "비밀번호를 공백 없이 영문+숫자 8자 이상으로 입력해야 합니다.")
	private String password;

	@NotBlank(message = "이메일을 필수로 입력해야 합니다.")
	@Email(message = "올바른 이메일 형식이 아닙니다.")
	@NotNull // @Email은 null == true 를 반환하기 때문에 내 프로젝트의 조건과 맞지 않음
	private String email;

	@NotBlank(message = "닉네임을 필수로 입력해야 합니다.")
	@Pattern(regexp = "^\\S{2,12}$",
			 message = "닉네임을 공백 없이 2자 이상 12자 이하로 입력해야 합니다.")
	private String nickname;

	// DTO -> Entity 변환 시점에 암호화된 비밀번호를 Service 에서 파라미터로 주입 받도록 조정
	public UserAccountEntity toEntity(String encodedPassword) {

		return UserAccountEntity.builder()
								.id(this.id)
								.password(encodedPassword)
								.email(this.email)
								.nickname(this.nickname)
								.build();
	}

}