package com.test.prac.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.test.prac.entity.ReplyEntity;

@Repository
public interface ReplyRepository extends JpaRepository<ReplyEntity, Long>, ReplyRepositoryCustom {
	
}
