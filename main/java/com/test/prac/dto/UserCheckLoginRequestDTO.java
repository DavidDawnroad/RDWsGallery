package com.test.prac.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 로그인 시 프론트엔드 -> 백엔드 데이터 전달용 DTO
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCheckLoginRequestDTO {

	private String id;
	private String password;
	private boolean rememberId;
}