package com.test.prac.service;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.test.prac.config.UserDetailsCustom;
import com.test.prac.dto.BoardCreateRequestDTO;
import com.test.prac.dto.BoardIncreaseViewsRequestDTO;
import com.test.prac.dto.BoardShowListResponseDTO;
import com.test.prac.dto.BoardShowPostResponseDTO;
import com.test.prac.dto.BoardUpdateRequestDTO;
import com.test.prac.dto.ReplyCreateRequestDTO;
import com.test.prac.dto.ReplyShowListResponseDTO;
import com.test.prac.entity.BoardEntity;
import com.test.prac.entity.ReplyEntity;
import com.test.prac.entity.UserAccountEntity;
import com.test.prac.enums.UStat;
import com.test.prac.exception.UserNotFoundException;
import com.test.prac.repository.BoardRepository;
import com.test.prac.repository.ReplyRepository;
import com.test.prac.repository.UserAccountRepository;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j // 로그 작성용 lombok 라이브러리
public class BoardService {

	private final BoardRepository boardRepository;
	private final UserAccountRepository userRepository;
	private final ReplyRepository replyRepository;
	private final StringRedisTemplate stringRedisTemplate;

//	// 조회수 증가
//	@Transactional
//	public void increaseViews(Long boardSeq) {
//		
//		int changedRows = boardRepository.increaseViews(boardSeq);
//		
//		// 조회수가 증가된 게시글이 0개일 경우
//		if (changedRows == 0) {
//			throw new IllegalArgumentException("존재하지 않거나 삭제된 게시글입니다.");
//		}
//	}
	
	// 조회수 증가 (Redis 도입으로 대용량 트래픽에 최적화된 버전)
	/* - @Transactional이 제거된 이유
	 * 이 버전의 increaseViews()는 모든 요청이 RDB를 직접 건드리지 않고 Redis 메모리에서 increment() 연산만 진행된다.
	 * 만약 @Transactional이 있다면 DB Access 를 하지 않는 수많은 Redis 단계 요청들이 DB Connection 만 점유하고 있는 상황이 된다.
	 * 이는 서버를 다운시키는 Connection Pool Starving 현상을 유발할 수 있다.
	 * 그리고 @Transactional은 RDB의 트랜잭션을 제어하는 용도로 쓰이기 때문에, Redis 연산은 애초에 제어할 수가 없다.
	 * 에러가 발생해서 RDB 데이터가 ROLLBACK 되더라도 이미 Redis 에서 1 증가된 조회수 데이터는 ROLLBACK할 수가 없다는 뜻이다.
	 */
	
	/* 처음 채택했던 Entity Setter 를 활용한 어플리케이션 레벨(JVM RAM)의 데이터 수정 방식은 동시성 이슈 발생 시 데이터가 유실될 가능성이 존재한다는 단점을 가지고 있었다.
	 * 기존 방식을 @Modifying을 포함한 DB 레벨의 원자적(Atomic) JPQL Query 방식으로 변경하여 데이터 정합성을 확보하는 것으로 단점을 개선했다.
	 * 하지만 대용량 트래픽 환경에서의 X-Lock 경합으로 발생하는 DB 커넥션 풀 고갈(Connection Pool Starvation) 현상이라는 새로운 단점이 생겼고
	 * 이는 Redis 기반의 인-메모리 연산 구조를 도입함으로 해결했다.
	 * 수많은 동시성 이슈가 발생하더라도 RDB의 UPDATE 쿼리로 바로 이동하지 않고, Redis 의 increment() 연산만 진행하기 때문에 수많은 Locking 이 발생하는 원인이 사라졌다.
	 * 조회수 증가가 반영된 조회수도 DISK 레벨까지 이동하지 않고 Memory 레벨의 Redis 에서 가져오기 때문에 수천 수만 건의 method 요청에도 처리 속도가 매우 빠르다.
	 * 특정 시간마다 Redis 에 쌓인 데이터를 스케줄러로 한 번에 UPDATE 벌크 연산으로 수행하면 되기 때문에 DB에 가해지는 부담 자체도 적다.
	 */
	public void increaseViews(Long boardSeq, BoardIncreaseViewsRequestDTO viewsDTO) {
		
		// RDBMS의 테이블처럼 namespace 설정
		// : 의 역할은 폴더 계층 구조처럼 키를 시각화하는데 쓰기 위함
		String deltaKey = "board:views:delta";
		
		// increase-views()가 호출될 때 JavaScript에서 viewsToken 데이터를 넘겨줬는지 검사 (올바른 조회수 증가 요청인지 확인)
		String tokenKey = "board:view-count-token:" + boardSeq + ":" + viewsDTO.viewsToken();
		
		// getAndDelete()로 토큰 값을 가져온 직후 삭제해서 토큰을 여러 번 사용할 수 없게 방지
		String tokenValue = stringRedisTemplate.opsForValue().getAndDelete(tokenKey);
		
		if (tokenValue == null) {
			return;
		}
		
		// Hash Key(deltaKey) 내의 특정 Hash(identifyKeyField, 게시글 번호에 따른 조회수) 값을 delta(1L)만큼 증가 
		stringRedisTemplate.opsForHash().increment(deltaKey, String.valueOf(boardSeq), 1L);
	}
	
