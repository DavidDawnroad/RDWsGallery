package com.test.prac.dto;

import com.test.prac.enums.Category;

import jakarta.persistence.Lob;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 작성한 게시글 데이터 정보가 프론트엔드 -> 백엔드로 이동할 때 사용하는 DTO
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardCreateRequestDTO {

	private Category category;
	
	@NotBlank(message = "제목을 필수로 입력해야 합니다.")
	private String title;
	
	@Lob
	@NotBlank(message = "내용을 필수로 입력해야 합니다.")
	@Size(min = 2, message = "내용은 최소 2글자 이상이어야 합니다.")
	private String content;

}
