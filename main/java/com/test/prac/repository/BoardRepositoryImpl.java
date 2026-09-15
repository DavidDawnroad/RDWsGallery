package com.test.prac.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.test.prac.entity.BoardEntity;
import com.test.prac.entity.QBoardEntity;
import com.test.prac.entity.QUserAccountEntity;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BoardRepositoryImpl implements BoardRepositoryCustom {

	private final JPAQueryFactory jpaQueryFactory;

	@Override
	public Page<BoardEntity> findAllWithUser(Pageable pageable) {

		QBoardEntity boardEntity = QBoardEntity.boardEntity;
		QUserAccountEntity userEntity = QUserAccountEntity.userAccountEntity;

		/* - 일반 Join 만 시도했을 경우
		 * DB에서 BOARD 테이블의 데이터를 가져와서 BoardEntity만 영속화
		 * 영속화: 영속성 컨텍스트에 등록(JPA가 엔티티를 관리대상으로 인식하게 함)하는 행위
		 * SELECT 절에 BoardEntity의 필드만 있는 상태고, UserAccountEntity는 BoardEntity에서 지연 로딩(LAZY)이 설정되었기 때문에 userEntity 객체가 없음
		 * BoardEntity의 구성에 UserAccountEntity의 데이터가 당장 필요하지는 않지만 userEntity 객체 자체는 필요함
		 * JPA는 진짜 유저 객체 대신 프록시 객체(가짜 객체)를 만들어서 BoardEntity에 끼워넣고 영속화를 시도함
		 * 이후 절차에서 프론트엔드 화면이 구성되면서 ThymeLeaf가 ForEach로 board.getUserEntity.getNickname을 계속해서 호출하면
		 * UserAccountEntity의 진짜 데이터가 필요해진 상황이 되었기 때문에 JPA가 프록시 객체 자리에 진짜 데이터를 넣기 위해서 UserAccountEntity에 추가 Query 를 execute 함
		 * -> 게시글 수만큼 nickname 을 불러와야 하기 때문에 N+1 문제가 발생
		 * 
		 * - Fetch Join 을 같이 시도했을 경우
		 * DB에서 BOARD 테이블과 연관관계인 USER_ACCOUNT 테이블의 모든 데이터까지 전부 가져와서 BoardEntity와 UserAccountEntity를 전부 영속화
		 * 프론트엔드에서 getNickname을 호출하는 시점에서 BoardEntity의 userEntity 필드가 프록시 객체가 아닌 진짜 객체로 구성되어 있기 때문에 추가 Query 불필요
		 * -> 맨 처음의 Query 로 모든 문제가 해결되기 때문에 N+1 문제가 발생하지 않음
		 */
		List<BoardEntity> content = jpaQueryFactory.selectFrom(boardEntity)
				 // innerJoin의 두 번째 파라미터를 명시하면 이후 쿼리에서 where() 등의 조건을 사용할 때 해당 인자를 직접 활용할 수 있어 확장성이 좋아짐
												   .innerJoin(boardEntity.userEntity, userEntity).fetchJoin()
												   .offset(pageable.getOffset())
									               .limit(pageable.getPageSize())
									               .orderBy(boardEntity.boardSeq.desc())
									               .fetch();
		
		// 성능 향상을 위해 pageCount를 구하는 Query 분리 작성
		JPAQuery<Long> pageCount = jpaQueryFactory.select(boardEntity.count())
												  .from(boardEntity);
		
		// 첫 페이지나 마지막 페이지일 경우 불필요한 pageCount Query 생략을 위해 PageableExecutionUtils.getPage() 사용
		return PageableExecutionUtils.getPage(content, pageable, pageCount::fetchOne);
	}

}