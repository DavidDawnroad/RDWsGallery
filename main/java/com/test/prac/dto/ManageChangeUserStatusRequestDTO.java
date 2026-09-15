package com.test.prac.dto;

import com.test.prac.enums.UStat;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManageChangeUserStatusRequestDTO {
	
	@NotNull(message = "유저 상태값은 무조건 존재해야 합니다.")
	UStat userStatus;
}
