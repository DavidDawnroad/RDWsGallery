package com.test.prac.enums;

import java.util.Arrays;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// CHAR(1) <- AttributeConverter -> Enum 매핑을 위한 Enum 정의 파일

@Getter // getLevel() 자동 생성
@RequiredArgsConstructor // final 키워드가 붙은 필드의 생성자 자동 생성
public enum AccLv {
	USER("U"),
	ADMIN("A");

	private final String level;

	// DB에서 읽어온 access_level 값을 Enum 객체로 변환(매핑)하는 method
	public static AccLv accessLevel(String acclv) {

		return Arrays.stream(values()) // values()로 Enum에 선언된 모든 값을 배열로 반환 -> 하나씩 검사할 수 있게 stream()에 투입
				.filter(s -> s.level.equals(acclv)) // 받은 acclv 값이 level에 있는 값과 같은지?
				.findFirst() // filtering 된 첫번째 객체 반환
				.orElseThrow(() -> new IllegalArgumentException("Unknown User Access Level: " + acclv)); // 예상 값과 다를 경우 오류 발생
	}

}
// Query DSL에서 사용 시 .where(UserAccountEntity.AccLv.eq(Status.USER)).fetch();