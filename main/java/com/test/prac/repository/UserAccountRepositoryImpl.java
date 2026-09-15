package com.test.prac.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.test.prac.dto.ManageShowUserListResponseDTO;
import com.test.prac.entity.QUserAccountEntity;
import com.test.prac.entity.UserAccountEntity;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserAccountRepositoryImpl implements UserAccountRepositoryCustom {

	private final JPAQueryFactory jpaQueryFactory;

	@Override // 메서드명 오타로 인한 컴파일 오류를 방지하기 위해 '인터페이스 파일에 선언된 그 메서드의 구현체다' 를 명시
	public Optional<UserAccountEntity> findByUserAccountId(String id) {


		/* 파라미터명이 너무 길어서 가독성이 구리다면?
		 * QUserAccountEntity.userAccountEntity 에 대한 객체를 따로 선언해서
		 * Query DSL 안에 들어갈 파라미터들의 길이를 줄일 수도 있다
		 */
		// QUserAccountEntity user = QUserAccountEntity.userAccountEntity;


		return Optional.ofNullable(jpaQueryFactory.selectFrom(QUserAccountEntity.userAccountEntity)
												  .where(QUserAccountEntity.userAccountEntity.id.eq(id))
												  .fetchOne()); // 값이 없으면 빈 Optional return
	}

	@Override
	public Optional<UserAccountEntity> findByUserAccountIdAndEmail(String id, String email) {

		QUserAccountEntity entity = QUserAccountEntity.userAccountEntity;

		return Optional.ofNullable(jpaQueryFactory.selectFrom(entity)
												  .where(entity.id.eq(id)
														  .and(entity.email.eq(email)))
												  .fetchOne());
	}

	@Override
	public boolean existsByUserAccountId(String id) {

		QUserAccountEntity entity = QUserAccountEntity.userAccountEntity;

		return jpaQueryFactory.selectOne()
							  .from(entity)
							  .where(entity.id.eq(id))
							  .fetchFirst() != null;
		/* 이 query 에서 fetchFirst()의 결과가 있으면 그 타입은 UserAccountEntity이고, 없으면 null 이다.
		 * boolean 형의 결과를 얻기 위해서 fetchFirst()에 IS NOT NULL 조건을 추가했고 결과가 있으면 true, 없으면 false 를 반환하게 되었다.
		 */
	}
	
	@Override
	public Page<ManageShowUserListResponseDTO> findDatasForDisplayUserList(Pageable pageable) {
		
		QUserAccountEntity userEntity = QUserAccountEntity.userAccountEntity;
		
		List<ManageShowUserListResponseDTO> content = jpaQueryFactory.select(
				// selectFrom() 등으로 모든 필드를 가져오지 않고 가져오고 싶은 필드를 직접 나열할 경우, Query DSL은 내부적으로 Tuple 타입의 데이터를 반환한다.
				// Query DSL은 Tuple 타입 데이터를 개발자가 원하는 DTO로 직접 매핑해주지 않기 때문에, Projections.constructor()로 매핑 대상을 직접 명시해야 한다.
																			 Projections.constructor(ManageShowUserListResponseDTO.class,
																					 				 userEntity.id,
																					 				 userEntity.nickname,
																					 				 userEntity.userStatus
																					 				 )
																			)
																	 .from(userEntity)
																	 .offset(pageable.getOffset())
																	 .limit(pageable.getPageSize())
																	 .orderBy(userEntity.seq.desc())
																	 .fetch();
		
		JPAQuery<Long> pageCount = jpaQueryFactory.select(userEntity.count())
												  .from(userEntity);
		
		return PageableExecutionUtils.getPage(content, pageable, pageCount::fetchOne);
	}
}
