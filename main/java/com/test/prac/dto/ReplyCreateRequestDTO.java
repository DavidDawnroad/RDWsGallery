package com.test.prac.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 댓글 작성 시 프론트엔드 -> 백엔드 데이터 전달용 DTO
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplyCreateRequestDTO {

	@NotBlank(message = "내용을 필수로 입력해야 합니다.")
	private String content;
}
