package com.test.prac.entity;

import java.io.Serializable;

import org.hibernate.annotations.DynamicUpdate;

import com.test.prac.enums.AccLv;
import com.test.prac.enums.AccLvConverter;
import com.test.prac.enums.UStat;
import com.test.prac.enums.UStatConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter // Entity 클래스에서 Setter 는 생성자 + 비즈니스 메서드로 구현한다
@Table(name = "USER_ACCOUNT") // Entity-Table의 이름이 다를 경우 어떤 이름의 테이블과 매핑되는지 명시
@DynamicUpdate // UPDATE 수행 시 변경된 필드만 Query 에 반영되도록 설정 (동일값 덮어쓰기 작업 방지)
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA에서 DB 데이터를 자바 객체로 변환할 때 내부적으로 호출하는 기본 생성자 주입
public class UserAccountEntity extends BaseEntity implements Serializable { // BaseEntity.java의 regdate & moddate 자동 추가 / Session Redis 에 필요한 직렬화 작업을 위한 Serializable 인터페이스 상속
																			// 반드시 extends(단일 상속)-implements(다중 구현)의 상속 순서를 지킬 것
	
	private static final long serialVersionUID = 1L;

	@Id // Primary Key 명시
	@GeneratedValue(strategy = GenerationType.IDENTITY) // GenerationType.IDENTITY는 .SEQUENCE와 달리 generator 옵션과 @SequenceGenerator를 지정할 필요가 없다.
	private Long seq; // 안정성을 위해 값형 대신 참조(Object)형 변수로 선언

	private String id;
	private String password;
	private String email;
	private String nickname;

	@Convert(converter = AccLvConverter.class)
	@Column(name = "ACCESS_LEVEL", columnDefinition = "CHAR(1)")
	private AccLv accessLevel = AccLv.USER;

	@Convert(converter = UStatConverter.class)
	@Column(name = "USER_STATUS", columnDefinition = "CHAR(1)")
	private UStat userStatus = UStat.NORMAL;

	// @AllArgsConstructor 대신 회원가입 시점에 필요한 사용자 입력 필드만 모아서 커스텀 생성자를 @Builder로 구현
	@Builder
	public UserAccountEntity(String id, String password, String email, String nickname) {
		this.id = id;
		this.password = password;
		this.email = email;
		this.nickname = nickname;
	}

	// Setter 를 method 로 구현
	public void changePassword(String encodedPassword) {
		this.password = encodedPassword;
	}

	public void changeNickname(String nickname) {
		this.nickname = nickname;
	}

	public void changeStatus(UStat newStatus) {

		if (newStatus == null) {
			throw new IllegalArgumentException("회원 상태는 필수값입니다.");
		}

		this.userStatus = newStatus;
	}
}