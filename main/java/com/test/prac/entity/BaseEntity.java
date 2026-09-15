package com.test.prac.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@Getter
@MappedSuperclass // 이 클래스를 상속받는 Entity 에게 필드와 @도 상속해줌
@EntityListeners(AuditingEntityListener.class) // Auditing(메타데이터 자동기록) 기능 활성화
public class BaseEntity {

	@CreatedDate // 최초 생성 시각 자동기록
	@Column(name = "regdate", updatable = false)
	private LocalDateTime regdate;

	@LastModifiedDate // 수정 시각 자동기록
	@Column(name = "moddate")
	private LocalDateTime moddate;
}
