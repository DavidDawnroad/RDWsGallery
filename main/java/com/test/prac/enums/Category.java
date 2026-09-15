package com.test.prac.enums;

import java.util.Arrays;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Category {
	Manage(0),
	Notice(1),
	General(2);

	private final Integer category;

	public static Category boardCategory(Integer boardCategory) {

		return Arrays.stream(values())
				.filter(s -> s.getCategory().equals(boardCategory))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Unknown Board Category Number: " + boardCategory));
	}
}
// Query DSL 에서 사용 시 .where(BoardEntity.Category.eq(Category.General)).fetch();