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
@Slf4j
public class RedisLikeCountScheduler {

	private final StringRedisTemplate stringRedisTemplate;
	private final LikeCountSyncExecutor likeCountSyncExecutor;
	
	@Scheduled(cron = "0 0/2 * * * *")
	public void syncRedisLikeCountToRDB() {
		
		syncLikes();
		syncDislikes();
	}
	
	private void syncLikes() {
		log.info(">> [추천수 스케줄러] Redis - RDB 동기화 시작 <<");
		
		String deltaKey = "board:likes:delta";
		String syncKey = "board:likes:sync";
		
		if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(syncKey))) {
			log.warn(">> [추천수 스케줄러] WARN: 이전 주기에 처리되지 않은 격리 키 데이터 발견됨. 데이터 복구 절차 진행 <<");
			
			executeLikeSync(syncKey);
			
			if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(syncKey))) {
				log.error(">> [추천수 스케줄러] 데이터 복구 시도 이후에도 여전히 처리되지 않은 키 데이터를 발견. DB 상태 점검을 요함. 이번 주기의 Rename을 통한 Delta 데이터 적재 작업 보류. <<");
				
				return;
			}
		}
		
		if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(deltaKey))) {
			log.info(">> [추천수 스케줄러] 동기화할 데이터 없음 <<");
			
			return;
		}
		
		try {
			stringRedisTemplate.rename(deltaKey, syncKey);
			
			executeLikeSync(syncKey);
			
		} catch (Exception e) {
			log.error(">> [추천수 스케줄러] Key: {} 처리 중 오류 발생. 에러 메시지: {} <<", deltaKey, e.getMessage());
		}
	}
	
	private void syncDislikes() {
		log.info(">> [비추수 스케줄러] Redis - RDB 동기화 시작 <<");
		
		String deltaKey = "board:dislikes:delta";
		String syncKey = "board:dislikes:sync";
		
		if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(syncKey))) {
			log.warn(">> [비추수 스케줄러] WARN: 이전 주기에 처리되지 않은 격리 키 데이터 발견됨. 데이터 복구 절차 진행 <<");
			
			executeDislikeSync(syncKey);
			
			if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(syncKey))) {
				log.warn(">> [비추수 스케줄러] 데이터 복구 시도 이후에도 여전히 처리되지 않은 키 데이터를 발견. DB 상태 점검을 요함. 이번 주기의 Rename을 통한 Delta 데이터 적재 작업 보류. <<");
		
				return;
			}
		}
		
		if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(deltaKey))) {
			log.info(">> [비추수 스케줄러] 동기화할 데이터 없음 <<");
			
			return;
		}
		
		try {
			stringRedisTemplate.rename(deltaKey, syncKey);
			
			executeDislikeSync(syncKey);
			
		} catch (Exception e) {
			log.error(">> [비추수 스케줄러] Key: {} 처리 중 오류 발생. 에러 메시지: {} <<", deltaKey, e.getMessage());
		}
	}
	
	private void executeLikeSync(String syncKey) {
		
		int count = 0;
		
		HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();
		
		try (Cursor<Map.Entry<String, String>> cursor = hashOperations.scan(syncKey, ScanOptions.scanOptions().count(100).build())) {
			
			while (cursor.hasNext()) {
				
				Map.Entry<String, String> entry = cursor.next();
				
				try {
					Long boardSeq = Long.parseLong(String.valueOf(entry.getKey()));
					Long delta = Long.parseLong(String.valueOf(entry.getValue()));
					
					likeCountSyncExecutor.updateSingleBoardLikes(delta, boardSeq);
					stringRedisTemplate.opsForHash().delete(syncKey, entry.getKey());
					count++;
				} catch (Exception e) {
					log.error(">> [추천수 스케줄러] {} 번 게시글 추천수 동기화 처리 중 오류 발생하여 해당 게시글 무시. 에러 메시지: {} <<", entry.getKey(), e.getMessage());
				}
			}
		} catch (Exception e) {
			log.error(">> [추천수 스케줄러] Cursor HSCAN 작업 중 오류 발생. 에러 메시지: {} <<", e.getMessage());
		}
		
		log.info(">> [추천수 스케줄러] 게시글 추천수 데이터 {} 건 동기화 완료 <<", count);
		
		Long remainingKeys = hashOperations.size(syncKey);
		
		if (remainingKeys != null && remainingKeys > 0) {
			log.warn(">> [추천수 스케줄러] 게시글 추천수 데이터 {} 건 동기화 실패. 다음 주기에 재시도 예정 <<", remainingKeys);

		} else {
			stringRedisTemplate.unlink(syncKey);
			log.info(">> [추천수 스케줄러] 백그라운드 스레드의 비동기 메모리 unlink 작업 완료 <<");
		}
	}
	
	private void executeDislikeSync(String syncKey) {
		
		int count = 0;
		
		HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();
		
		try (Cursor<Map.Entry<String, String>> cursor = hashOperations.scan(syncKey, ScanOptions.scanOptions().count(100).build())) {
			
			while (cursor.hasNext()) {
				
				Map.Entry<String, String> entry = cursor.next();
				
				try {
					Long boardSeq = Long.parseLong(String.valueOf(entry.getKey()));
					Long delta = Long.parseLong(String.valueOf(entry.getValue()));
					
					likeCountSyncExecutor.updateSingleBoardDislikes(delta, boardSeq);
					stringRedisTemplate.opsForHash().delete(syncKey, entry.getKey());
					count++;
				} catch (Exception e) {
					log.error(">> [비추수 스케줄러] {} 번 게시글 비추천수 동기화 처리 중 오류 발생하여 해당 게시글 무시. 에러 메시지: {} <<", entry.getKey(), e.getMessage());
				}
			}
		} catch (Exception e) {
			log.error(">> [비추수 스케줄러] Cursor HSCAN 작업 중 오류 발생. 에러 메시지: {} <<", e.getMessage());
		}
		
		log.info(">> [비추수 스케줄러] 게시글 비추천수 데이터 {} 건 동기화 완료 <<", count);
		
		Long remainingKeys = hashOperations.size(syncKey);
		
		if (remainingKeys != null && remainingKeys > 0) {
			log.warn(">> [비추수 스케줄러] 게시글 비추천수 데이터 {} 건 동기화 실패. 다음 주기에 재시도 예정 <<", remainingKeys);

		} else {
			stringRedisTemplate.unlink(syncKey);
			log.info(">> [비추수 스케줄러] 백그라운드 스레드의 비동기 메모리 unlink 작업 완료 <<");
		}
	}
	
	@Component
	@RequiredArgsConstructor
	public static class LikeCountSyncExecutor {
		
		private final BoardRepository boardRepository;
		
		@Transactional(propagation = Propagation.REQUIRES_NEW)
		public void updateSingleBoardLikes(Long delta, Long boardSeq) {
			boardRepository.increaseLikes(delta, boardSeq);
		}
		
		@Transactional(propagation = Propagation.REQUIRES_NEW)
		public void updateSingleBoardDislikes(Long delta, Long boardSeq) {
			boardRepository.increaseDislikes(delta, boardSeq);
		}
	}
}
