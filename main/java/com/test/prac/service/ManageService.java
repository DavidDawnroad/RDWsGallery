package com.test.prac.service;

import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.test.prac.dto.ManageChangeUserStatusRequestDTO;
import com.test.prac.dto.ManageShowUserDataResponseDTO;
import com.test.prac.dto.ManageShowUserListResponseDTO;
import com.test.prac.entity.BoardEntity;
import com.test.prac.entity.ReplyEntity;
import com.test.prac.entity.UserAccountEntity;
import com.test.prac.enums.UStat;
import com.test.prac.exception.NicknameDuplicatedException;
import com.test.prac.exception.NicknameNotChangedException;
import com.test.prac.exception.UserNotFoundException;
import com.test.prac.repository.BoardRepository;
import com.test.prac.repository.ReplyRepository;
import com.test.prac.repository.UserAccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ManageService {

	private final UserAccountRepository userAccountRepository;
	private final BoardRepository boardRepository;
	private final ReplyRepository replyRepository;
	// Session Redis DI
	private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;
	
	@Transactional(readOnly = true)
	public ManageShowUserDataResponseDTO getManageDatas(String userAccountId) {
		
		UserAccountEntity userEntity = userAccountRepository.findByUserAccountId(userAccountId)
										.orElseThrow(() -> new UserNotFoundException("존재하지 않는 사용자입니다."));
		
		// Paging UI를 만들지 않을거라 Controller 영역이 아니라 Service 영역에서 Paging 처리
		int datasByPage = 10;
		Pageable pageable = PageRequest.of(0, datasByPage);
		List<BoardEntity> boardDatas = boardRepository.findByUserEntity_SeqOrderByRegdateDesc(userEntity.getSeq(), pageable);
		List<ReplyEntity> replyDatas = replyRepository.findReplyDatasWithBoardTitleByUserSeq(userEntity.getSeq(), pageable);
		
		ManageShowUserDataResponseDTO.UserData userData = ManageShowUserDataResponseDTO.UserData.from(userEntity);
		List<ManageShowUserDataResponseDTO.BoardData> boardData = boardDatas.stream().map(ManageShowUserDataResponseDTO.BoardData::from).toList();
		List<ManageShowUserDataResponseDTO.ReplyData> replyData = replyDatas.stream().map(ManageShowUserDataResponseDTO.ReplyData::from).toList();
		
		return new ManageShowUserDataResponseDTO(userData, boardData, replyData);
	}
	
	// 본인닉네임변경/관리자닉네임변경 method 에 사용할 내부 method
	private String updateNickname(UserAccountEntity userEntity, String newNickname) {
		
		if (userEntity.getNickname().equals(newNickname)) {
			throw new NicknameNotChangedException("동일한 닉네임으로는 변경할 수 없습니다.");
		}
		
		if (userAccountRepository.existsByNickname(newNickname)) {
			throw new NicknameDuplicatedException("이미 사용 중인 닉네임입니다.");
		}

		try {
			// Entity Setter 사용
			userEntity.changeNickname(newNickname);
			
			// DataIntegrityViolationException을 바로 Catch 하기 위해 트랜잭션 종료까지 기다리지 않고 Query 를 즉시 RDB에 반영시키는 saveAndFlush() 사용
			userAccountRepository.saveAndFlush(userEntity);
			
			return newNickname;
		} catch (DataIntegrityViolationException e) {
			// 두 명이 동시에 동일한 닉네임으로 변경하는 경우
			// existsByNickname()은 둘 다 통과할 수 있지만, saveAndFlush()에서 후자의 시도가 Unique 제약 조건에 위반되기 때문에 첫 번째 유저만 닉네임이 변경된다.
			throw new NicknameDuplicatedException("이미 사용 중인 닉네임입니다.");
		}
	}
	
	@Transactional
	public String changeMyNickname(Long seq, String newNickname) {
		
		/* method 하나에 RDB Access 가 3회 수행되는 것은 Connection Pool 을 낭비하는 행위일 수 있지만,
		 * RDB Access 횟수를 줄이겠다고 Long seq 대신 userDetails의 정보를 파라미터로 직접 가져다 사용하게 되면 데이터 정합성이 깨진다.
		 * 260723 기준 Spring Security 의 설정은 로그인 데이터가 HTTPSession에 저장되는 방식인데,
		 * ( filter.setSecurityContextRepository(new HttpSessionSecurityContextRepository()); )
		 * 로그인 사용자가 실시간으로 관리자에 의해 BAN 처리가 되어도 현재 로그인이 풀리지 않는 이상 계속해서 활동할 수 있는 문제가 발생하기 때문이다.
		*/
		UserAccountEntity userEntity = userAccountRepository.findById(seq)
										.orElseThrow(() -> new UserNotFoundException("존재하지 않는 사용자입니다."));
		
		if (userEntity.getUserStatus() == UStat.BANNED || userEntity.getUserStatus() == UStat.TERMINATED) {
			throw new IllegalArgumentException("올바른 계정 상태가 아닙니다.");
		}
		
		return updateNickname(userEntity, newNickname);
	}
	@Transactional
	public String changeNicknameByAdmin(String userAccountId, String newNickname) {
		
		UserAccountEntity userEntity = userAccountRepository.findByUserAccountId(userAccountId)
										.orElseThrow(() -> new UserNotFoundException("가입 이력이 없거나 개인정보 보관 기한이 지난 사용자입니다."));
		
		if (userEntity.getUserStatus() == UStat.BANNED || userEntity.getUserStatus() == UStat.TERMINATED) {
			throw new IllegalArgumentException("이미 정지처리된 사용자거나 계정을 삭제한 사용자입니다.");
		}
		
		return updateNickname(userEntity, newNickname);
	}
	
	@Transactional
	public void changeUserStatusByAdmin(String userAccountId, ManageChangeUserStatusRequestDTO uStatDTO) {
		
		UserAccountEntity userEntity = userAccountRepository.findByUserAccountId(userAccountId)
				.orElseThrow(() -> new UserNotFoundException("가입 이력이 없거나 개인정보 보관 기한이 지난 사용자입니다."));
		
		UStat newUserStatus = uStatDTO.getUserStatus();
		
		if (newUserStatus == null) {
			throw new IllegalArgumentException("유저의 상태값이 정상적이지 않습니다.");
		}
		// 이전 상태값과 변경 상태값이 동일하지 않은 경우에만 UPDATE 와 Session 삭제(강제 로그아웃) 진행
		if (userEntity.getUserStatus() != newUserStatus) {
			
			userEntity.changeStatus(newUserStatus);
			
			// 사용자(Principal, 즉 UserDetailsCustom) 기반 Session 역조회
			Map<String, ? extends Session> userSessions = sessionRepository.findByPrincipalName(userAccountId);
			
			// CollectionUtils == Collection 형 객체(List, Set)와 Map 형 객체(ResultSet, ResultMap)의 NULL & 비어있음 검사를 한 번에 처리할 수 있는 Spring Framework 내장 라이브러리
			if (!CollectionUtils.isEmpty(userSessions)) { // 유저가 로그인 중이라서 세션이 존재하는 경우 (isEmpty() == false)
	            for (String sessionId : userSessions.keySet()) {
	                sessionRepository.deleteById(sessionId); // 해당 유저의 Redis Session 삭제
	            }
	        }
		}
	}
	
	@Transactional(readOnly = true)
	public Page<ManageShowUserListResponseDTO> getUserList(Pageable pageable) {
		
		Page<ManageShowUserListResponseDTO> userEntityPage = userAccountRepository.findDatasForDisplayUserList(pageable);
		
		// 가입한 유저가 0명일 경우
		if (userEntityPage.getTotalElements() == 0) {
			return Page.empty(pageable);
		}
		
		return userEntityPage;
	}
}
