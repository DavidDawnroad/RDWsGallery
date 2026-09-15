package com.test.prac.config;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

// Spring Security 로그인 인증 성공 시 발동되는 handler. 이 곳에서 아이디 저장 체크박스용 쿠키도 생성
@Component // Spring Bean 으로 관리하고 싶은 클래스(이면서 특정 MVC 레이어에 포함되지 않는)에 작성
@RequiredArgsConstructor
public class PracLoginSuccessHandler implements AuthenticationSuccessHandler {

	private final ObjectMapper objectMapper;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
			throws IOException, ServletException {

		// 프론트엔드에서 보낸 rememberId 체크값 가져오기
		Boolean rememberId = (Boolean) request.getAttribute("rememberId");
		String userId = authentication.getName();

		// api/login 에 있던 '아이디 저장하기' 관련 쿠키 코드
		if (Boolean.TRUE.equals(rememberId)) {
			Cookie cookie = new Cookie("savedId", userId); // savedId 라는 이름으로 쿠키 생성
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

		// Axios 비동기 응답에 맞춰 성공 결과를 JSON으로 반환하기 위한 작업
		response.setContentType("application/json;charset=UTF-8");
		response.setStatus(HttpStatus.OK.value());

		Map<String, String> result = new HashMap<>();
		result.put("status", "SUCCESS");
		result.put("id", userId);

		PrintWriter writer = response.getWriter();
		writer.print(objectMapper.writeValueAsString(result));
		writer.flush();
	}
}
