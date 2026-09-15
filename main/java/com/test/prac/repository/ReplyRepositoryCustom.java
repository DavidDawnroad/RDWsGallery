package com.test.prac.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.test.prac.entity.ReplyEntity;

public interface ReplyRepositoryCustom {
	
	Page<ReplyEntity> findByBoardSeqWithUser(Long boardSeq, Pageable pageable);
	
	List<ReplyEntity> findReplyDatasWithBoardTitleByUserSeq(Long userSeq, Pageable pageable);

}
