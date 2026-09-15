package com.test.prac.dto;

import com.test.prac.enums.Category;

import jakarta.persistence.Lob;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 게시글을 수정할 때 프론트엔드 -> 백엔드 데이터 전달용 DTO
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardUpdateRequestDTO {

	@NotNull
	private Category category;
	
	@NotBlank(message = "제목을 필수로 입력해야 합니다.")
	private String title;
	
	@Lob
	@NotBlank(message = "내용을 필수로 입력해야 합니다.")
	@Size(min = 2, message = "내용은 최소 2글자 이상이어야 합니다.")
	private String content;
}
