document.getElementById('post-button').addEventListener('click', async function(event) {
	
	event.preventDefault();
	
	const notyf = new Notyf({
		duration: 2000,
		position: {
			x: 'center',
			y: 'top'
		}
	});
	
	const titleInput = document.getElementById('post-title');
	const contentInput = document.getElementById('post-content');
	
	const data = {
		category: document.getElementById('post-category').value,
		title: titleInput.value.trim(),
		content: contentInput.value.trim()
	};
	
	if (!data.title) {
		notyf.error("제목을 필수로 입력해야 합니다.");
		titleInput.focus();
		return;
	}
	
	if (!data.content) {
		notyf.error("내용을 필수로 입력해야 합니다.");
		contentInput.focus();
		return;
	}
	
	try {
		const response = await axios.post(`${contextPath}api/board/post`, data);
		
		if (response.data.status == 'SUCCESS') {
			window.location.href = `${contextPath}board`;
		}
	} catch (error) {
		console.error('error 발생: ', error);
		
		if (error.response) {
			notyf.error(error.response.data?.message || '게시글 등록 중 오류가 발생했습니다.');
		} else if (error.request) {
			notyf.error('서버가 일시적으로 응답하지 않습니다.\n잠시 후 다시 시도해 주세요.');
		}else {
			notyf.error('요청 중 오류 발생');
		}
	}
});