	// 추천수 증가
	public Long increaseLikes(Long boardSeq, Long userSeq) {
		
		// 실제 좋아요 수를 가져오기 위해 increaseLikes()가 반영된 BoardEntity 조회
		BoardEntity boardEntity = boardRepository.findById(boardSeq)
				 .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
		
		// 매일 자정 기준으로 추천/비추천 기능을 재활성화하기 위해 TTL을 설정할 수 있는 Redis Key 생성 
		// Hash Key 는 Key 각각의 TTL을 설정할 수 없기 때문에 여기서는 사용할 수 없음
		String restrictKey = "board:likes:restrict:" + boardSeq + ":" + userSeq;
		
		// 사용자의 거주 국가에 따라 달라지는 LocalDateTime 대신 KST를 표준시로 삼기 위해 ZonedDateTime 선택
		ZonedDateTime nowKST = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
		ZonedDateTime midnightKST = nowKST.plusDays(1).with(LocalTime.MIDNIGHT);		
		// 추천을 누른 시각인 nowKst부터 자정까지 몇 초가 남았는지 계산
		// 정확히 0시 0분 0초 000에 추천을 눌렀을 경우를 대비하여 Math.max()로 예외 처리
		long lastSecondsToMidnight = Math.max(1, Duration.between(nowKST, midnightKST).getSeconds());
		
		// opsForValue().setIfAbsent() -> Key 가 없으면 Key 와 값을 저장하고 TRUE를 반환하고, Key 가 있으면(이미 추천을 눌렀으면) FALSE를 반환 (Redis 명령어로는 NX)
		Boolean isFirstTime = stringRedisTemplate.opsForValue().setIfAbsent(restrictKey, "LIKED", lastSecondsToMidnight, TimeUnit.SECONDS);
		
		if (Boolean.FALSE.equals(isFirstTime)) {
			throw new IllegalArgumentException("추천은 하루에 한 번만 가능합니다.");
		}
		
		// RDB에 UPDATE를 바로 하지 않고, Hash Key 를 생성하여 delta 값 적재
		String deltaKey = "board:likes:delta";
		
		Long currentLikeCountDelta = stringRedisTemplate.opsForHash().increment(deltaKey, String.valueOf(boardSeq), 1L);
		
		log.info("[추천 로그] {} 번 게시글에서 유저번호 {} 의 추천 발생. 이 게시글의 누적 증가량: {}, 발생 시각: {}", boardSeq, userSeq, currentLikeCountDelta, nowKST.toLocalDateTime());
		
		long boardEntityLikes = boardEntity.getLikes() != null ? boardEntity.getLikes() : 0L;
		
		return boardEntityLikes + (currentLikeCountDelta != null ? currentLikeCountDelta : 0L);
	}
	
	// 비추천수 증가
	public Long increaseDislikes(Long boardSeq, Long userSeq) {
		
		BoardEntity boardEntity = boardRepository.findById(boardSeq)
				 .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
		
		String restrictKey = "board:dislikes:restrict:" + boardSeq + ":" + userSeq;
		
		ZonedDateTime nowKST = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
		ZonedDateTime midnightKST = nowKST.plusDays(1).with(LocalTime.MIDNIGHT);
		long lastSecondsToMidnight = Math.max(1, Duration.between(nowKST, midnightKST).getSeconds());
		
		Boolean isFirstTime = stringRedisTemplate.opsForValue().setIfAbsent(restrictKey, "DISLIKED", lastSecondsToMidnight, TimeUnit.SECONDS);
		
		if (Boolean.FALSE.equals(isFirstTime)) {
			throw new IllegalArgumentException("비추천은 하루에 한 번만 가능합니다.");
		}
		
		String deltaKey = "board:dislikes:delta";
		
		Long currentDislikeCountDelta = stringRedisTemplate.opsForHash().increment(deltaKey, String.valueOf(boardSeq), 1L);
		
		log.info("[비추 로그] {} 번 게시글에서 유저번호 {} 의 비추 발생. 이 게시글의 누적 증가량: {}, 발생 시각: {}", boardSeq, userSeq, currentDislikeCountDelta, nowKST.toLocalDateTime());
		
		long boardEntityDislikes = boardEntity.getDislikes() != null ? boardEntity.getDislikes() : 0L;
		
		return boardEntityDislikes + (currentDislikeCountDelta != null ? currentDislikeCountDelta : 0L);
	}
	
	// 게시글 등록
	@Transactional
	public void createPost(BoardCreateRequestDTO createDTO, Long seq) {
		
		UserAccountEntity userEntity = userRepository.findById(seq)
				.orElseThrow(() -> new UserNotFoundException("사용자 정보를 찾을 수 없습니다."));
		
		if (userEntity.getUserStatus() != UStat.NORMAL) {
			throw new IllegalArgumentException("올바른 계정 상태가 아닙니다.");
		}
		
		BoardEntity boardEntity = BoardEntity.builder().userEntity(userEntity)
													   .category(createDTO.getCategory())
													   .title(createDTO.getTitle())
													   .content(createDTO.getContent())
													   .build();
		
		boardRepository.save(boardEntity);
	}
	
