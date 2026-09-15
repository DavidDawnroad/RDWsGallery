package com.test.prac.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.test.prac.entity.ReplyEntity;

// 댓글 목록 출력 시 백엔드 -> 프론트엔드 데이터 전달용 DTO
public record ReplyShowListResponseDTO(
		Long		  replySeq,
		String		  userId,
		String		  nickname,
		String 		  content,
		LocalDateTime regdate
		) {
			public static ReplyShowListResponseDTO of(ReplyEntity entity) {
				
				return new ReplyShowListResponseDTO(entity.getReplySeq(),
												    entity.getUserEntity().getId(),
													entity.getUserEntity().getNickname(),
													entity.getContent(),
													entity.getRegdate());
			}
			
			// 프론드엔드에서 등록일 데이터를 표기하기 위한 method
			public String formattedRegdate() {
				
				return this.regdate.format(DateTimeFormatter.ofPattern("MM.dd HH:mm:ss"));
			}
	
}
