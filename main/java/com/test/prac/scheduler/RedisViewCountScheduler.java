package com.test.prac.scheduler;

import java.util.Map;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.test.prac.repository.BoardRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
/*  application.yml의 로깅 레벨 설정으로는 스케줄러에서 BoardRepository.updateViews()를 실행시키는 시점의 SQL Query 와 Parameter 만 감지할 수 있다
 * 스케줄러의 시작/종료, Redis 에 연결하여 key 를 몇 개 가져왔는지 여부, 변경할 데이터가 없어서 Redis 가 그냥 종료된 상황 등의 구체적인 행동은 감지할 수 없다
 * Spring Boot 내장 로깅 시스템인 Logback 의 기능을 사용하려는 상황에서, Logback 을 직접 import 하여 사용하는 대신 추상화(Facade) Interface 인 SLF4J를 import 한 이유는
 * SOLID 개발 원칙 중 OCP를 구현함으로 결합도를 낮추어 추후 로깅 엔진을 Logback 에서 Log4J2 같은 고성능 로깅 엔진으로 변경하는 일 같은 확장성에 대비하기 위함이다.
 */
@Slf4j
public class RedisViewCountScheduler {
	
	private final StringRedisTemplate stringRedisTemplate;
	//private final BoardRepository boardRepository;
	private final ViewCountSyncExecutor viewCountSyncExecutor;
	
/*
	@Scheduled(cron = "0 0/10 * * * *") // (cron = "초 분 시 일 월 요일")
	@Transactional
	public void syncRedisViewCountToRDB() {
		log.info(">> [조회수 스케줄러] Redis - RDB 동기화 시작 <<");
		
		// Redis 에서 board:views:* 의 형태를 가진 key 들을 keys 에 전부 세팅
		Set<String> keys = redisTemplate.keys("board:views:*");
		
		if (keys == null || keys.isEmpty()) {
			log.info(">> [조회수 스케줄러] 동기화할 데이터 없음 <<");
			
			return;
		}
		
		int count = 0;
		
		for (String key : keys) {
			try {
				// key 에서 boardSeq 추출
				Long boardSeq = Long.parseLong(key.split(":")[2]);
				
				// Redis 에 저장된 key 의 최신 value 값 가져오기
				String nowValue = (String) redisTemplate.opsForValue().get(key);
				
				if (nowValue != null) {
					Long views = Long.parseLong(nowValue);
					
					// RDB에 반영
					boardRepository.updateViews(boardSeq, views);
					count++;
				}
			} catch (Exception e) {
				log.error(">> [조회수 스케줄러] Key: {} 동기화 중 오류 발생. 오류 메시지: {} <<", key, e.getMessage());
			}
		}
		log.info(">>[조회수 스케줄러] 게시글 조회수 데이터 {} 건 동기화 완료 <<", count);
	}
*/
	
