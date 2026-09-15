package com.test.prac.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.test.prac.dto.UserUpdatePasswordRequestDTO;
import com.test.prac.dto.UserRegisterRequestDTO;
import com.test.prac.dto.UserVerifyPasswordRequestDTO;
import com.test.prac.entity.UserAccountEntity;
import com.test.prac.service.UserAccountService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth") // /prac/api/auth
@RequiredArgsConstructor
public class UserAccountAPIController {

	private final UserAccountService userAccountService;

	// signup.js가 보낸 JSON 데이터를 @RequestBody DTO 객체로 받는다
	// @Valid 어노테이션으로 데이터 검증 활성화
	@PostMapping("/signup")
	public ResponseEntity<Map<String, String>> createAccount(@Valid @RequestBody UserRegisterRequestDTO registerDTO) {

		UserAccountEntity entity = userAccountService.createAccount(registerDTO);
		// 프론트엔드로 로그인 검증 결과를 전송할 해시맵 객체 선언
		Map<String, String> signupData = new HashMap<>();

		signupData.put("status", "SUCCESS");
		signupData.put("id", entity.getId());
		signupData.put("nickname", entity.getNickname());

		// 상태코드 201: Created 전송
		return ResponseEntity.status(HttpStatus.CREATED).body(signupData);
	}
/*	Spring Security 기술 적용 이후 해당 메서드의 역할은 SecurityConfig의 로그인 필터가 담당하므로 주석 처리
	// 쿠키같은 브라우저 시스템 설정을 헤더로 제어할 때는 Model 대신 HttpServletResponse를 사용하는 것이 좋다.
	@PostMapping("/login")
	public ResponseEntity<Map<String, String>> checkLogin(@RequestBody UserCheckLoginRequestDTO checkDTO, HttpServletResponse response) {

		UserAccountEntity entity = userAccountService.checkLogin(checkDTO);
		boolean rememberId = checkDTO.isRememberId();

		if (rememberId) { // 아이디 저장하기를 체크했으면
			Cookie cookie = new Cookie("savedId", checkDTO.getId()); // savedId 라는 이름으로 쿠키 생성
			cookie.setPath("/prac"); // context-path 싱크
			cookie.setMaxAge(60 * 60 * 24 * 30); // 쿠키 수명: 30일(초 단위)
			cookie.setHttpOnly(true); // XSS 방어
//			cookie.setSecure(true); // 네트워크 스니핑 방어 (HTTPS에서만 통신)
			response.addCookie(cookie);

		} else {
			Cookie cookie = new Cookie("savedId", null);
			cookie.setPath("/prac");
			cookie.setMaxAge(0); // kill
			response.addCookie(cookie);
		}

		Map<String, String> checkedData = new HashMap<>();

		checkedData.put("status", "SUCCESS");
		checkedData.put("id", entity.getId());

		// 상태코드 200: OK 전송하는 내장함수
		return ResponseEntity.ok(checkedData);
	}
*/

	@PostMapping("/verify-user")
	public ResponseEntity<Map<String, String>> verifyUser(@RequestBody UserVerifyPasswordRequestDTO verifyDTO) {

		String resetPasswordToken = userAccountService.verifyUser(verifyDTO);

		Map<String, String> verifiedData = new HashMap<>();
		verifiedData.put("status", "SUCCESS");
		verifiedData.put("token", resetPasswordToken);

		return ResponseEntity.ok(verifiedData);
	}

	@PostMapping("/update-password")
	public ResponseEntity<Map<String, String>> updatePassword(@Valid @RequestBody UserUpdatePasswordRequestDTO updateDTO) {

		userAccountService.updatePassword(updateDTO);

		// 정적 팩토리 메서드 of()로 Map 객체 선언하고 put() 하는 과정 생략
		// 단, of()로 생성된 객체는 중간에 값을 수정할 수 없고 null value 를 허용하지 않는다.
		return ResponseEntity.ok(Map.of("status", "SUCCESS"));
	}
}
