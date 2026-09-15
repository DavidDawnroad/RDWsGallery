package com.test.prac.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice // 모든 @RestController 에서 발생하는 exception 을 이 곳에서 처리
public class PracAPIExceptionHandler {

	// @Valid 검증 오류 발생 시
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleValidationException(MethodArgumentNotValidException e) {
		Map<String, String> errorData = new HashMap<>();

		// signup.js에서 status 값에 따라 결과를 분기시키기 위한 작업
		errorData.put("status", "FAIL");
		// UserRegisterRequestDTO 에 작성된 상황별 오류 메시지 대입
		errorData.put("message", e.getBindingResult().getFieldError().getDefaultMessage());

		// 상태코드 400: Bad Request 전송
		return ResponseEntity.badRequest().body(errorData);

	}

	// 일반적인 비즈니스 로직 오류 발생 시
	@ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException e) {
        Map<String, String> errorData = new HashMap<>();
        errorData.put("status", "FAIL");
        errorData.put("message", e.getMessage());

        return ResponseEntity.badRequest().body(errorData);
    }

	// 중복 ID로 회원가입 시도 시
	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<Map<String, String>> handleIllegalStateException(IllegalStateException e) {
		Map<String, String> errorData = new HashMap<>();
        errorData.put("status", "FAIL");
        errorData.put("message", e.getMessage());

        return ResponseEntity.badRequest().body(errorData);
	}

	// 같은 비밀번호로 비밀번호 변경 시도 시 (커스텀 에러 클래스)
	@ExceptionHandler(PasswordNotChangedException.class)
	public ResponseEntity<Map<String, String>> handlePasswordNotChangedException(PasswordNotChangedException e) {
		Map<String, String> errorData = new HashMap<>();
        errorData.put("status", "FAIL");
        errorData.put("message", e.getMessage());

        // 422
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorData);
	}
/*	Spring Security 적용 이후 401 상태코드는 PracLoginFailureHandler가 담당하기 때문에 UnauthorizedException.java 삭제와 함께 메서드 주석 처리
	// Spring Security 미적용 상황에서 400: Bad Request 와 401: Unauthorized 를 분리하기 위해 만든 임시 커스텀 에러 클래스
	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<Map<String, String>> handleUnauthorizedException(UnauthorizedException e) {
		Map<String, String> errorData = new HashMap<>();
        errorData.put("status", "FAIL");
        errorData.put("message", e.getMessage());

        // 상태코드 401: Unauthorized 전송
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorData);
	}
*/
	// 커스텀 에러 클래스
	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleUserNotFoundException(UserNotFoundException e) {
		Map<String, String> errorData = new HashMap<>();
        errorData.put("status", "FAIL");
        errorData.put("message", e.getMessage());

        // 400
        return ResponseEntity.badRequest().body(errorData);
	}
	
	// 다른 사용자의 닉네임과 겹칠 때
	@ExceptionHandler(NicknameDuplicatedException.class)
	public ResponseEntity<Map<String, String>> handleNicknameDuplicatedException(NicknameDuplicatedException e) {
		Map<String, String> errorData = new HashMap<>();
        errorData.put("status", "FAIL");
        errorData.put("message", e.getMessage());
		
        // 409
		return ResponseEntity.status(HttpStatus.CONFLICT).body(errorData);
	}
	
	// 본인의 이전 닉네임과 겹칠 때
	@ExceptionHandler(NicknameNotChangedException.class)
	public ResponseEntity<Map<String, String>> handleNicknameNotChangedException(NicknameNotChangedException e) {
		Map<String, String> errorData = new HashMap<>();
		errorData.put("status", "FAIL");
		errorData.put("message", e.getMessage());
		
		// 422
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorData);
	}
}
