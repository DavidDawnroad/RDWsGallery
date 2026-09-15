package com.test.prac.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.test.prac.entity.UserAccountEntity;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccountEntity, Long>, UserAccountRepositoryCustom { // 다중 상속

	boolean existsByNickname(String nickname);
}
