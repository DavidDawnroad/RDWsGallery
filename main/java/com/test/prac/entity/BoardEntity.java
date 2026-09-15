package com.test.prac.entity;

import com.test.prac.enums.Category;
import com.test.prac.enums.CategoryConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Getter
@Table(name = "BOARD")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardEntity extends BaseEntity {

	@Id
	@Column(name = "BOARD_SEQ")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long boardSeq;

//	private Long userSeq;
	// userSeq 선언 대신 UserAccountEntity를 직접 참조하여 FK 연관관계 구축
	// Many(BoardEntity) To One(UserAccountEntity) -> 회원(1):게시글(N) 관계
	@ManyToOne(fetch = FetchType.LAZY) // PageRenderController에서 findAll()을 사용할 때 UserAccountEntity가 중복조회되지 않도록 지연 로딩 옵션 추가 -> 성능 저하 방지
	@JoinColumn(name = "USER_SEQ", nullable = false)
	private UserAccountEntity userEntity;

	@Convert(converter = CategoryConverter.class)
	@Column(name = "BOARD_CATEGORY", columnDefinition = "NUMBER", nullable = false)
	private Category category = Category.General;

	@Column(name="BOARD_TITLE")
	private String title;

	@Column(name="BOARD_CONTENT")
	@Lob // CLOB 매핑
	private String content;

	// 정수형 값을 취급하는 필드임에도 Long 형을 사용하는 이유는 정수 오버플로우(2147483647) 문제를 방지하기 위함
	@Column(name = "BOARD_VIEWS", nullable = false)
	private Long views = 0L;

	@Column(name = "BOARD_LIKES", nullable = false)
	private Long likes = 0L;
	
	@Column(name = "BOARD_DISLIKES", nullable = false)
	private Long dislikes = 0L;
	
	// 더 간단한 방법은 COUNT(*)를 사용하는 것이지만, 게시글 데이터와 댓글 데이터가 BoardEntity와 ReplyEntity로 나뉜 상황에서
	// JOIN과 GROUP BY 연산을 매 번 시행해야 한다면 CPU 리소스 사용량이 매우 많이 늘어날 것이다. (시간 복잡도 O(N) 혹은 인덱스 사용 시 O(logN))
	// 대용량 트래픽 환경에서도 안정적인 시스템 성능을 챙기기 위해 반정규화(De-normailzation) 방법을 선택했다.
	// 반정규화로 Entity 와 DTO에 Column 하나만 미리 만들어두면 Table 을 한 번에 하나만 조회하면 된다.
	// 쓰기 작업(replyCount가 변하는 작업)이 조회처럼 매우 빈번하게 일어나서
	// Row-Lock 대기시간이 유의미하게 길어지거나 CPU 리소스 사용량이 한계에 달하는 상황이라면 Redis 카운터를 사용해야 하겠지만,
	// Cache 데이터의 정합성, 장애 시 추가적인 데이터 복구 로직, 동기화 배치 전략, 작업 오류 시 분산 트랜잭션 상황 발생 등 운영적 부담이 늘어나기 때문에
	// 경제성과 안정성을 적절히 조율하는 것이 실무 환경에서 현실적이라고 볼 수 있겠다.
	@Column(name = "BOARD_REPLY_COUNT", nullable = false)
	private Long replyCount = 0L;

	@Builder
	public BoardEntity(UserAccountEntity userEntity, Category category, String title, String content) {
		this.userEntity = userEntity;
		this.category = (category != null) ? category : Category.General;
		this.title = title;
		this.content = content;
	}
	
	// Method 로 Setter 구현 (moddate 는 @LastModifiedDate로 수정 시각이 자동 기록되게 설정했기 때문에 파라미터로 넣을 필요 없음)
	public void updatePost(Category category, String title, String content) {
		this.category = category;
		this.title = title;
		this.content = content;
	}
	
	/* Dirty Checking 방식을 사용했을 때 실무 환경에서 10명이 동시에 게시글을 클릭하는 상황이 발생했다고 가정해보자.
	 * 원래 의도대로라면 조회수가 110이 되어야 하지만 10명 각각의 메모리에서 100 + 1 = 101 을 수행하면서 views 가 101로 덮어씌워지는 데이터 손실이 발생한다.
	 * 이를 동시성 이슈(Race Condition)로 인한 데이터 손실이라고 한다.
	 * 조회수 증가 로직은 구조가 단순한 편에 속하기 때문에, 굳이 Query DSL 방식을 고집하기 보다는 JPQL Atomic Query + @Modifying 방식을 사용하는 것이 경제적이다.
	public void increaseViews() {
		this.views += 1L;
	}
	*/
}
