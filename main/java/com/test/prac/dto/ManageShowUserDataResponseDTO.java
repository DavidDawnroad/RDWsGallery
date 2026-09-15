package com.test.prac.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.test.prac.entity.BoardEntity;
import com.test.prac.entity.ReplyEntity;
import com.test.prac.entity.UserAccountEntity;
import com.test.prac.enums.AccLv;
import com.test.prac.enums.UStat;

// 사용자 관리 페이지에서 사용자 데이터를 출력할 때 백엔드 -> 프론트엔드 데이터 전달용 DTO
public record ManageShowUserDataResponseDTO(
		UserData userData,
		List<BoardData> boardDatas,
		List<ReplyData> replyDatas
		) {
			public record UserData(String userAccountId, String nickname, AccLv accessLevel, UStat userStatus) {
				public static UserData from(UserAccountEntity entity) {
					
					return new UserData(
							entity.getId(),
							entity.getNickname(),
							entity.getAccessLevel(),
							entity.getUserStatus()
					);
				}
			}
			
			public record BoardData(Long boardSeq, String title, LocalDateTime regdate) {
				public static BoardData from(BoardEntity entity) {
					
					return new BoardData(
							entity.getBoardSeq(),
							entity.getTitle(),
							entity.getRegdate()
					);
				}
			}
			
			public record ReplyData(Long replySeq, Long boardSeq, String title, String content, LocalDateTime regdate) {
				public static ReplyData from(ReplyEntity entity) {
					
					return new ReplyData(
							entity.getReplySeq(),
							entity.getBoardEntity().getBoardSeq(),
							entity.getBoardEntity().getTitle(),
							entity.getContent(),
							entity.getRegdate()
							);
				}
			}
}
