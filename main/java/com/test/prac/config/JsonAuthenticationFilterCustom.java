package com.test.prac.config;

import java.io.IOException;

import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.prac.dto.UserCheckLoginRequestDTO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// 기존 formLogin의 방식은 JSON 형태의 데이터를 읽지 못 하기 때문에 커스텀 필터 방식 사용
// 프론트엔드-백엔드에서 JSON 데이터 이동 구조를 채택할 때 많이 사용하는 방법 (Thymeleaf + Axios 코드 변경 X)

public class JsonAuthenticationFilterCustom extends AbstractAuthenticationProcessingFilter {

	// JSON 문자열 <-> DTO 변경하는 Jackson 라이브러리의 핵심 객체 ObjectMapper 선언
	private final ObjectMapper objectMapper = new ObjectMapper();

	public JsonAuthenticationFilterCustom(String defaultFilterProcessesUrl) {
		// 프론트엔드에서 /api/auth/login 으로 POST 요청을 보내면 SecurityConfig을 거쳐서 이 필터가 처리하도록 설정
        super(new AntPathRequestMatcher(defaultFilterProcessesUrl, "POST"));
    }

	@Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException {

		String contentType = request.getContentType();
		// POST 요청이 아니거나, contentType이 아예 비었거나, 있는데 JSON 타입이 아니면 예외 처리
        if (!"POST".equalsIgnoreCase(request.getMethod()) || contentType == null || !contentType.contains("application/json")) {
            throw new AuthenticationServiceException("Authentication method not supported: " + request.getMethod());
        }

        // body 에 담긴 JSON 텍스트 스트림 데이터를 getInputStream()으로 읽고 readValue()가 UserCheckLoginRequestDTO로 Parse 하여 Mapping
        UserCheckLoginRequestDTO loginDTO = objectMapper.readValue(request.getInputStream(), UserCheckLoginRequestDTO.class);

        // 로그인 성공 핸들러(PracLoginSuccessHandler)에서 아이디 저장하기 쿠키 생성을 위해 rememberId 데이터를 꺼내 쓸 수 있도록 HttpServletRequest에 임시 보관
        // getInputStream()으로 body 데이터를 읽는 것은 한 번만 가능하기 때문에 핸들러에서도 body 데이터의 일부를 쓸 수 있게 여기서 저장해놓는 것
        request.setAttribute("rememberId", loginDTO.isRememberId());

        // 스프링 시큐리티 인증용 토큰(이지만 유저 정보만 들어가 있는 미완성 버전) 생성
        /* SecurityConfig 파일에서 AuthenticationManager 가 Providers 로 DB 데이터와 이 데이터를 비교하는 작업을 거쳐 최종 인증에 성공하면
         * 권한 정보를 채우고 완성된 토큰을 반환할 예정이며, Security 가 PracLoginSuccessHandler를 실행함
         * 이 토큰은 Security 시스템 안에서만 사용되는 토큰이기 때문에 JWT를 도입해도 계속 사용 가능(사용 위치가 다름)
        */
        UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(loginDTO.getId(), loginDTO.getPassword());

        // 4. AuthenticationManager에게 인증 위임하여 검증 진행
        return this.getAuthenticationManager().authenticate(authRequest);
    }
}
