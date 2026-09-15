package com.test.prac.dto;

import java.io.Serializable;

import com.test.prac.entity.UserAccountEntity;
import com.test.prac.enums.AccLv;
import com.test.prac.enums.UStat;

public record SessionDTO(
	Long seq,
	String id,
	String password,
	String email,
	String nickname,
	AccLv accessLevel,
	UStat userStatus
) implements Serializable {

	private static final long serialVersionUID = 1L;
	
	public static SessionDTO from(UserAccountEntity userEntity) {
		
		return new SessionDTO(
				userEntity.getSeq(),
				userEntity.getId(),
				userEntity.getPassword(),
				userEntity.getEmail(),
				userEntity.getNickname(),
				userEntity.getAccessLevel(),
				userEntity.getUserStatus());
	}
}
