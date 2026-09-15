package com.test.prac.config;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.test.prac.dto.SessionDTO;
import com.test.prac.enums.UStat;

import lombok.Getter;

// 인증된 사용자 정보를 가지고 있는 객체 (principal)
// UserDetailsCustom은 보안 관련 비즈니스 로직을 가지고 DTO의 역할을 하고 있는 클래스이다.
@Getter
@JsonIgnoreProperties(ignoreUnknown = true) // 직렬화할 때 @JsonIgnore로 제외시킨 @Override method 들을 무시하도록 설정하는 Annotation
public class UserDetailsCustom implements UserDetails, Serializable {

	private final SessionDTO sessionDTO;
	
	// 사용자 세션 제어를 위한 사전 작업-직렬화 버전ID 명시
	// 클래스 파일에 변경 사항이 생기거나, 서버 재배포 시 기존 유저의 세션을 유지하기 위한 작업이다.
	// 버전ID를 따로 명시하지 않으면 JVM이 클래스 파일의 변경 사항 발생 시마다 임의의 버전ID 값을 자동 생성하여 부여한다.
	// -> 파일이 수정될 때마다 InvalidClassException이 발생하여 기존 세션 정보가 파손된다.
	// 사용자 세션 제어를 위한 작업이기 때문에, 사용자 정보가 들어갈 UserAccountEntity에도 직렬화 버전ID 명시 작업을 같이 수행해야 한다.
	private static final long serialVersionUID = 1L;
	
	// Jackson 역직렬화를 하기 위한 생성자 + 필드 매핑
	// 역직렬화에는 @NoArgsConstructor 로 기본 생성자를 만들거나, @JsonCreator/@JsonProperty가 명시된 생성자를 만드는 선행작업이 필요하다.
	// SessionDTO는 private final 로 선언되어 기본 생성자를 만들 수 없으므로, 후자의 방법을 선택한다.
	@JsonCreator
	public UserDetailsCustom(@JsonProperty("sessionDTO") SessionDTO sessionDTO) {
		this.sessionDTO = sessionDTO;
	}

	// 권한 설정
	@JsonIgnore
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {

		Collection<GrantedAuthority> authorities = new ArrayList<>();
		String role = sessionDTO.accessLevel().name(); // getAccessLevel()만 쓰면 CHAR(1) 결과가 나오니까 name()을 붙여서 Enum 에 정의한 단어 전체가 나오게 함

		authorities.add(new SimpleGrantedAuthority("ROLE_" + role));

		return authorities;
	}
	
	@JsonIgnore
	@Override
	public String getUsername() { // 닉네임 아니고 ID
		return sessionDTO.id();
	}

	@JsonIgnore
	@Override
	public String getPassword() {
		return sessionDTO.password();
	}

	@JsonIgnore
	// 탈퇴계정일 경우 (가장 범용성이 높은 체크 유형)
	@Override
	public boolean isEnabled() {
		return sessionDTO.userStatus() != UStat.TERMINATED;
	}

	@JsonIgnore
	// 계정이 만료(휴면)된 상태일 경우
	@Override
	public boolean isAccountNonExpired() {
		return sessionDTO.userStatus() != UStat.DORMANT;
	}

	@JsonIgnore
	// 계정이 잠긴 상태일 경우
	@Override
	public boolean isAccountNonLocked() {
		UStat userStatus = sessionDTO.userStatus();
		return userStatus != UStat.LOCKED && userStatus != UStat.BANNED;
	}

	@JsonIgnore
	// 로그인에 성공했지만 비밀번호가 만료되었을 경우 (ex-정기적 비밀번호 변경 로직)
	@Override
	public boolean isCredentialsNonExpired() {
		return true; // 미완성
	}
	
	@JsonIgnore
	// 커스텀 계정 상태 검사 메서드
	public boolean isNormalStatus() {
		return sessionDTO.userStatus() == UStat.NORMAL;
	}
}
