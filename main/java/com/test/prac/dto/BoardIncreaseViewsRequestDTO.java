package com.test.prac.dto;

public record BoardIncreaseViewsRequestDTO(
		String viewsToken
		) {
			public static BoardIncreaseViewsRequestDTO from(String viewsToken) {
				
				return new BoardIncreaseViewsRequestDTO(viewsToken);
			}
}