	// 게시글 목록(Board.html) 조회
	// findAllWithUser()에서 Fetch Join 으로 데이터를 이미 다 가져왔기 때문에 이후의 Redis 작업을 생각하면 @Transactional이 없어도 된다고 판단.
	// 오히려 @Transactional을 유지할 경우 opsForHash.multiGet()에서 시간이 지체되면서 Connection Pool Starvation 이 발생할 확률이 올라감.
	public Page<BoardShowListResponseDTO> readBoardList(Pageable pageable) {
		
		// Paging 용으로 작성한 Query DSL 커스텀 메서드 호출. Paging 작업 에 필요한 상세 파라미터는 PageRenderController 에서 제공
		Page<BoardEntity> boardEntityPage = boardRepository.findAllWithUser(pageable);
		
		// 글이 0개인 경우에 대한 예외 처리
		if (boardEntityPage.isEmpty()) {
			return Page.empty(pageable);
		}
		
	/*  1
		// List<Object>형을 반환하는 데이터 조회 메서드 opsForHash().multiGet()이 두번째 파라미터로 요구하는 Collection<Object>형 변수를 만들기 위한 작업
		// getContent().stream()으로 데이터를 일렬로 먼저 빼내고,
		// 그 중 진짜 필요한 데이터인 boardSeq를 Redis 가 Key 를 저장할 때 내부 시스템적으로 요구하는 자료형 중 하나인 String 형으로 형변환하고,
		// stream().map()으로 내부 함수 결과가 적용된 새로운 Stream 형 데이터 스트림을 반환한다.
		// 이후의 로직에 List<Object>형 데이터가 필요하기 때문에 map()으로 반환되는 데이터 타입을 Stream<String> 에서 Stream<Object> 형으로 캐스팅했다.
		// 여기서의 map()은 Java.util.Map의 자료형 Map 이 아님에 유의.
		List<Object> boardSeqs = boardEntityPage.getContent().stream().<Object>map(entity -> String.valueOf(entity.getBoardSeq())).toList();
		
		String deltaKey = "board:views:delta";
		// Object 형으로 반환되는 opsForHash().get()의 결과물을 opsForHash().multiGet()으로 한 번에 일괄(Batch) 조회
		List<Object> deltas = redisTemplate.opsForHash().multiGet(deltaKey, boardSeqs);
		
		int index = 0;
	    List<BoardShowListResponseDTO> dtoList = new ArrayList<>();
	    for (BoardEntity boardEntity : boardEntityPage.getContent()) {
	    	// deltas 의 데이터를 하나씩 순서대로 꺼내서 조회수 delta 값을 DTO에 넣을 수 있게 Long 형으로 변환
	    	Object deltaValueObj = (deltas != null && deltas.size() > index) ? deltas.get(index++) : null;
	    	long delta = (deltaValueObj != null) ? Long.parseLong(String.valueOf(deltaValueObj)) : 0L;
	    	
	    	dtoList.add(BoardShowListResponseDTO.of(boardEntity, delta));
	    }
	*/
	/*	2
		// board:delta:views 로 조회수만 관리하던 기존 Hash Key 형태에서 board:delta:{boardSeq} 형태 + 조회수/추천수/비추천수 필드로 키를 변경하기 위한 필드 선언 작업
		List<Object> fields = List.of("views", "likes", "dislikes");
		
		// WAS-Redis 간 네트워크 통신에서의 왕복 시간과 응답 속도를 줄이기 위해 다회의 요청 대신 한 번의 대량 요청을 하는 것이 파이프라이닝(Pipelining) 작업이다.
		// Low-Level의 byte[] 조작을 피하고 High-Level API의 가독성과 편의성을 얻기 위해 Pipelining 작업에 RedisCallBack 인터페이스 대신 SessionCallBack 인터페이스를 선택했다.
		// RedisCallBack은 파라미터로 RedisConnection을 전달받고, 키/필드/응답결과도 전부 byte[] 형태를 가지기 때문에 byte[] <-> String 파싱 로직을 직접 구현해야 한다.
		// SessionCallBack은 파라미터로 RedisTemplate의 Generic Interface 인 RedisOperations를 전달받고, 키/필드에 String 을 사용할 수 있으며 응답결과도 String/Long으로 자동 역직렬화하여 반환한다.
		// 극단적인 성능 최적화가 필요한 상황에서는 내부 직렬화 작업을 거치지 않는 RedisCallBack을 사용하지만, 일반적인 1RTT 파이프라이닝에는 SessionCallBack을 사용한다.
		
		// 그런데, SessionCallBack 인터페이스의 execute 추상 메서드는 자체 타입 파라미터가 <K, V>인 제네릭 메서드이다.
		// 람다식을 사용하기 위한 조건 중 하나는 '추상 메서드가 제네릭 메서드가 아닐 것' 이다. Java 언어의 명세 상 람다 표현식은 메서드 레벨 제네릭을 표현할 수 있는 방법이 없기 때문이다.
		// 따라서 SessionCallBack처럼 자체 제네릭 타입 메서드를 가진 인터페이스들은 람다식 대신 익명 내부 클래스(Anonymous Inner Class) 형태로 구현해야 한다.
		// results 에는 게시글 1개와 대응되는 Key 들 내부의 필드들(조회수/추천수/비추천수)에 들어갈 값이 List 형태로 저장된다.
		List<Object> valuesOfFields = redisTemplate.executePipelined(new SessionCallback<Object>() {
			@Override
			@SuppressWarnings("unchecked") // 강제 형변환 시 발생하는 안전성 미검증 경고를 무시하고 의도된 형변환임을 알리기 위한 Annotation
			public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
				// 기존의 operations 객체를 <K, V> -> <String, Object>로 다운캐스팅 (multiGet() 사용 시 오류가 나지 않도록 제네릭 타입 객체를 명확한 타입으로 선언)
				RedisOperations<String, Object> redisOperations = (RedisOperations<String, Object>) operations;
				
				for (BoardEntity boardEntity : boardEntityPage.getContent()) {
					String deltaKey = "board:delta:" + boardEntity.getBoardSeq();
					
					// 이 multiGet 명령어들이 매번 실행되는 대신 Queue 에 쌓였다가 Redis 로 한 번에 전송됨
					redisOperations.opsForHash().multiGet(deltaKey, fields);
				}
				return null; // pipelining 결과인 multiGet 결과물들(3개 필드의 값이 들어간 List<Object>(ex- [10L, 2L, 0L]))로 반환값이 대체되기 때문에 null 로 둔다
			}
		});
		
		int index = 0;
	    List<BoardShowListResponseDTO> dtoList = new ArrayList<>();
	    
	    for (BoardEntity boardEntity : boardEntityPage.getContent()) {
	    	// valuesOfFields에서 리스트를 하나씩 꺼내서 viewsDelta, likesDelta, dislikesDelta에 값 적재
	    	@SuppressWarnings("unchecked")
	    	List<Object> deltasObj = (valuesOfFields != null && valuesOfFields.size() > index) ? (List<Object>) valuesOfFields.get(index++) : null;
	    	
	    	long viewsDelta = parseDelta(deltasObj != null ? deltasObj.get(0) : null);
	    	long likesDelta = parseDelta(deltasObj != null ? deltasObj.get(1) : null);
	    	long dislikesDelta = parseDelta(deltasObj != null ? deltasObj.get(2) : null);
	    	
	    	// 미리 준비한 BoardShowListResponseDTO 정적 팩토리 메서드 양식에 맞게 포장해서 dtoList에 적재
	    	dtoList.add(BoardShowListResponseDTO.of(boardEntity, viewsDelta, likesDelta, dislikesDelta));
	    }
	*/
		// boardSeqs는 Object 로 형변환된 각 게시글의 seq 값이 모여서 List 형태를 이루고 있는 변수.
		// 실제 seq 값은 String 형이지만, boardSeqs를 두번째 파라미터로 요구하는 multiGet()에서는 Collection<String> 타입인 boardSeq를 Collection<Object> 형태로 요구한다.
		// stream 을 통해 이동하는 map 형 데이터의 타입이 Stream<String> -> Stream<Object>가 되게끔 하여 toList() 수행 시 바로 Collection<Object> 형태가 되도록 우회했다.
		// 굳이 이런 작업을 거치는 이유는 제네릭 타입이 Collection<Object>와 Collection<String>을 연관 관계가 아닌 별개의 타입으로 취급하기 때문이다.
		
		// 제네릭 타입(<K>, <V>, <T>, <E> 등)은 Array + Object 로 모든 데이터를 처리하던 시절의 고질적인 문제였던 '런타임 시점의 타입 에러 발생 가능성'을 컴파일 시점에서 잡아내기 위해 도입되었다.
		// 하위 타입으로 선언한 객체를 상위 타입으로 우회하는 방식은 런타임 에러를 유발할 수 있기 때문에, 제네릭 타입을 사용한 방식에서는 이같은 우회법을 아예 컴파일 오류로 취급하여 차단한다.

	//  List<Object> boardSeqs = boardEntityPage.getContent().stream().<Object>map(entity -> String.valueOf(entity.getBoardSeq())).toList();
		// StringRedisTemplate을 주입받음에 따라 List<Object>형 대신 List<String>형으로 타입 변경
		List<String> boardSeqs = boardEntityPage.getContent().stream().map(entity -> String.valueOf(entity.getBoardSeq())).toList();
		
		// 도메인에 맞게 분류된 키 3개를 쉽게 제어하기 위해 List<String> 타입으로 묶어서 선언
		List<String> metricKeys = List.of("board:views:delta", "board:likes:delta", "board:dislikes:delta");
		
		// executePipelined() 내부에서 사용할 SessionCallback 인터페이스의 execute() 제네릭 메서드를 Anonymous Inner Class 방식으로 구현
		List<Object> deltasOfKeys = stringRedisTemplate.executePipelined(new SessionCallback<Object>() {
		    @Override
		    @SuppressWarnings("unchecked")
		    public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
	//	    	RedisOperations<String, Object> redisOperations = (RedisOperations<String, Object>) operations;
		    	RedisOperations<String, String> redisOperations = (RedisOperations<String, String>) operations;
		    	
		    	// opsForHash()는 HashOperations<K, HK, HV>를 반환하는 제네릭 메서드인데, 제네릭 타입 추론 방식 대신 연산결과의 타입을 정확히 명시하여 타입 불일치 오류 발생 방지
		    	HashOperations<String, String, String> hashOperations = redisOperations.opsForHash();
		    	
		    	// key 이름과 필드 이름을 파라미터로 하여 조건에 맞는 값을 Redis Hash 내부에서 조회 (Queue 에 요청들을 쌓았다가 한 번에 Redis 로 전송)
		    	for (String metricKey : metricKeys) {
		    		hashOperations.multiGet(metricKey, boardSeqs);
		    	}
		    	// pipelining 결과인 multiGet 결과물들(3개 필드의 값이 들어간 List<Object>형 데이터ㅡ예를 들면 [10L, 2L, 0L]ㅡ)로 반환값이 대체되기 때문에 null 로 둔다
		    	return null;
		    }
		});
		