	/* keys 에 key 를 세팅할 때, RedisTemplate.keys() 를 사용하면 메모리의 모든 key 를 가져올 때까지 Redis 서버를 멈춰둔다. -> 시간 복잡도 O(N)
	 * 만일 key 개수가 매우 많다면(조회수를 바꿔야 하는 게시글 수가 매우 많다면), 이 때 순간적으로 Redis 서버가 마비될 수 있다.
	 * 때문에 대용량 트래픽 환경에서는 key 를 Paging 처럼 일정 단위로 쪼개서 가져오는 방식을 택하여 Redis 서버의 부하를 줄인다. -> 시간 복잡도 O(1) 의 반복으로 O(N) 달성
	 */
/*	
	@Scheduled(cron = "0 0/5 * * * *") // (cron = "초 분 시 일 월 요일")
	@Transactional
	public void syncRedisViewCountToRDB() {
		// key() 대신 executeWithStickyConnection()을 사용하여 단일 Connection 의 연결 상태를 유지하면서 조금씩 key 들을 찾아온다.
		// 동일한 Connection 을 유지해야 다음 key 를 찾아올 때 key 위치와 관련된 진행 정보가 유실되지 않기 때문이다.
		redisTemplate.executeWithStickyConnection(connection -> {
			// key 를 100개씩 끊어서 가져오기
			ScanOptions options = ScanOptions.scanOptions()
											 .match("board:views:*")
											 .count(100)
											 .build();
			
			int count = 0;
			
			// scan()은 key 의 위치를 가리키는 stream 형태의 Iterator 객체(Collection 요소에 순차 접근&순회를 가능하게 하는 객체)를 반환한다.
			//  다만 내부 Connection 객체가 반환하는 데이터의 타입이 이진 데이터(byte[])이기 때문에, 자료형을 일치시켰다
			//  JDBC에서 DBUtil의 open-close를 명시하는 것처럼 Redis 의 Cursor 도 close 를 명시해야 Connection 메모리 누수를 방지할 수 있다.
			//  try-catch 문의 입출력 전용 변형 버전인 try-with-resources 문을 사용하여 try 블록 종료 시 자동으로 할당 자원을 해제할 수 있도록 조치했다.
			//  Cursor -> 대용량 데이터를 다룰 때 시스템 안정성을 보장하기 위해 사용하는 라이브러리.
			  
			try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
				
				// cursor 가 더이상 존재하지 않을 때까지 반복
				while (cursor.hasNext()) {
					// SCAN 으로 가져온 cursor 의 자료형은 이진 데이터(byte[]) 타입이다.
					byte[] keyBytes = cursor.next();
					// 이진 데이터를 String 형 데이터로 변환
					String key = new String(keyBytes, StandardCharsets.UTF_8);
					
					try {
						// key 에서 boardSeq 추출
						Long boardSeq = Long.parseLong(key.split(":")[2]);
						
						// redisTemplate.opsForValue().get() 과 비슷한 작업
						byte[] nowValuesBytes = connection.stringCommands().get(keyBytes);
						
						if (nowValuesBytes != null) {
							String nowValueString = new String(nowValuesBytes, StandardCharsets.UTF_8);
							Long views = Long.parseLong(nowValueString);
							
							// RDB에 반영
							boardRepository.updateViews(boardSeq, views);
							count++;
						}
					} catch (Exception e) {
						log.error(">> [조회수 스케줄러] Key: {} 처리 중 오류 발생. 에러 메시지: {} <<", key, e.getMessage());
					}
				} // while
				log.info(">> [조회수 스케줄러] 게시글 조회수 데이터 {} 건 동기화 완료 <<", count);
			} catch (Exception e) {
				log.error(">> [조회수 스케줄러] 조회수 SCAN용 Cursor 처리 중 오류 발생. 에러 메시지: {} <<", e.getMessage());
			} // try
			// redisTemplate.executeWithStickyConnection()은 반환값을 요구하므로 null 반환으로 종료 알림
			return null;
		});
	}
*/
	
