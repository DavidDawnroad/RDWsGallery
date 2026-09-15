document.getElementById('updateForm').addEventListener('submit', async function(event) {
	
	event.preventDefault();
	
	const pw = document.getElementById('pw').value;
	const pwCheckInput = document.getElementById('pwCheck');
	const pwCheck = pwCheckInput.value;
	const resetPasswordToken = sessionStorage.getItem('resetPasswordToken');
	
	// 토큰이 없을 경우 (verify-user 단계를 거치지 않고 직접 접근했을 경우)
	if (!resetPasswordToken) {
		alert('잘못된 접근입니다.');
		window.location.href = `${window.contextPath}verify-user`;
		return;
	}
	
	// 새 요청 시 기존의 에러메시지 내용 초기화
	const serverMessage = document.getElementById('serverMessage');
	
	if (serverMessage) {
		serverMessage.textContent = '';
	}
	
	// 프론트엔드 단에서 pw와 pwCheck 검사
	if (pw !== pwCheck) {
		if (serverMessage) {
			serverMessage.textContent = '비밀번호가 일치하지 않습니다.';
		}
		// pwCheck 텍스트박스로 cursor 자동 이동
		pwCheckInput.focus();
		// 서버로 데이터 전송하지 않고 함수 종료
		return;
	}
	
	const data = {
		password: document.getElementById('pw').value,
		resetPasswordToken: resetPasswordToken
	};
	
	try {
		const response = await axios.post(`${window.contextPath}api/auth/update-password`, data);
		
		if (response.data.status == 'SUCCESS') {
			// 데이터 흔적 삭제
			sessionStorage.removeItem('resetPasswordToken');
			alert('비밀번호가 변경되었습니다.\n새로운 비밀번호로 로그인 해주세요.');
			window.location.href = `${window.contextPath}login`;
		}	
	} catch (error) {
		console.error('error 발생: ', error);
		
		if (error.response) {
			if (error.response.status === 400) {
				// serverMessage div 태그에 문구 출력
				const errorMessage = error.response.data?.message || '올바르지 않은 입력값입니다.';
				if (serverMessage) {
					serverMessage.textContent = errorMessage;
				}
			} else if (error.response.status === 401) {
				const errorMessage = error.response.data?.message || '계정 검증 중 오류가 발생했습니다.';
				if (serverMessage) {
					serverMessage.textContent = errorMessage;
				}
			} else {
				alert(`오류 코드: ${error.response.status}가 발생했습니다.\n잠시 후에 다시 시도해주세요.`);
			}
		} else if (error.request) {
			alert('서버가 응답하지 않습니다.');
		} else {
			alert('요청 중 오류가 발생했습니다.');
		}
	}
});