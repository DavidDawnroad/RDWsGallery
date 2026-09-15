// loginForm 이라는 이름의 Form 태그에서 submit 이벤트가 발생했을 때 작업을 수행
document.getElementById('loginForm').addEventListener('submit', async function(event) {
	
	// 1. Form 제출 시 페이지 새로고침 비활성화
	event.preventDefault();
	
	// 2. Controller로 보낼 데이터 선언
	const data = { // 데이터
		id: document.getElementById('id').value,
		password: document.getElementById('pw').value,
		rememberId: document.getElementById('rememberId').checked
	};

	// 3. 토스트 메시지용 Notyf 라이브러리 인스턴스
	const notyf = new Notyf({
		duration: 2000, // 1000ms = 1초
		position: {
			x: 'center',
			y: 'top'
		}
	});
	
	try {
		// Axios가 데이터를 자동으로 JSON 형태의 데이터로 변환하므로 Fetch API에서의 JSON.stringify() 작업 생략 가능
		//const response = await axios.post(url, data, config);
		const response = await axios.post(`${contextPath}api/auth/login`, data);
		
		if (response.data.status == 'SUCCESS') {
			// 로그인 페이지로 이동하기 전에 머물던 페이지로 다시 이동하기 위해 URL Query Parameter 분석
			const urlParams = new URLSearchParams(window.location.search);
			// header.html에서 정의한 redirect Key의 Value 추출
			const redirectPath = urlParams.get('redirect');
			
			if (redirectPath) {
				// 인코딩된 주소를 디코딩해서 이동
				window.location.href = decodeURIComponent(redirectPath);
			} else { // 이전에 머물던 페이지가 없을 경우
				window.location.href = `${contextPath}board`;
			}
		}
	} catch (error) {
		console.error('error 발생: ', error);
		
		if (error.response) {
			// 상태 코드 오류 (ex-400, 500)
			notyf.error(error.response.data?.message || '올바르지 않은 입력값입니다.');
		} else if (error.request) {
			// 네트워크 다운
			notyf.error('서버가 응답하지 않습니다.');
		} else {
			notyf.error('요청 중 오류 발생');
		}
	}
	
}
);