	/* 2번째 syncRedisViewCountToRDB()에 사용이 완료된 Redis 내의 데이터를 삭제하는 부분이 없었다는 것을 차치하고서라도, 이 코드에는 문제점이 존재한다.
	 * 1. Key 의 TTL(Time-To-Live, 데이터 유효 시간)이 설정되지 않았기 때문에 Key 가 계속 생성되기만 하고 삭제되지 않아 메모리 공간에 압박(OOM)이 생기는 시점이 온다.
	 * 2. Key 가 삭제되지 않기 때문에 SCAN을 진행해야하는 범위가 계속 늘어나서 성능 저하가 발생한다. 
	 * 3. 조회수가 동기화되는 순간에 누군가가 클릭을 하는 동시성 이슈가 발생하면 해당 데이터가 유실될 수 있다. 
	 * 4. 그렇다고 TTL을 주게 되면 Key 삭제 이후 조회수 증가 이벤트가 발생했을 시 Redis 카운트가 1부터 시작하게 되어 기존 조회수가 날아갈 수도 있다.
	 * 
	 * 이런 문제점을 해결하기 위해, 3번째 syncRedisViewCountToRDB()에는 아래와 같은 방법을 사용한다.
	 * 1. 전체 데이터를 매번 다시 처리하는 대신 데이터의 증가량만 처리하는 Incremental Load(증분 적재) 방식을 사용하여 처리 속도를 향상시킨다.
	 * 2. 증분 데이터의 이름을 변경하여 데이터 격리(Isolation)를 시행하는 것으로 동시성 이슈에 따른 데이터 소실 문제를 해결한다.
	 * 3. 데이터의 구조가 String(조회수)-Value(값) 형태이고, 트랜잭션 당 처리되는 데이터들의 Life Cycle 이 모두 동일하기 때문에 String Key 대신 Hash Key 를 사용하여 Key 의 수와 메타데이터가 차지하는 용량을 줄임으로 메모리를 확보한다.
	 * 4. 사용이 끝난 변경분 Hash Key 는 unlink()로 비동기 할당 해제 처리(flush)하여 delete()를 사용했을 때 발생하는 메인 thread 지연 현상을 방지한다.
	 */
	@Scheduled(cron = "0 0/2 * * * *") // (cron = "초 분 시 일 월 요일")
	public void syncRedisViewCountToRDB() {
		log.info(">> [조회수 스케줄러] Redis - RDB 동기화 시작 <<");
		
		String deltaKey = "board:views:delta";
		String syncKey = "board:views:sync";
		
		// 이전 주기에서 서버 다운 등으로 인해 처리되지 않은 syncKey 데이터가 남아있을 경우
		if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(syncKey))) {
			log.warn(">> [조회수 스케줄러] WARN: 이전 주기에 처리되지 않은 격리 키 데이터 발견됨. 데이터 복구 절차 진행 <<");
			
			executeViewSync(syncKey);
			
			// 위 조건문의 데이터 복구 절차도 실패해서(DB 장애가 2주기(4분) 내로 해결되지 않아서) 여전히 동기화되지 않은 remainingKeys가 남아있는 경우에 대한 방어 코드
			if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(syncKey))) {
				log.error(">> [조회수 스케줄러] 데이터 복구 시도 이후에도 여전히 처리되지 않은 키 데이터를 발견. DB 상태 점검을 요함. 이번 주기의 Rename을 통한 Delta 데이터 적재 작업 보류. <<");
				
				return;
			}
		}
		
		// .hasKey()는 Wrapper 클래스 Boolean 형을 반환하기 때문에 NPE 방지를 위해 != 대신 equals() 사용
		// Schedule 기간 동안 Hash Key 가 생성되지 않은 경우 == 게시판 내 조회가 발생하지 않은 경우 (1번 method 의 key.isEmpty() 검사 같은 느낌)
		if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(deltaKey))) {
			log.info(">> [조회수 스케줄러] 동기화할 데이터 없음 <<");
			
			return;
		}
		
		try {
			// 기존 Hash Key 인 deltaKey 의 이름을 syncKey 로 변경해 실시간 데이터와 가공 데이터를 격리(Isolation)하는 것으로 원자성(All or Nothing)을 보장하는 연산 준비
			stringRedisTemplate.rename(deltaKey, syncKey);
			
			executeViewSync(syncKey);
		} catch (Exception e) {
			log.error(">> [조회수 스케줄러] Key: {} 처리 중 오류 발생. 에러 메시지: {} <<", deltaKey, e.getMessage());
		}
	}
	
	private void executeViewSync(String syncKey) {
		
		int count = 0;
		
		HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();
		
		// Redis 의 모든 Key 공간을 탐색하는 SCAN(opsForValue.scan()) 대신 특정 Hash Key 내부만 탐색하는 HSCAN(opsForHash.scan()) 사용
		// 이진 데이터 대신 Hash 구조를 반환하기 때문에 구조에 맞게 자료형 일체화
		try (Cursor<Map.Entry<String, String>> cursor = hashOperations.scan(syncKey, ScanOptions.scanOptions().count(100).build())) {
			
			while (cursor.hasNext()) {
				
				Map.Entry<String, String> entry = cursor.next();
				
				try {
					Long boardSeq = Long.parseLong(String.valueOf(entry.getKey()));
					Long delta = Long.parseLong(String.valueOf(entry.getValue()));
					
					// 오류가 발생해도 해당 행만 ROLLBACK 하고 그 다음 행 작업은 별개의 트랜잭션으로 돌아갈 수 있도록 내부 컴포넌트의 method 호출
					// (롤백으로 유실되는 데이터를 오류행~끝행에서 1행 치로 최소화)
					viewCountSyncExecutor.updateSingleBoardViews(delta, boardSeq);
					// RDB에 해당 Key 의 Value 가 성공적으로 저장되었다면 Key 를 바로 삭제해서 서버 다운 후 재부팅 시의 더블 카운팅 문제 방지
					hashOperations.delete(syncKey, entry.getKey());
					count++;
				} catch (Exception e) {
					log.error(">> [조회수 스케줄러] {} 번 게시글 조회수 동기화 처리 중 오류 발생하여 해당 게시글 무시. 에러 메시지: {} <<", entry.getKey(), e.getMessage());
				}
			}
		} catch (Exception e) {
			log.error(">> [조회수 스케줄러] Cursor HSCAN 작업 중 오류 발생. 에러 메시지: {} <<", e.getMessage());
		}
		
		log.info(">> [조회수 스케줄러] 게시글 조회수 데이터 {} 건 동기화 완료 <<", count);
		
		// 동기화 처리 중 오류가 발생한 Key 들의 개수 카운팅 (모두 정상적으로 처리되었다면 try 문 안에서 동기화가 성공한 직후 Hash Delete 되었을 것이므로 0 or NULL)
		Long remainingKeys = stringRedisTemplate.opsForHash().size(syncKey);
		
		if (remainingKeys != null && remainingKeys > 0) {
			log.warn(">> [조회수 스케줄러] 게시글 조회수 데이터 {} 건 동기화 실패. 다음 주기에 재시도 예정 <<", remainingKeys);
		} else {
			// 적재 작업을 마친 (구)deltaKey 를 keyspace 에서 삭제
			// 실제 데이터 삭제 과정은 백그라운드에서 별도의 thread 가 비동기로 처리하도록 하여 프로세스 차단(Blocking)으로 인한 메인 thread 지연 현상 방지
			stringRedisTemplate.unlink(syncKey);

			log.info(">> [조회수 스케줄러] 백그라운드 스레드의 비동기 메모리 unlink 작업 완료 <<");
		}
	}
	
	// 독립된 트랜잭션 보장을 위한 내부 컴포넌트 선언
	@Component
	@RequiredArgsConstructor
	public static class ViewCountSyncExecutor {
		
		private final BoardRepository boardRepository;
		
		// while 문이 돌다가 특정 게시글 행에서 오류가 발생하면서 외부 트랜잭션이 전부 ROLLBACK-Only 상태로 변경될 경우
		// Propagation.REQUIRES_NEW 옵션으로 내부 트랜잭션을 분리해 오류 이후의 게시글 행에 대한 작업까지 전부 ROLLBACK 상태로 전파(Propagation)되는 상황을 방지
		@Transactional(propagation = Propagation.REQUIRES_NEW)
		public void updateSingleBoardViews(Long delta, Long boardSeq) {
			boardRepository.updateViews(delta, boardSeq);
		}
	}
	
	/* 조회수 증가라는 기능을 가지고 Entity Setter -> @Modified + Atomic Query -> Redis -> SCAN -> HASH SCAN + 데이터 격리 -> 트랜잭션 전파 분리 라는 리팩토링 과정을 거쳤다.
	 * 대용량 트래픽 환경임을 가정할 경우, 이 코드는 DeadLock 을 방지할 수는 있지만 내외부 트랜잭션 분리로 인한 네트워크 비용 증가와 스케줄러 처리 시간 지연(오버헤드)이라는 단점도 존재한다.
	 * 시스템 장애가 발생할 수 있는 이론적 시나리오는 코드의 모든 불안 요소를 해결하기 전까지 멈추지 않을 것이다. 
	 * 추가적인 기술을 도입하지 않는 선에서 최대한 데이터 정합성을 지키면서 시스템 자원 사용량을 조절하는 것으로 리팩토링 작업을 멈춘다.
	 * */
	
	/* Hash Key 는 메모리를 절감하는 데 탁월한 성능을 보이지만, 그럼에도 String Key 를 사용해야 하는 상황들이 존재한다.
	 * 1. 데이터의 Life Cycle 이 전부 달라서 개별적인 TTL(Time-To-Live)이 필요할 경우
	 * 2. 재고 선점 등의 비즈니스 이슈로 여러 서버가 동시에 같은 자원에 접근하는 상황을 제어하는 분산 락(Distributed Lock)을 구현해야 하는 경우 (Lettuce를 사용하고 있더라도 Redisson 라이브러리 추가 권장)
	 * 3. 초대용량 트래픽 환경에서 Hash Key 하나에 모든 데이터를 저장함으로 서버에 부하가 생기는 Big Key Problem 을 해결해야 하는 경우
	 * 
	 * Redis 단일 서버로 해결하기 어려운 Big Key Problem(또는 Hotspot Problem)은 몇 가지 해결 방식을 같이 사용하는 것으로 해결하는데, 그 방법들은 아래와 같다.
	 * 1. API 서버로 수많은 트래픽이 순간적으로 들어오면 Kafka 시스템의 Queue 에 요청들을 쌓아두고 후속 시스템(Redis)이 처리할 수 있는 양만큼만 Pulling 하여 부하를 제어한다.
	 * 2. Redis 서버를 여러 개 두고(Clustering) 데이터를 분산시켜 저장하는 Hash Sharding 방식을 사용하여 서버 부하를 제어한다.
	 */
}
