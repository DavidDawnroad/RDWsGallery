package com.test.prac.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 사용자 관리 페이지에서 닉네임 변경 API를 호출할 때 프론트엔드 -> 백엔드 데이터 전달용 DTO
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManageChangeNicknameRequestDTO {
	
	@NotBlank(message = "닉네임을 필수로 입력해야 합니다.")
	@Pattern(regexp = "^\\S{2,12}$",
			 message = "닉네임을 공백 없이 2자 이상 12자 이하로 입력해야 합니다.")
	private String nickname;
}
