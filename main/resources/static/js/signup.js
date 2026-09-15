document.getElementById('signupForm').addEventListener('submit', async function(event) {
	// Form 제출 시 페이지 새로고침 비활성화
	event.preventDefault();
	// 토스트 메시지용 Notyf 라이브러리 인스턴스
	const notyf = new Notyf({
		duration: 2000, // 2초
		position: {
			x: 'center',
			y: 'top'
		}
	});
	
	const id = document.getElementById('id');
	const idRegex = /^(?=.*[A-Za-z])\S{4,20}$/; // 공백 없는 영문 포함 4자 이상 20자 이하
	const pw = document.getElementById('pw');
	const pwRegex = /^(?=.*[A-Za-z])(?=.*\d)\S{8,}$/; // 공백 없는 영문+숫자 8자 이상
	const nickname = document.getElementById('nickname');
	const nicknameRegex = /^\S{2,12}$/; // 공백 없는 2자 이상 12자 이하
	
	const signupMessage = document.getElementById('signupMessage');
	if (signupMessage) {
		signupMessage.textContent = '';
	}
	
	// 백엔드로 데이터를 보내기 전에 프론트엔드에서 데이터 먼저 검사
	if (!idRegex.test(id.value)) {
		if (signupMessage) {
			signupMessage.textContent = 'ID를 영문 포함 공백 없이 4자 이상 20자 이하로 입력해야 합니다.';
		}
		id.focus();
		return;
	}
	
	if (!pwRegex.test(pw.value)) {
		if (signupMessage) {
			signupMessage.textContent = '비밀번호를 공백 없이 영문+숫자 8자 이상으로 입력해야 합니다.';
		}
		pw.focus();
		return;
	}
	
	if (!nicknameRegex.test(nickname.value)) {
		if (signupMessage) {
			signupMessage.textContent = '닉네임을 공백 없이 2자 이상 12자 이하로 입력해야 합니다.';
		}
		nickname.focus();
		return;
	}
	// Controller로 보낼 데이터 선언
	const data = { // 데이터
		id: id.value.trim(), // id 전후 공백오타 제거
		password: pw.value,
		email: document.getElementById('email').value,
		nickname: nickname.value
	};
	
	try {
		// JSON 형태 문자열을 HTTP body에 싣고 백엔드로 전송
		const response = await axios.post(`${contextPath}api/auth/signup`, data);
		
		// AccountAPIController.createAccount.signupData.status
		if (response.data.status == 'SUCCESS') {
			notyf.success(`${response.data.nickname} 님, 환영합니다.\n가입한 ID로 다시 로그인 해주세요.`);
			
			setTimeout(() => {window.location.href = `${contextPath}login`}, 2000);
		}
	} catch (error) {
		console.error('error 발생: ', error);
		
		if (error.response) { // 서버의 응답이 400, 404, 502 등일 경우
			
			if (error.response.status === 400) {
				// ?(Optional Chaining)을 사용하여 에러메시지 미전달 상황 방지
				// 에러메시지가 없을 경우 || 오른쪽의 문장 출력
				const errorMessage = error.response.data?.message || '올바르지 않은 입력값입니다.';
				if (signupMessage) {
					signupMessage.textContent = errorMessage;
				}
			} else {
				notyf.error(`오류 코드: ${error.response.status}`);
			}
		} else if (error.request) { // 서버가 아예 다운되었을 경우
			notyf.error('서버가 응답하지 않습니다.');
		} else {
			notyf.error('요청 중 오류가 발생했습니다.');
		}
	}
});