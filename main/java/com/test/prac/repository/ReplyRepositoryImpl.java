package com.test.prac.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.test.prac.entity.QBoardEntity;
import com.test.prac.entity.QReplyEntity;
import com.test.prac.entity.QUserAccountEntity;
import com.test.prac.entity.ReplyEntity;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ReplyRepositoryImpl implements ReplyRepositoryCustom {

	private final JPAQueryFactory jpaQueryFactory;
	
	@Override
	public Page<ReplyEntity> findByBoardSeqWithUser(Long boardSeq, Pageable pageable) {
		
		QUserAccountEntity userEntity = QUserAccountEntity.userAccountEntity;
		QReplyEntity replyEntity = QReplyEntity.replyEntity;
		
		List<ReplyEntity> content = jpaQueryFactory.selectFrom(replyEntity)
							  					   .innerJoin(replyEntity.userEntity, userEntity).fetchJoin()
							  					   .where(replyEntity.boardEntity.boardSeq.eq(boardSeq)) // 연관된 Entity 의 PK 값을 비교/조회할 때는 JPA가 굳이 Join 을 맺지 않는다
							  					   .orderBy(replyEntity.replySeq.asc())
							  					   .offset(pageable.getOffset())
							  					   .limit(pageable.getPageSize())
							  					   .fetch();
		
		JPAQuery<Long> pageCount = jpaQueryFactory.select(replyEntity.count())
												  .from(replyEntity)
												  .where(replyEntity.boardEntity.boardSeq.eq(boardSeq));
		
		return PageableExecutionUtils.getPage(content, pageable, pageCount::fetchOne);
	}
	
	@Override
	public List<ReplyEntity> findReplyDatasWithBoardTitleByUserSeq(Long userSeq, Pageable pageable) {
		
		QBoardEntity boardEntity = QBoardEntity.boardEntity;
		QReplyEntity replyEntity = QReplyEntity.replyEntity;
		
		return jpaQueryFactory.selectFrom(replyEntity)
				.join(replyEntity.boardEntity, boardEntity).fetchJoin()
				.where(replyEntity.userEntity.seq.eq(userSeq))
				.orderBy(replyEntity.regdate.desc())
				.offset(pageable.getOffset())
				.limit(pageable.getPageSize())
				.fetch();
	}
}
