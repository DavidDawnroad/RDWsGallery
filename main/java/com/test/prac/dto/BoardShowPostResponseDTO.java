package com.test.prac.dto;

import java.time.LocalDateTime;

import com.test.prac.entity.BoardEntity;
import com.test.prac.enums.Category;

// 게시글 페이지에 데이터를 출력할 때 백엔드 -> 프론트엔드 데이터 전달용 DTO
public record BoardShowPostResponseDTO(
		Long boardSeq,
	    Category category,
	    String title,
	    String content,
	    String nickname,
	    String userId, // 작성자 검증용
	    LocalDateTime regdate,
	    Long views, // 실시간 조회수
	    Long likes, // 실시간 추천수
	    Long dislikes, // 실시간 비추천수
	    String viewsToken // 조회수 증가를 허가하는 임시 토큰
		) {
			public static BoardShowPostResponseDTO of(BoardEntity boardEntity, long viewsDelta, long likesDelta, long dislikesDelta, String viewsToken) {
				
				long entityViews = boardEntity.getViews() != null ? boardEntity.getViews() : 0L;
				long entityLikes = boardEntity.getLikes() != null ? boardEntity.getLikes() : 0L;
				long entityDislikes = boardEntity.getDislikes() != null ? boardEntity.getDislikes() : 0L;
				
				return new BoardShowPostResponseDTO(
		                boardEntity.getBoardSeq(),
		                boardEntity.getCategory(),
		                boardEntity.getTitle(),
		                boardEntity.getContent(),
		                boardEntity.getUserEntity().getNickname(),
		                boardEntity.getUserEntity().getId(),
		                boardEntity.getRegdate(),
		                entityViews + viewsDelta,
		                entityLikes + likesDelta,
		                entityDislikes + dislikesDelta,
		                viewsToken
		                );
			}
}
