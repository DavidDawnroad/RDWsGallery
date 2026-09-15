package com.test.prac.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 비밀번호 변경 시 프론트엔드 -> 백엔드 데이터 전달용 DTO
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdatePasswordRequestDTO {

	@NotBlank(message = "비밀번호를 필수로 입력해야 합니다.")
	@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
			 message = "영문+숫자 8자 이상으로 입력해야 합니다.")
	private String password;
	
	private String resetPasswordToken;

	// Dirty Checking 방식으로 데이터를 UPDATE(기존 데이터 조회 후 필드만 수정)할 예정이므로, toEntity() 메서드는 삭제.
}