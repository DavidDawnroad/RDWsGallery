document.addEventListener('DOMContentLoaded', function() {
	
	const notyf = new Notyf({
		duration: 2000,
		position: {
			x: 'center',
			y: 'top'
		}
	});
	 
	const nicknameInput = document.getElementById('manage-content-nickname');
	const changeButton = document.getElementById('manage-change-my-nickname-button');
	const messageArea = document.getElementById('manage-change-my-nickname-message');
	
	// 관리자가 아니거나 본인 사용자 페이지가 아니라서 닉네임 변경 태그가 없을 경우 리스너 중단
	if (!nicknameInput || !changeButton) {
		return;
	}
	// 현재 닉네임 데이터를 nicknameInput 변수에 저장 (값을 재할당할 수 있도록 const 대신 let으로 선언)
	let presentNickname = nicknameInput.value;
	
	// 닉네임 변경 버튼의 이벤트 리스너 등록
	changeButton.addEventListener("click", async function(event) {
		
		event.preventDefault();
		
		// 닉네임 변경 버튼 클릭 시점의 닉네임 데이터를 newNickname 변수에 저장
		const newNickname = document.getElementById('manage-content-nickname').value.trim();
		
		if (presentNickname === newNickname) {
			messageArea.textContent = '같은 닉네임으로는 변경할 수 없습니다.';
			messageArea.style.color = '#ff4d4f'; // RED
			
			return;
		}
		
		if (!newNickname) {
			messageArea.textContent = '닉네임을 반드시 입력해야 합니다.';
			messageArea.style.color = '#ff4d4f'; // RED
			
			return;
		}
		
		try {
			const data = {
				nickname: newNickname // nickname이라는 이름으로 newNickname의 value를 보냄
			}
			const response = await axios.post(window.contextPath + 'api/manage/my/change-nickname', data);
			
			if (response.data.status == 'SUCCESS') {
				// 변경한 닉네임으로 화면 데이터 최신화
				presentNickname = response.data.newNickname;
				nicknameInput.value = response.data.newNickname;
				messageArea.textContent = '변경 완료. 페이지를 새로고침해서 확인해 보세요.';
				messageArea.style.color = '#32cd32'; // LIMEGREEN
			}
		} catch (error) {
			console.error('error 발생: ', error);

			if (error.response) {
				messageArea.textContent = error.response.data.message;
				messageArea.style.color = '#ff4d4f'; // RED
			} else if (error.request) {
				messageArea.textContent = '서버가 일시적으로 응답하지 않습니다.\n잠시 후 다시 시도해 주세요.';
				messageArea.textContent = '#ff4d4f'; // RED
			} else {
				messageArea.textContent = '요청 중 오류 발생';
				messageArea.textContent = '#ff4d4f'; // RED
			}
		}
	});
});