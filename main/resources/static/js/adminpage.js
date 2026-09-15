document.addEventListener('DOMContentLoaded', () => {
	const notyf = new Notyf({
		duration: 2000, // 1000ms = 1초
		position: {
			x: 'center',
			y: 'top'
		}
	});
	
	const tableBody = document.querySelector('tbody');
	
	// tbody 태그에 클릭 이벤트 리스너를 등록
	tableBody.addEventListener('click', async function(event) {
		const clickedTarget = event.target;
		
		// 클릭된 대상이 버튼이 아니라면 무시
		if (!clickedTarget.matches('button')) {
			return;
		}
		
		// 클릭된 버튼이 속한 tr 태그와 tr 태그의 유저 ID 데이터 가져오기
		const tr = clickedTarget.closest('tr');
		const userAccountId = tr.dataset.userAccountId;
		
		// Admin의 사용자 닉네임 변경
		if (clickedTarget.classList.contains('admin-change-nickname-button')) {
			// 버튼 중복 클릭 방지
			clickedTarget.disabled = true;
			
			try {
				const nicknameInput = tr.querySelector('.admin-change-nickname-input');
				const newNickname = nicknameInput.value;
				const data = {
					nickname: newNickname
				}
				
				if (!newNickname.trim()) {
					notyf.error('변경할 닉네임을 입력하세요.');
					
					return;
				}
				
				const response = await axios.post(`${window.contextPath}api/manage/admin/change-nickname/${userAccountId}`, data);
				
				if (response.data.status == 'SUCCESS') {
					notyf.success('닉네임 변경 성공');
					nicknameInput.value = '';
					// placeholder를 변경 이후 닉네임으로 세팅
					nicknameInput.placeholder = response.data.newNickname;
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
			} finally {
				clickedTarget.disabled = false;
			}
		}
		
		// Admin의 사용자 상태 변경
		if (clickedTarget.classList.contains('admin-change-status-button')) {
			// 버튼 중복 클릭 방지
			clickedTarget.disabled = true;
			
			const statusSelect = tr.querySelector('.admin-user-status-select');
			const previousStatus = statusSelect.dataset.userStatus;
			
			try {
				const newStatus = statusSelect.value;
				
				const data = {
					userStatus: newStatus
				}
				
				const response = await axios.post(`${window.contextPath}api/manage/admin/change-status/${userAccountId}`, data);
				
				if (response.data.status == 'SUCCESS') {
					notyf.success('계정 상태 변경 성공');
					// 새로운 계정 상태로 Select 태그값 세팅
					statusSelect.dataset.userStatus = newStatus;
				}
			} catch (error) {
				console.error('error 발생: ', error);
				statusSelect.value = previousStatus;

				if (error.response) {
					// 상태 코드 오류 (ex-400, 500)
					notyf.error(error.response.data?.message || '올바르지 않은 입력값입니다.');
				} else if (error.request) {
					// 네트워크 다운
					notyf.error('서버가 응답하지 않습니다.');
				} else {
					notyf.error('요청 중 오류 발생');
				}
			} finally {
				clickedTarget.disabled = false;
			}
		}
	});
});