		// 게시글 번호를 필드 이름으로 갖는 board:views:delta 의 값들
		@SuppressWarnings("unchecked")
		List<Object> viewsDeltas = (deltasOfKeys != null && deltasOfKeys.size() > 0) ? (List<Object>) deltasOfKeys.get(0) : Collections.emptyList();
		@SuppressWarnings("unchecked")
		List<Object> likesDeltas = (deltasOfKeys != null && deltasOfKeys.size() > 1) ? (List<Object>) deltasOfKeys.get(1) : Collections.emptyList();
		@SuppressWarnings("unchecked")
		List<Object> dislikesDeltas = (deltasOfKeys != null && deltasOfKeys.size() > 2) ? (List<Object>) deltasOfKeys.get(2) : Collections.emptyList();
		
		int index = 0;
	    List<BoardShowListResponseDTO> dtoList = new ArrayList<>();
	    
	    for (BoardEntity boardEntity : boardEntityPage.getContent()) {
	    	long viewsDelta = parseDelta(viewsDeltas.get(index));
	    	long likesDelta = parseDelta(likesDeltas.get(index));
	    	long dislikesDelta = parseDelta(dislikesDeltas.get(index));
	    	
	    	index++;
	    	
	    	dtoList.add(BoardShowListResponseDTO.of(boardEntity, viewsDelta, likesDelta, dislikesDelta));
	    }
		
