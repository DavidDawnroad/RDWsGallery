package com.test.prac.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.prac.dto.SessionDTO;

@Configuration(proxyBeanMethods = false) // Bean 이 하나이거나, 다수지만 Bean 간 직접 호출 작업이 없고 파라미터 주입을 통한 간접 호출 방식을 사용하고 있어서 Proxy 객체로 만드는 작업을 통해 Singleton(특정 클래스의 객체를 하나만 생성)을 보장할 필요가 없을 경우, ProxyBean 생성을 생략해서 구동 속도 개선
// 사용자 Principal 을 기준으로 저장된 모든 Session 을 인덱싱 기반으로 역추적 가능
@EnableRedisIndexedHttpSession(maxInactiveIntervalInSeconds = 7200, // 2시간
							   redisNamespace = "spring:session", // Key Prefix
							   cleanupCron = "0 */2 * * * *") // 2분 마다 (default == 1분)
public class RedisSessionConfig {

	/* - WAS 메모리(Spring Boot 는 Embedded 된 Tomcat 을 사용함) 기반 HttpSession 방식 -> Redis 인메모리 데이터 기반 Spring Session Redis 방식으로 전환하여 얻는 이점
	 * (WAS는 비즈니스 로직, DB 연동과 처리 등을 처리하는 서버)
	 * 1. WAS가 사용자 세션 데이터를 들고 있지 않는 Stateless 상태가 됨 -> 서버 트래픽 증가로 인한 WAS 서버 증설 시에도 새 WAS 서버에서 사용자 세션 데이터를 쉽게 관리 가능
	 * 2. WAS 서버가 다운되거나 재배포로 인해 재시작되어도 사용자 세션이 소멸되어 강제 로그아웃되지 않고 그대로 유지 가능
	 * 3. 1,2로 인해 무중단 배포가 가능해져서 UX가 좋아짐
	 * 4. @EnableRedisIndexedHttpSession으로 특정 사용자의 세션을 역추적해서 중복로그인 제한, 실시간 밴 처리, 현재 접속자 수 조회 등의 제어 가능
	 * 5. WAS JVM 메모리 점유의 큰 부분을 차지하는 세션 데이터를 외부 DB로 격리하여 성능 개선
	*/
	
	/* Java 기본 직렬화 도구인 JdkSerializationRedisSerializer 대신 별도의 직렬화 도구(GenericJackson2JsonRedisSerializer)를 사용하는 이유에 대한 설명은
	 * RedisConfig.java에서 StringRedisSerializer 직렬화 도구를 선택하면서 주석으로 작성해놓았음.
	 * 
	 * HTTPSession에 DTO를 직접 저장할(개발자가 세션에 특정 데이터를 저장해서 필요한 부분에 쓰는) 일이 생길 경우,
	 * JSON 데이터를 Java 객체로 역직렬화 해야 하므로 DTO에 기본 생성자를 반드시 생성시킬 것. (@NoArgsConstructor)
	*/
	
	// Spring Session Framework 가 이 Bean 이름을 감지해서 Session Serializing 작업에 사용
	@Bean("springSessionDefaultRedisSerializer")
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return new GenericJackson2JsonRedisSerializer(objectMapper());
    }

	private ObjectMapper objectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		
		ClassLoader classLoader = getClass().getClassLoader();
		// Spring Security 의 GrantedAuthority(유저 권한 설정), SecurityContext(로그인 정보) 가 JSON -> Java 로 역직렬화될 때 SecurityJackson2Modules로 정상적으로 복원
		mapper.registerModules(SecurityJackson2Modules.getModules(classLoader));
		
		// 역직렬화 공격 방지를 위해 역직렬화 시 허용된 클래스만 Allowlist 에 포함시켜 Jackson 역직렬화를 수행할 수 있도록 설정 (Spring Security 6+)
		mapper.addMixIn(UserDetailsCustom.class, SecurityAllowlistMixin.class);
		mapper.addMixIn(SessionDTO.class, SecurityAllowlistMixin.class);
		mapper.addMixIn(Long.class, SecurityAllowlistMixin.class);
		
		return mapper;
	}
	
	// 검증 통과를 위한 Jackson Mixin 클래스 선언
	@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS) // 직렬화할 때 패키지/클래스명을 JSON에 포함하고, 역직렬화할 때 이 Annotation 이 붙은 클래스를 허용하라
	private abstract static class SecurityAllowlistMixin {}
}
