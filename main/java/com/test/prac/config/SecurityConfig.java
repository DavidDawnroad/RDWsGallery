package com.test.prac.config;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.security.SpringSessionBackedSessionRegistry;

import lombok.RequiredArgsConstructor;

@Configuration // SpringBoot 설정 파일임을 명시
@EnableWebSecurity // Spring Security 의 전반적 보안 정책을 설정하는 클래스(SecurityConfig) 전용 Annotation
@RequiredArgsConstructor
public class SecurityConfig {

	private final PracLoginSuccessHandler pracLoginSuccessHandler;
	private final PracLoginFailureHandler pracLoginFailureHandler;
	// 커스텀 필터에 인증 기능을 부여하기 위해 AuthenticationConfiguration 주입
	private final AuthenticationConfiguration authenticationConfiguration;
	// 동일 계정 다중 로그인 제한 기능을 사용하기 위해 Spring Session Redis의 IndexedSessionRepository 주입
	private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;


	// AuthenticationManager = Spring Security 인증 총괄 컨트롤러 인터페이스
	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		
		return configuration.getAuthenticationManager();
	}
	
	// Session Registry 로 중복 로그인 제어
	@Bean
	public SpringSessionBackedSessionRegistry<? extends Session> sessionRegistry() {
		return new SpringSessionBackedSessionRegistry<>(sessionRepository);
	}

	// 커스텀 필터 사용 전 초기화 작업 (프론트엔드-백엔드 통신 데이터 포맷을 JSON으로 통일하기 위함)
	@Bean
	public JsonAuthenticationFilterCustom jsonAuthenticationFilterCustom() throws Exception {

		// 필터 생성 시 로그인 처리 URL 지정
		JsonAuthenticationFilterCustom filter = new JsonAuthenticationFilterCustom("/api/auth/login");

		// 필터 필수 요소인 AuthenticationManager와 성공 핸들러(PracLoginHandler) 세팅
		filter.setAuthenticationManager(authenticationManager(authenticationConfiguration));
		filter.setAuthenticationSuccessHandler(pracLoginSuccessHandler);
		// PracExceptionHandler가 잡을 수 없는 Security 필터체인 시점 예외(JsonAuthenticationFilterCustom 관련 오류들)를 잡아내는 실패 핸들러 세팅
		filter.setAuthenticationFailureHandler(pracLoginFailureHandler);
		// 인증 성공 시 로그인 정보를 Session 에 저장
		// (Spring Session Redis 의 SessionRepositoryFilter가 FilterChain 보다 상위에서 실행되므로 코드 변경 필요 없음)
		filter.setSecurityContextRepository(new HttpSessionSecurityContextRepository());

		return filter;
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		// CSRF 토큰 설정 임시 해제 코드. Security 설정 완료 이후 주석처리 할 것.
		// http.csrf(auth -> auth.disable());

		// URI 권한 허가 설정
		http.authorizeHttpRequests(auth -> auth
				.requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll() // css, js, image 등의 static 리소스 접근 제한 해제
				.requestMatchers("/login", "/signup", "/verify-user", "/update-password").permitAll() // 명시한 페이지의 액세스 권한 전부 오픈. 아닐 경우 상태코드 302 반환
				.requestMatchers("/board", "/board/{boardSeq}").permitAll()
				.requestMatchers("/api/auth/login", "/api/auth/signup", "/api/auth/verify-user", "/api/auth/update-password").permitAll()
				.requestMatchers("/api/board/{boardSeq}/increase-views").permitAll()
				.requestMatchers("/adminpage").hasRole("ADMIN") // ROLE_ADMIN 권한이 있을 경우 통과
				.anyRequest().authenticated() // 나머지 요소는 반드시 인증 요구
				)
				.csrf(csrf -> csrf.ignoringRequestMatchers("/api/auth/**", "/api/board/{boardSeq}/increase-views")) // CSRF Filter 적용 해제
				// 미허가 페이지 진입 시 403 상태코드 반환을 대신할 인증 진입점 설정
				.exceptionHandling(exception -> exception
						.defaultAuthenticationEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), new AntPathRequestMatcher("/api/**")) // API 요청일 경우 401 UNAUTHORIZED 반환
						.defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint("/login"), AnyRequestMatcher.INSTANCE) // 페이지 진입 요청일 경우 로그인 페이지로 반환
				);
		

/*		로그인 시 커스텀 필터 방식을 사용하므로 formLogin 관련 설정 주석 처리
		// 로그인 관련 설정
		http.formLogin(form -> form
				.loginPage("/login") // 로그인 페이지 URL
				.loginProcessingUrl("/api/auth/login") // 이 주소로 로그인 관련 POST 요청 시 Security 가 대신 처리
				.defaultSuccessUrl("/board")  // 로그인에 성공 시 이동할 페이지
				.successHandler(pracLoginHandler) // 로그인 성공 시 쿠키 처리 핸들러
				.permitAll()
				);
*/
		// 커스텀 로그인 필터를 일반 로그인 필터(UsernamePasswordAuthenticationFilter)에 끼워넣기
		http.addFilterBefore(jsonAuthenticationFilterCustom(), UsernamePasswordAuthenticationFilter.class);

		// 로그아웃 관련 설정
		http.logout(logout -> logout
				.logoutSuccessUrl("/board")
				.invalidateHttpSession(true) // 세션 속 데이터 즉시 삭제
//	            .deleteCookies("JSESSIONID") // 쿠키 즉시 삭제
				.deleteCookies("SESSION", "JSESSIONID") // Spring Session Redis를 세션 DB로 사용할 경우 세션 쿠키 이름을 JSESSIONID 에서 SESSION 으로 변경해야 함
				.permitAll()
				);
		
		// 동시 세션 제어 설정
		http.sessionManagement(session -> session
				.maximumSessions(2) // 사용자당 최대 세션 수
				// PC 2대 로그인은 막고 PC+모바일 로그인만 허용해야 한다면 Login Header 의 User-Agent나 프론트엔드의 기기타입 커스텀 파라미터를 확인하고 Principal username 에 디바이스 타입을 추가하는 방식의 로직을 작성해야 한다
				.maxSessionsPreventsLogin(false) // false: 기존 로그인을 강제 로그아웃 / true: 신규 로그인을 차단
				.sessionRegistry(sessionRegistry()) // Session Redis 의 SessionRegistry 연결
				);
		
		return http.build();
	}

	// BCrypt 비밀번호 암호화 객체
	@Bean
	public BCryptPasswordEncoder bCryptPasswordEncoder() {

		return new BCryptPasswordEncoder();
	}

}
