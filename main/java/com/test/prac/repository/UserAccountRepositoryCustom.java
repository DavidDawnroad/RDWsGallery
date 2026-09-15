package com.test.prac.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.test.prac.dto.ManageShowUserListResponseDTO;
import com.test.prac.entity.UserAccountEntity;

public interface UserAccountRepositoryCustom {

	Optional<UserAccountEntity> findByUserAccountId(String id);

	Optional<UserAccountEntity> findByUserAccountIdAndEmail(String id, String email);

	boolean existsByUserAccountId(String id);

	Page<ManageShowUserListResponseDTO> findDatasForDisplayUserList(Pageable pageable);
}
