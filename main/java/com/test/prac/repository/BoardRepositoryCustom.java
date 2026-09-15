package com.test.prac.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.test.prac.entity.BoardEntity;

public interface BoardRepositoryCustom {

	Page<BoardEntity> findAllWithUser(Pageable pageable);
}
