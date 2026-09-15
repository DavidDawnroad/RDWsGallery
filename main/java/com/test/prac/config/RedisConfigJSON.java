package com.test.prac.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/* 260806 기준 BoardService와 UserAccountService에는 Redis Key 와 Value 가 전부 String 데이터 형식을 사용하는 로직만 작성되어 있다.
 * 따라서 기존의 파일에는 String 데이터 전용 라이브러리인 StringRedisTemplate를 의존 주입받게 바꾸고,
 * 추후 DTO/Java Object 전용 Caching 작업이 필요할 때 이 파일을 사용할 수 있도록 리팩토링한다.
 * 1. 파일의 이름을 RedisConfig -> RedisConfigJSON 으로 변경
 * 2. value 직렬화 도구를 GenericJackson2JsonRedisSerializer로 변경
 * Cache 데이터의 TTL이 만료되어 사라지는 순간 수천 건의 게시글 조회가 발생하여 Cache Stampede 현상이 발생하면 Redisson 라이브러리의 분산 락까지 추가 구현할 것
*/

@Configuration
@EnableScheduling  // 스케줄러가 활성되려면 이 Annotation 이 반드시 작성되어 있어야 한다
public class RedisConfigJSON {
	
	@Value("${spring.data.redis.host}")
	private String host;
	
	@Value("${spring.data.redis.port}")
	private int port;
	
	// Lettuce 라이브러리를 사용하는 RedisConnectionFactory(Redis 와 연결하기 위한 'Connection' 을 생성, 관리하는 method) 를 Bean 으로 등록
	@Bean
	public RedisConnectionFactory redisConnectionFactory() {
		return new LettuceConnectionFactory(host, port);
	}
	
	@Bean
	public RedisTemplate<String, Object> redisTemplate() {
		
		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
		
		// Redis 연결
		redisTemplate.setConnectionFactory(redisConnectionFactory());
		
		/* - 직렬화(Serialize)
		 * Java 의 객체나 데이터를 파일로 저장하거나 네트워크로 전송하기 위해 JSON 같은 문자열이나 Byte Stream(기계어) 형식으로 변환하는 작업
		 * 
		 * - 직렬화가 필요한 이유
		 * 메모리(RAM)는 Java 객체를 주소값 기반의 복잡한 데이터 형태로 저장한다.
		 * 하지만 이 메모리 주소값이라는 것은 '나의 JVM에서 프로그램이 켜져 있는 동안'만 유효하다.
		 * 프로그램을 재부팅해서 메모리 주소가 리셋되면 주소를 그대로 저장하는 행위가 무의미해지고,
		 * 내 메모리 주소를 타인에게 그대로 전송해봐야 타인의 메모리에는 그 주소에 그 객체가 없기 때문에 무의미하다.
		 * 메모리 주소같은 컴퓨터 내부 정보는 버리고 데이터의 값과 구조만 남겨서 저장/전송할 수 있는 형태로 만드는 것을 직렬화라고 한다. 
		 */

		/* - Java 기본 직렬화 도구인 JdkSerializationRedisSerializer 대신 StringRedisSerializer 라는 별도의 직렬화 도구를 사용하는 이유는 무엇인가?
		 *  JdkSerializationRedisSerializer는 데이터의 값과 함께 역직렬화시 필요한 패키지 경로, 타입 정보, 메타데이터 등을 특수한 Binary 형태로 저장한다.
		 *  이는 Java 환경에서만 사용하는 직렬화 방식이기 때문에 다른 언어로 제작된 서비스와 Redis 캐시 데이터를 공유할 수 없다. 
		 *  이 데이터를 조회하면 인간이 읽기 힘들고 디버깅이 불가능한 수준의 문자열로 출력된다. (대충 특수문자 붙은 16진수)
		 *  데이터의 저장 방식이 Java 클래스 구조에 의존하기 때문에 클래스 필드를 추가하거나 패키지명/클래스명을 변경하는 등의 조작이 있을 경우
		 *  Redis 에 저장된 기존 데이터를 읽어오다가 역직렬화 에러가 발생하여 서버가 다운된다.
		 *  
		 *  StringRedisSerializer는 데이터만을 지정된 문자열 바이트(평문 텍스트. 기본 설정 UTF-8)로 변환해서 저장한다.
		 *  따라서 저장 용량이 최소화되고,
		 *  인간이 쉽게 모니터링과 디버깅할 수 있으며,
		 *  데이터 저장 방식이 Java 클래스 구조에 의존하지 않기 때문에 변경 사항이 있어도 역직렬화 시 오류가 발생하지 않고,
		 *  다른 언어로 작성된 시스템과 자유롭게 데이터 공유가 가능하다.
		 *  Redis 내부에서 데이터를 숫자로 취급해야 하는 INCR 연산과의 호환성도 좋다.
		 */
		
		// StringRedisSerializer를 사용하여 Key-Value 형태로 직렬화(Serialize) 수행
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		// String Key 구조 대신 Hash Key 구조를 사용할 경우
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        
        // DTO에 LocalDateTime/ZonedDateTime 타입 필드가 있을 경우
        ObjectMapper objectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule()) // LocalDateTime/ZonedDateTime 지원 module 등록
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // 숫자 array 형태 대신 YYYY-MM-DD 같은 ISO-8601 문자열 포맷으로 데이터 저장
                .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE) // ZonedDateTime 데이터의 원본 타임존(표준시 기준지역) 유지 (ZonedDateTime 전용 설정)
                .build();
        
        // objectMapper 설정을 포함하여 jsonSerializer 직렬화 도구 생성
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        
        // Value 에는 DTO나 Java Object 가 들어갈 것이므로 String 직렬화 도구 대신 JSON 직렬화 도구 선택
		redisTemplate.setValueSerializer(jsonSerializer);
        redisTemplate.setHashValueSerializer(jsonSerializer);
        
        return redisTemplate;
	}
}