	    return new PageImpl<>(dtoList, pageable, boardEntityPage.getTotalElements());
	}
	
	// 게시글 조회
	// findById()는 readBoardList().findAllWithUsers()처럼 Fetch Join 을 사용하지 않고
	// BoardEntity.userEntity가 FetchType.LAZY이기 때문에 @Transactional로 세션을 유지해야 함
	@Transactional(readOnly = true)
	public BoardShowPostResponseDTO readPost(Long boardSeq) {
		
		BoardEntity boardEntity = boardRepository.findById(boardSeq)
				 .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
		
		// 실제로 존재하는 게시글에 대한 조회 요청임이 확인될 경우 조회수를 늘려도 좋다는 임시 허가 토큰 발급
		String viewsToken = increaseViewsVerifyToken(boardSeq);

	/*	1 (get() 3번)
		// [도메인(board)]:[목적(조회수)]:[식별자(게시글번호)] 구조로 RDBMS의 테이블처럼 namespace 설정
		// : 의 역할은 폴더 계층 구조로 키를 시각화하는데 쓰기 위함
		String viewsDeltaKey = "board:views:delta";
		String likesDeltaKey = "board:likes:delta";
		String dislikesDeltaKey = "board:dislikes:delta";
		
		// 개별 게시글 번호 식별을 위해 boardSeq를 String 형으로 형변환
		String keyFieldIdentifier = String.valueOf(boardSeq);
		
		// deltaKey의 keyFieldIdentifier 필드를 get()으로 조회 (Object 형으로 반환되는 board:views:특정 boardSeq Hash Key 의 값 조회)
		Object viewsDeltaObj = redisTemplate.opsForHash().get(viewsDeltaKey, keyFieldIdentifier);
		Object likesDeltaObj = redisTemplate.opsForHash().get(likesDeltaKey, keyFieldIdentifier);
		Object dislikesDeltaObj = redisTemplate.opsForHash().get(dislikesDeltaKey, keyFieldIdentifier);

		// Wrapper 타입 변수에 대한 null 처리와 long 타입 캐스팅 진행
		long viewsDelta = (viewsDeltaObj != null) ? Long.parseLong(String.valueOf(viewsDeltaObj)) : 0L;
		long likesDelta = (likesDeltaObj != null) ? Long.parseLong(String.valueOf(likesDeltaObj)) : 0L;
		long dislikesDelta = (dislikesDeltaObj != null) ? Long.parseLong(String.valueOf(dislikesDeltaObj)) : 0L;
	*/
		
	/*	2 (Hash Get() 1번 + RedisCallback 이 포함된 Pipelining 작업)
		// get()을 3번 사용하면 Redis 서버와 WAS 간 3번의 RTT(Round Trip Time)가 발생하여 성능이 저하되기 때문에,
		// 3번의 get() 조회 요청을 1번의 Hash Get() 요청으로 묶어서 최적화한다. (Redis Pipelining)
		List<Object> deltas = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
			// hGet()은 바이트 배열(byte[]) 타입을 파라미터로 요구하기 때문에,
			// String 데이터를 새로운 byte[] 데이터로 생성해주는 연산 메서드 getBytes()를 사용해서 파라미터 타입을 일치시킨다. (서로 연관이 없기 때문에 타입 캐스팅이 아님) 
			connection.hashCommands().hGet(viewsDeltaKey.getBytes(StandardCharsets.UTF_8), keyFieldIdentifier.getBytes(StandardCharsets.UTF_8));
			connection.hashCommands().hGet(likesDeltaKey.getBytes(StandardCharsets.UTF_8), keyFieldIdentifier.getBytes(StandardCharsets.UTF_8));
			connection.hashCommands().hGet(dislikesDeltaKey.getBytes(StandardCharsets.UTF_8), keyFieldIdentifier.getBytes(StandardCharsets.UTF_8));
			return null;
		});
				
		long viewsDelta = parseDelta(deltas.get(0));
		long likesDelta = parseDelta(deltas.get(1));
		long dislikesDelta = parseDelta(deltas.get(2));
	*/
		
	/*
	// 3
		// 역할별 Key 3개 대신 게시글별 Key 1개를 생성하고, 조회수/추천수/비추천수를 Hash Key 의 Field 로 두면
		// Hash Get + Redis Pipelining 작업 없이 Hash Multi Get 요청 한 번으로 최적화할 수 있다.
		String deltaKey = "board:delta:" + boardSeq;
		List<Object> fields = List.of("views", "likes", "dislikes");
		
		List<Object> deltasObj = redisTemplate.opsForHash().multiGet(deltaKey, fields);
		
		boolean isDeltasObjNormal = deltasObj != null && deltasObj.size() == fields.size();
		
		long viewsDelta = parseDelta(isDeltasObjNormal ? deltasObj.get(0) : null);
		long likesDelta = parseDelta(isDeltasObjNormal ? deltasObj.get(1) : null);
		long dislikesDelta = parseDelta(isDeltasObjNormal ? deltasObj.get(2) : null);

		
		// 미리 준비한 정적 팩토리 메서드 양식에 맞추어 실시간 조회수/추천수/비추수가 포함된 게시글 정보 반환
		return BoardShowPostResponseDTO.of(boardEntity, viewsDelta, likesDelta, dislikesDelta);
	*/
		
	// 4
		// Scheduler 에서는 board:delta:* Key 구조 대신 board:{metric}:delta Key 구조를 사용하여 Redis 데이터 처리 로직을 구현했다.
		// readPost-3 처럼 게시글 단위의 Key 구조를 사용하는 대신, Scheduler 처럼 기능(Domain) 단위의 Key 구조를 사용하는 것이
		// Redis 가 추구하는 Write-Back 패턴의 목적(안정성 유지, 데이터 유실 방지, 원자적 격리)에 적합하다.
		// 1. rename 작업으로 실시간 적재용 Key / RDB 동기화용 Key 를 격리하여 원자성을 지킬 수 있고, 동시성 이슈가 발생해도 데이터 정합성을 지킬 수 있다.
		// 2. KeySpace 전체에서 board:delta:* 형식으로 통합된 Key 수천 개를 매 번 스캔하는 것보다 목적에 맞는 하나의 Key 를 1회 탐색하는 것이 성능 최적화 면에서 좋다.
		// 3. 장애 발생 시에도 2번 처럼 게시글 단위의 모든 Key 를 찾아가면서 오류 발생 부분을 찾는 것보다 격리된 역할 Key 하나만 체크하는 것이 로직의 단순성 면에서 이득이다.
		// readPost-2의 Hash Get() 1번 + Pipelining 방식을 차용하되, 저수준의 byte[] 코드를 다루는 RedisCallback 대신 SessionCallback을 사용하는 readBoardList-2의 방식을 조합한다.
		String keyFieldIdentifier = String.valueOf(boardSeq);
		
		List<String> metricKeys = List.of("board:views:delta", "board:likes:delta", "board:dislikes:delta");
		
		List<Object> deltasObj = stringRedisTemplate.executePipelined(new SessionCallback<Object>() {
			@Override
			@SuppressWarnings("unchecked")
			public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
				RedisOperations<String, String> redisOperations = (RedisOperations<String, String>) operations;
				
				for (String metricKey : metricKeys) {
					redisOperations.opsForHash().get(metricKey, keyFieldIdentifier);
				}
				return null;
			}
		});
		
		long viewsDelta = parseDelta(deltasObj != null && deltasObj.size() > 0 ? deltasObj.get(0) : null);
		long likesDelta = parseDelta(deltasObj != null && deltasObj.size() > 1 ? deltasObj.get(1) : null);
		long dislikesDelta = parseDelta(deltasObj != null && deltasObj.size() > 2 ? deltasObj.get(2) : null);
		
		return BoardShowPostResponseDTO.of(boardEntity, viewsDelta, likesDelta, dislikesDelta, viewsToken);
	}
	
	/* readPost-2 에서 사용했던 내부 method
	// Object -> String 타입 캐스팅 & String -> long 파싱 작업을 담당하는 내부 method 분리 선언
	private long parseDelta(Object obj) {
		
		// Wrapper 타입인 Object 형 변수에 대한 NULL 처리
		if (obj == null) {
			return 0L;
		}
		// obj 가 byte[] 타입인지 검사
		if (obj instanceof byte[] bytes) { // 별도의 형변환 작업 없이 앞타입 변수를 뒤타입으로 자동 형변환해서 바로 변수로 사용할 수 있게 하는 Pattern Matching 기능 (Java 16+)
			return Long.parseLong(new String(bytes, StandardCharsets.UTF_8));
		}
		
		return Long.parseLong(String.valueOf(obj));
	}
	*/
	
	// readPost-3, readPost-4와 readBoardList에서 사용하는 조회수/추천수/비추천수 증가량 매핑용 내부 method
	private long parseDelta(Object obj) {
		return (obj != null) ? Long.parseLong(String.valueOf(obj)) : 0L;
	}
	
	// increaseViews()에 악의적인 접근 시도를 방지하기 위해 조회수 증가용 임시 토큰을 발행하여 올바른 요청인지 검증하는 데에 필요한 method
	private String increaseViewsVerifyToken(Long boardSeq) {
		
		String token = UUID.randomUUID().toString();
		String redisKey = "board:view-count-token:" + boardSeq + ":" + token;
		
		stringRedisTemplate.opsForValue().set(redisKey, "1", Duration.ofSeconds(30)); // 1은 dummy value
		
		return token;
	}
	
	// 게시글 수정/삭제를 위해 PageRenderController와 BoardAPIController에서 @PreAuthorize를 사용할 때 사용자ID를 가져오는 용도의 method
	@Transactional(readOnly = true)
	public String getUserAccountId(Long boardSeq) {
		
		BoardEntity boardEntity = boardRepository.findById(boardSeq)
				 .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
		
		return boardEntity.getUserEntity().getId();
	}
	
	// 게시글 수정 페이지(PageRenderController.boardDisplayDetailPageForUpdate()) 세팅을 위한 BoardEntity 조회
	@Transactional(readOnly = true)
	public BoardEntity getPostedData(Long boardSeq) {
		
		return boardRepository.findById(boardSeq)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 삭제된 게시글입니다."));
	}
	
	// 게시글 수정
	@Transactional
	public void updatePost(Long boardSeq, @Valid BoardUpdateRequestDTO updateDTO, UserDetailsCustom userDetails) {
		
		BoardEntity boardEntity = boardRepository.findById(boardSeq)
				 .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
		
		String loginUserId = userDetails.getUsername();
		String postUserId = boardEntity.getUserEntity().getId();
		// 기존 방식: Collection<? extends GrantedAuthority> 형태의 권한 목록(안에 든 권한이 여러가지)을 advanced for + if 문으로 검사
		// 보완 방식: 권한 목록을 stream().anyMatch()로 검사하여 조건을 만족하는 값이 하나라도 있을 경우 TRUE를 반환하고 탐색 종료
		// 다른 방식: AuthorityUtils 라이브러리의 authorityListToSet(userDetails.getAuthorities()).contains("ROLE_ADMIN") 로 검사
		boolean isAdmin = userDetails.getAuthorities().stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
		
		// API 요청자가 작성자일 경우
		if (loginUserId.equals(postUserId)) {
			// 작성자가 맞지만 계정 상태가 정상적이지 않을 경우
			if (boardEntity.getUserEntity().getUserStatus() == UStat.NORMAL) {
				boardEntity.updatePost(updateDTO.getCategory(), updateDTO.getTitle(), updateDTO.getContent());
			} else {
				throw new IllegalArgumentException("올바른 계정 상태가 아닙니다.");
			}
		} else if (isAdmin) { // 관리자일 경우
			boardEntity.updatePost(updateDTO.getCategory(), updateDTO.getTitle(), updateDTO.getContent());
		} else { // API 요청자가 작성자도 아니고 관리자도 아닐 경우
			throw new AccessDeniedException("글 수정 권한이 없습니다.");
		}
	}
	
	// 게시글 삭제
	@Transactional
	public void deletePost(Long boardSeq, UserDetailsCustom userDetails) {
		
		BoardEntity boardEntity = boardRepository.findById(boardSeq)
												 .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
		
		String loginUserId = userDetails.getUsername();
		String postUserId = boardEntity.getUserEntity().getId();
		boolean isAdmin = userDetails.getAuthorities().stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
		
		if (loginUserId.equals(postUserId)) {
			if (boardEntity.getUserEntity().getUserStatus() == UStat.NORMAL) {
				boardRepository.delete(boardEntity);
			} else {
				throw new IllegalArgumentException("올바른 계정 상태가 아닙니다.");
			}
		} else if (isAdmin) {
			boardRepository.delete(boardEntity);
		} else {
			throw new AccessDeniedException("글 삭제 권한이 없습니다.");
		}
	}
	
	// 댓글 목록 조회
	@Transactional(readOnly = true)
	public Page<ReplyShowListResponseDTO> readReplyList(Long boardSeq, Pageable pageable) {
		
		Page<ReplyEntity> replyEntityPage = replyRepository.findByBoardSeqWithUser(boardSeq, pageable);
		
		return replyEntityPage.map(ReplyShowListResponseDTO::of);
	}

	// 댓글 등록
	@Transactional
	public void createReply(Long boardSeq, @Valid ReplyCreateRequestDTO replyDTO, Long userSeq) {
		
		UserAccountEntity userEntity = userRepository.findById(userSeq)
				 .orElseThrow(() -> new UserNotFoundException("사용자 정보를 찾을 수 없습니다."));
		
		BoardEntity boardEntity = boardRepository.findById(boardSeq)
				 .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
		
		if (userEntity.getUserStatus() != UStat.NORMAL) {
			throw new IllegalArgumentException("올바른 계정 상태가 아닙니다.");
		}
		
		ReplyEntity replyEntity = ReplyEntity.builder().boardEntity(boardEntity)
													   .userEntity(userEntity)
													   .content(replyDTO.getContent())
													   .build();
		
		replyRepository.save(replyEntity);
		
		// 게시글의 댓글 수(replyCount) 증가
		boardRepository.increaseReplyCount(boardSeq);
	}
	
	// 댓글 삭제를 위해 BoardAPIController에서 @PreAuthorize를 사용할 때 사용자ID를 가져오는 용도의 method
	@Transactional(readOnly = true)
	public String getUserAccountIdForReply(Long boardSeq, Long replySeq) {
		
		ReplyEntity replyEntity = replyRepository.findById(replySeq)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));
		
		if (!replyEntity.getBoardEntity().getBoardSeq().equals(boardSeq)) {
			throw new IllegalArgumentException("요청이 올바르지 않습니다. 해당 게시글의 댓글이 아닙니다.");
		}
		
		return replyEntity.getUserEntity().getId();
	}

	// 댓글 삭제
	@Transactional
	public void deleteReply(Long boardSeq, Long replySeq, UserDetailsCustom userDetails) {
		
		ReplyEntity replyEntity = replyRepository.findById(replySeq)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));
		
		String loginUserId = userDetails.getUsername();
		String replyUserId = replyEntity.getUserEntity().getId();
		boolean isAdmin = userDetails.getAuthorities().stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
		
		// 실제로 존재하는 댓글 번호이지만 연계된 게시글번호에 엉뚱한 값을 넣어서 낚시성 요청을 하는 경우
		if (!replyEntity.getBoardEntity().getBoardSeq().equals(boardSeq)) {
			throw new IllegalArgumentException("요청이 올바르지 않습니다. 해당 게시글의 댓글이 아닙니다.");
		}
		
		if (loginUserId.equals(replyUserId)) {
			if (replyEntity.getUserEntity().getUserStatus() == UStat.NORMAL) {
				replyRepository.delete(replyEntity);				
				// 게시글의 댓글 수(replyCount) 감소
				boardRepository.decreaseReplyCount(boardSeq);
				/* decreaseReplyCount()는 @Modifying(clearAutomatically = true)가 붙은 query method 이다.
				 * @Modifying 이 붙은 method 들은 BoardRepository의 주석처럼 벌크 연산(한꺼번에 처리) method 가 되고, clearAutomatically=true 에 의해 자동으로 영속성 컨텍스트가 비워진다.
				 * Hibernate 의 기본 flush 최적화 전략은 Flushmode.AUTO로, DB로 flush 할 query 의 대상 테이블이 메모리에 대기 중인 테이블과 같은지 확인한다.
				 * 1. replyRepository.delete()로 REPLY 테이블의 삭제 요청이 메모리(Action Queue)에 등록된다.
				 * 2. flush 직전에, 다음 요청인 boardRepository.decreaseReplyCount()로 영향받는 테이블이 delete()의 테이블과 같은지 확인한다.
				 * 3. 서로 같은 테이블이라면 delete()를 먼저 flush 하고 decreaseReplyCount()를 이어서 flush 했을 것이다.
				 * 4. 하지만 서로 다른 테이블(REPLY/BOARD)이었기 때문에 Hibernate 는 delete()를 일단 메모리에 두고 다음 요청인 decreaseReplyCount()가 어떤 method 인지 확인했다.
				 * 5. decreaseReplyCount()는 @Modifying이 붙은 벌크 연산 Query 이기 때문에, JPA가 내부적으로 commit 시점에 실행되도록 예약해놓은 일반 Query 인 delete()와 달리 호출되는 코드 라인에서 즉시 flush 됐다.
				 * 6. 이 때, decreaseReplyCount()의 clearAutomatically = true 옵션으로 인해 예약된 delete()의 정보가 들어있던 영속성 컨텍스트 데이터가 삭제됐다.
				 * 7. method 개별로는 문제가 없었지만, 두 코드가 상호작용한 결과 한 쪽이 실행되지 않은 것이다.
				 * 
				 * BoardService.deleteReply()의 경우는 한 쪽만 영속성 컨텍스트 삭제 옵션이 붙은 벌크 쿼리이기 때문에, 두 method 의 실행 순서가 반대였다면 정상적으로 실행됐을 것이다.
				 * 여기까지 이해가 됐다면... 동일한 방식으로 코딩된 BoardService.createReply()의 save() -> increaseReplyCount() 에서는 왜 이 문제가 발생하지 않았을까?
				 * 그것은 Entity 의 PK 데이터가 자동으로 생성되도록 하는 @GeneratedValue(strategy = GenerationType.IDENTITY) 방식을 선택했기 때문이다.
				 * PK 번호가 필요 없는 delete()와 달리 save()는 PK 번호가 반드시 필요하다.
				 * 따라서 Query 를 메모리(Action Queue)에 쓰기 지연 방식으로 달아두지 못 하고 바로 flush 하여 생성된 PK 번호를 받아와야 한다.
				 * save()가 이미 DB에 flush 되었기 때문에, increaseReplyCount()가 영속성 컨텍스트 데이터를 없애도 정상적인 결과가 나온다.
				 * (만약 PK 데이터 생성 방식이 IDENTITY가 아니라 SEQUENCE 였다면 오류가 발생했을 것이다.)
				 */
			} else {
				throw new IllegalArgumentException("올바른 계정 상태가 아닙니다.");
			}
		} else if (isAdmin) {
			replyRepository.delete(replyEntity);				
			// 게시글의 댓글 수(replyCount) 감소
			boardRepository.decreaseReplyCount(boardSeq);
		} else {
			throw new AccessDeniedException("댓글 삭제 권한이 없습니다.");
		}
	}

}
