package com.test.prac.service;

import java.time.Duration;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.test.prac.dto.UserUpdatePasswordRequestDTO;
import com.test.prac.dto.UserRegisterRequestDTO;
import com.test.prac.dto.UserVerifyPasswordRequestDTO;
import com.test.prac.entity.UserAccountEntity;
import com.test.prac.exception.PasswordNotChangedException;
import com.test.prac.exception.UserNotFoundException;
import com.test.prac.repository.UserAccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserAccountService {

	private final UserAccountRepository userAccountRepository;
	private final BCryptPasswordEncoder bcryptPasswordEncoder;
	private final StringRedisTemplate stringRedisTemplate;

	@Transactional // createAccount 메서드는 CREATE 라는 데이터 변경작업을 수행하기 때문에 @Transactional 어노테이션을 따로 명시
	public UserAccountEntity createAccount(UserRegisterRequestDTO registerDTO) {

		// 중복 ID 검사
		if (userAccountRepository.existsByUserAccountId(registerDTO.getId())) {
			throw new IllegalStateException("이미 존재하는 ID입니다.");
		}

		// 중복 닉네임 검사
		if (userAccountRepository.existsByNickname(registerDTO.getNickname())) {
			throw new IllegalStateException("이미 존재하는 닉네임입니다.");
		}
		// 프론트엔드를 통해 들어온 DTO에서 비밀번호를 꺼내서 bcryptPasswordEncoder로 암호화 (DTO 값 변경 없음)
		String encodedPassword = bcryptPasswordEncoder.encode(registerDTO.getPassword());

		// toEntity()로 DTO -> Entity 변환이 이루어지는 시점에 encodedPassword 데이터를 매개변수로 밀어넣고, DB에 완성된 Entity 를 save(INSERT)
		UserAccountEntity userEntity = userAccountRepository.save(registerDTO.toEntity(encodedPassword)); // save()는 JPARepository 내장함수이기 때문에 Repository 파일에 적지 않음

		return userEntity;
	}

/* Spring Security 로 로그인 검증 기능을 이관하면서 해당 메서드 주석 처리
	// SELECT 기능만 수행하는 조회 전용 메서드이므로 @Transactional 미포함
	public UserAccountEntity checkLogin(UserCheckLoginRequestDTO checkDTO) {

		UserAccountEntity entity = userAccountRepository.findByUserAccountId(checkDTO.getId())
															.orElseThrow(() -> new UnauthorizedException("없는 ID이거나 비밀번호가 틀렸습니다."));

		// 비밀번호 불일치 시
		if (!bcryptPasswordEncoder.matches(checkDTO.getPassword(), entity.getPassword())) {
			throw new UnauthorizedException("없는 ID이거나 비밀번호가 틀렸습니다.");
		}

		return entity;
	}
*/
	@Transactional(readOnly = true)
	public String verifyUser(UserVerifyPasswordRequestDTO verifyDTO) {

		UserAccountEntity userEntity = userAccountRepository.findByUserAccountIdAndEmail(verifyDTO.getId(), verifyDTO.getEmail())
															.orElseThrow(() -> new UserNotFoundException("일치하는 데이터가 없습니다."));

		// userEntity가 존재함을 확인했으니 비밀번호 변경을 위한 임시 토큰 발급
		return resetPasswordToken(userEntity.getId()); // (만약 JWT를 도입한다면 JWT 토큰 return)
	}
	
	// verifyUser()를 통과한 사용자 정보에 대해 비밀번호 변경용 임시 토큰을 발급하여 updatePassword() 단계로 진행할 수 있게 하는 method
	private String resetPasswordToken(String userAccountId) {
		
		// 비밀번호 변경용 임시 토큰을 발행하기 위해 128-bit 고유 식별자인 UUID 사용
		String token = UUID.randomUUID().toString();
		String redisKey = "user:reset-password-token:" + token;
		
		// userAccountId를 value 로 넣고 Key 등록
		stringRedisTemplate.opsForValue().set(redisKey, userAccountId, Duration.ofMinutes(4));
		
		return token;
	}

	@Transactional
	public void updatePassword(UserUpdatePasswordRequestDTO updateDTO) {
		
		String redisKey = "user:reset-password-token:" + updateDTO.getResetPasswordToken();
		// 토큰용 Key 를 가져오면서 동시에 삭제
		String userAccountId = stringRedisTemplate.opsForValue().getAndDelete(redisKey);

		// 원본 Entity 데이터를 꺼내옴
		UserAccountEntity userEntity = userAccountRepository.findByUserAccountId(userAccountId)
															.orElseThrow(() -> new UserNotFoundException("존재하지 않는 계정입니다."));

		// 이전 비밀번호와 바꿀 비밀번호가 같을 경우
		if (bcryptPasswordEncoder.matches(updateDTO.getPassword(), userEntity.getPassword())) { // 평문-암호문 비교이지만 matches() 덕분에 정상 비교 가능
			throw new PasswordNotChangedException("이전과 같은 비밀번호로는 변경할 수 없습니다.");
		}
		// 바꿀 비밀번호를 암호화
		String encodedPassword = bcryptPasswordEncoder.encode(updateDTO.getPassword());

		// Entity 에 저장
		userEntity.changePassword(encodedPassword);
	}
}
