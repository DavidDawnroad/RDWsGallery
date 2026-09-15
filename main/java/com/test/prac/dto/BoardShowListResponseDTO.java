package com.test.prac.dto;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.test.prac.entity.BoardEntity;
import com.test.prac.enums.Category;

/* DTO를 불변(final) 데이터 클래스(record)로 생성 (Java 16+)
 * 필드를 인자에 집어넣는 것만으로 내부 선언 없이 필드가 private final 로 선언된다. -> 상태 변경 불가
 * 생성자 / Getter / equals() / toString() / hashCode() 자동 제공 -> Lombok 의존성 제거
 * 필드를 인자에 넣는 특성 상 필드의 개수가 많으면 오히려 가독성이 떨어질 수도 있다.
 * record 는 정적 변수와 정적 메서드를 포함할 수 있다 -> static 메서드를 활용하여 Static Factory Method 정의 가능
 *
 */
// 게시판 페이지에 데이터를 출력할 때 백엔드 -> 프론트엔드 데이터 전달용 DTO
public record BoardShowListResponseDTO(
		Long 		  boardSeq,
		Category 	  category,
		String 		  title,
		String 		  nickname,
		LocalDateTime regdate,
		Long 		  views,
		Long 		  likes, // 추천수에서 비추천수를 뺀 값을 반환
		Long		  replyCount
		) {
	
			private static final ZoneId KST = ZoneId.of("Asia/Seoul");
			
	// 여러개의 매개변수를 받아서 내부 객체를 반환하는 메서드를 작성할 때는 of()를, 매개변수 하나를 동일 도메인의 다른 타입으로 변환할 때는 from()을 사용하는 것이 관례
	// 지금은 파라미터가 2개이기 때문에 of()을 사용하는 것이 적합
			public static BoardShowListResponseDTO of(BoardEntity entity, long viewsDelta, long likesDelta, long dislikesDelta) {

				long entityViews = entity.getViews() != null ? entity.getViews() : 0L;
				long entityLikes = entity.getLikes() != null ? entity.getLikes() : 0L;
				long entityDislikes = entity.getDislikes() != null ? entity.getDislikes() : 0L;
				
				return new BoardShowListResponseDTO(
						entity.getBoardSeq(),
						entity.getCategory(),
						entity.getTitle(),
						entity.getUserEntity().getNickname(),
						entity.getRegdate(),
						entityViews + viewsDelta,
						(entityLikes + likesDelta) - (entityDislikes + dislikesDelta), // 추천수에서 비추천수를 뺀 값을 반환
						entity.getReplyCount() != null ? entity.getReplyCount() : 0L // 댓글이 null 개일 경우(replyCount 추가 이전 게시글들) 0으로 초기화
				);
			}
			
			// 프론드엔드에서 등록일 데이터를 여러가지로 표기하기 위한 method
			public String formattedRegdate() {
				
				if (this.regdate.toLocalDate().equals(ZonedDateTime.now(KST).toLocalDate())) {
					// 등록일 데이터 일자가 오늘이면 시:분 반환
					return this.regdate.format(DateTimeFormatter.ofPattern("HH:mm"));
				}
				// 오늘이 아니면 연.월.일 반환
				return this.regdate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
			}
}