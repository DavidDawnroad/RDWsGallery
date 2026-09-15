package com.test.prac.dto;

import com.test.prac.entity.UserAccountEntity;
import com.test.prac.enums.UStat;

public record ManageShowUserListResponseDTO(
		String userAccountId,
		String nickname,
		UStat userStatus
		) {
			public static ManageShowUserListResponseDTO of(UserAccountEntity userEntity) {
				
				return new ManageShowUserListResponseDTO(
						userEntity.getId(),
						userEntity.getNickname(),
						userEntity.getUserStatus()
						);
			}
}
