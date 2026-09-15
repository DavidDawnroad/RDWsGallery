package com.test.prac.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PracLoginFailureHandler implements AuthenticationFailureHandler {

	private final ObjectMapper objectMapper;

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException, ServletException {

		response.setContentType("application/json;charset=UTF-8");
		response.setStatus(HttpStatus.UNAUTHORIZED.value());

		String message;

		if (exception instanceof DisabledException) {
			message = "탈퇴 처리된 계정입니다.";
		} else if (exception instanceof LockedException) {
			message = "현재 잠금 처리된 계정입니다.";
		} else {
			message = "없는 ID이거나 비밀번호가 틀렸습니다.";
		}

		Map<String, String> errorData = new HashMap<>();

		errorData.put("status", "FAIL");
		errorData.put("message", message);
		objectMapper.writeValue(response.getWriter(), errorData);
	}
}
