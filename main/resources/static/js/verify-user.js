document.getElementById('verifyForm').addEventListener('submit', async function(event) {
	
	event.preventDefault();
	
	const id = document.querySelector('.find-id');
	const email = document.querySelector('.find-email');
	
	const data = {
		id: id.value,
		email: email.value
	};
	
	const notyf = new Notyf({
		duration: 2000, // 1000ms = 1초
		position: {
			x: 'center',
			y: 'top'
		}
	});
	
	try {
		
		const response = await axios.post(`${window.contextPath}api/auth/verify-user`, data);
		
		if (response.data.status == 'SUCCESS') {
			// SessionStorage에 토큰 값 저장
			sessionStorage.setItem('resetPasswordToken', response.data.token);
			window.location.href = `${window.contextPath}update-password`;
		}	
	} catch (error) {
		console.error('error 발생: ', error);
		
		if (error.response) {
			notyf.error(error.response.data?.message || '올바르지 않은 입력값입니다.');
		} else if (error.request) {
			notyf.error('서버가 응답하지 않습니다.');
		} else {
			notyf.error('요청 중 오류가 발생했습니다.');
		}
	}
}
);