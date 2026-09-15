package com.test.prac.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.test.prac.entity.BoardEntity;

@Repository
public interface BoardRepository extends JpaRepository<BoardEntity, Long>, BoardRepositoryCustom {

	/* increaseViews를 Entity Setter 로 구현하게 되면 동시성 이슈(Race Condition)로 인한 데이터 손실 발생의 가능성이 있다.
	 * 이를 방지하기 위해 @Modifying이 추가된 JPQL Atomic Query 방식을 사용했다.
	 * @Query는 JPA에서 해당 Query 를 기본적으로 SELECT(executeQuery())로 인식하게 만든다.
	 * @Modifying이 붙으면 JPA가 해당 Query 를 UPDATE 혹은 DELETE 용도(executeUpdate())의 벌크 연산(Bulk Operation) Query 로 인식한다.
	 * 벌크 연산은 JPA의 일반적인 데이터 수정 방식인 변경 감지(Dirty Checking)가 아니라 대량 변경 연산의 개념이다.
	 * 변경 감지 방식은 1차 캐시인 영속성 컨텍스트의 변경을 감지하는 식으로 이루어지고, 단건 처리 방식이기 때문에 10만 개의 데이터를 변경해야 한다면 UPDATE Query 를 10만 번 execute 해야 한다.
	 * 반면 벌크 연산은 Query 가 영속성 컨텍스트를 거치지 않고 바로 DB로 execute 되기 때문에 한 번의 Query 로 10만 개의 데이터를 변경할 수 있다.
	 * 다만 영속성 컨텍스트(서버 메모리)를 거치지 않는다는 점에서 일반 조회-벌크 연산-일반 조회 시 DB 데이터와 영속성 컨텍스트의 데이터가 일치하지 않아 뒤의 조회에서 연산 이전 값을 반환하는 오류가 발생할 수 있다.
	 * 따라서 벌크 연산 수행 후에는 반드시 영속성 컨텍스트를 비워야 하는데, 그 작업을 @Modifying의 clearAutomatically 옵션이 담당한다.
	 * (Query DSL 사용 시에는 Query 이후에 EntityManager.clear()를 수동으로 호출해서 영속성 컨텍스트를 비워야 한다.)
	 * 다시 돌아와서, @Modifying은 데이터베이스 Locking 현상의 직접적인 원인은 아니다. Locking 을 직접 걸고 싶은 Query 에는 @Lock(LockModeType)을 사용한다.
	 * 하지만 UPDATE/DELETE 명령어가 실행되면 DB 자체에서 트랜잭션의 ACID 원칙 중 일관성(atomicity)과 독립성(isolation)을 지키기 위해 모든 행에 배타적 락(Exclusive Lock, X-Lock)을 건다. 
	 * 동시성 제어를 위해 데이터베이스에서 Locking 메커니즘이 작동하여 UPDATE 요청들이 직렬화(Serialization, 겹치지 않고 순차 실행)되는 것이다.
	 */
//	@Modifying(clearAutomatically = true)
//	@Query("UPDATE BoardEntity b SET b.views = b.views + 1 WHERE b.boardSeq = :boardSeq")
//	int increaseViews(@Param("boardSeq") Long boardSeq);
	
	/* increaseViews()에서 사용했던 SET b.views = b.views + 1 구문은 상대적 업데이트(Relative Update) Query 이다.
	 * Java 서버는 DB 값을 정확히 모르는 상태이고, DB 값에 1을 더하는 방식.
	 * 
	 * updateViews()에서 사용하는 SET b.views = :views 구문은 절대적 업데이트(Absolute Update) Query 이다.
	 * Java 서버가 DB 값을 알든 모르든, 서버가 가지고 있는 views 값으로 DB 값을 Overwrite 하는 방식.
	 */
//	@Modifying(clearAutomatically = true)
//	@Query("UPDATE BoardEntity b SET b.views = :views WHERE b.boardSeq = :boardSeq")
//	int updateViews(@Param("views") Long views, @Param("boardSeq") Long boardSeq);
	
	/* RedisViewCountScheduler.java에 작성된 여러가지 이슈를 해결하기 위해 조회수 증가 쿼리를 증분 적재 방식으로 변경했다.
	 * 값을 덮어쓰는 대신 기존 값에 증가량을 더하는 Query 이다.
	 */
	
	/* clearAutomatically = true 는 DB로 flush 되지 않고 대기 중인 작업을 유실시킬 수 있는 위험성이 있기 때문에,
	 * 코드 호출과 동시에 즉시 flush 되는 벌크 연산의 실행 전에 대기 중인 Query 를 DB로 먼저 밀어 넣도록 flushAutomatically = true 옵션을 같이 설정해야 안전하다.
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE BoardEntity b SET b.views = b.views + :delta WHERE b.boardSeq = :boardSeq")
	int updateViews(@Param("delta")Long delta, @Param("boardSeq") Long boardSeq);
	
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE BoardEntity b SET b.likes = b.likes + :delta WHERE b.boardSeq = :boardSeq")
	int increaseLikes(@Param("delta")Long delta, @Param("boardSeq") Long boardSeq);
	
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE BoardEntity b SET b.dislikes = b.dislikes + :delta WHERE b.boardSeq = :boardSeq")
	int increaseDislikes(@Param("delta")Long delta, @Param("boardSeq") Long boardSeq);
	
	// Query 로 인해 영향을 받은 행(Row)의 개수를 반환한다. 특정 값을 반환함이 아님에 주의
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE BoardEntity b SET b.replyCount = b.replyCount + 1 WHERE b.boardSeq = :boardSeq")
	int increaseReplyCount(@Param("boardSeq") Long boardSeq);
	
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE BoardEntity b SET b.replyCount = b.replyCount - 1 WHERE b.boardSeq = :boardSeq AND b.replyCount > 0")
	int decreaseReplyCount(@Param("boardSeq") Long boardSeq);
	
	List<BoardEntity> findByUserEntity_SeqOrderByRegdateDesc(Long userSeq, Pageable pageable);
}
