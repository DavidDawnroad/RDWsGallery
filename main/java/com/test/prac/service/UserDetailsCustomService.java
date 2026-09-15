package com.test.prac.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.test.prac.config.UserDetailsCustom;
import com.test.prac.dto.SessionDTO;
import com.test.prac.entity.UserAccountEntity;
import com.test.prac.repository.UserAccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserDetailsCustomService implements UserDetailsService {

	private final UserAccountRepository userAccountRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

		UserAccountEntity userEntity = userAccountRepository.findByUserAccountId(username)
				.orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 계정입니다."));

		SessionDTO sessionDTO = SessionDTO.from(userEntity);
		
		return new UserDetailsCustom(sessionDTO);
	}
}
