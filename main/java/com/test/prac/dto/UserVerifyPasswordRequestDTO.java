package com.test.prac.dto;

import com.test.prac.entity.UserAccountEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

//비밀번호 찾기 시 프론트엔드 -> 백엔드 데이터 전달용 DTO

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVerifyPasswordRequestDTO {

	private String id;
	private String email;

	public UserAccountEntity toEntity() {

		return UserAccountEntity.builder()
								.id(this.id)
								.email(this.email)
								.build();
	}
}
