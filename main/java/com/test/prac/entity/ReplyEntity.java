package com.test.prac.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "REPLY")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReplyEntity extends BaseEntity {

	@Id
	@Column(name = "REPLY_SEQ")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long replySeq;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "BOARD_SEQ", nullable = false)
	private BoardEntity boardEntity;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "USER_SEQ", nullable = false)
	private UserAccountEntity userEntity;
	
	@Column(name = "REPLY_CONTENT")
	private String content;
	
	@Builder
	public ReplyEntity(BoardEntity boardEntity, UserAccountEntity userEntity, String content) {
		this.boardEntity = boardEntity;
		this.userEntity = userEntity;
		this.content = content;
	}
	
	public void updateReply(String content) {
		this.content = content;
	}
}
