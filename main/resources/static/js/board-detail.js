// 화면 로드 완료 시
document.addEventListener("DOMContentLoaded", function() {
	
	
//	const boardSeq = document.getElementById("detail-container").dataset.boardSeq;
	// dataset 객체로 boardSeq 값에 접근
	const boardSeq = document.querySelector(".detail-container").dataset.boardSeq; // . 은 태그 속성이 class 임을 식별할 수 있도록 하는 class 식별자
	const viewsToken = document.querySelector(".detail-container").dataset.viewsToken;
	const deleteButton = document.querySelector(".js-detail-delete-button");
	const updateButton = document.querySelector(".js-detail-update-button");
	const replyArea = document.getElementById("reply-content-textarea");
	const replyButton = document.getElementById("reply-submit-button");
	const likeImage = document.getElementById("like-image");
	const dislikeImage = document.getElementById("dislike-image");
	
	const notyf = new Notyf({
		duration: 2000,
		position: {
			x: 'center',
			y: 'top'
		}
	});
	
	const data = {
		viewsToken: viewsToken
	}
	
	if (boardSeq) { // 값이 빈 채로 API가 호출되어 400 Bad Request or 404 Not Found 가 반환되는 상황 방지
		// /prac/api/board/게시글번호/increse-views 주소로 POST 요청
		axios.post(window.contextPath + 'api/board/' + boardSeq + '/increase-views', data)
			//BoardAPIController.increaseViews() 가 성공하면
			 .then(() => {
				const viewCount = document.getElementById("detail-view-count");
				
				if (viewCount) {
					const currentViewCount = parseInt(viewCount.textContent, 10); // 2번째 인자는 n진수 인자
					// 프론트엔드에서 조회수 + 1 이 바로 반영되게끔 설정
					viewCount.textContent = isNaN(currentViewCount) ? 1 : currentViewCount + 1;
				}
			 })
			 .catch(error => {
				console.error("조회수 반영 중 오류 발생: ", error);
			 });
	}
	
	if (deleteButton) {
		deleteButton.addEventListener("click", function() {
			if (confirm("삭제된 게시물은 복구할 수 없습니다.\n정말 삭제하시겠습니까?")) {
				axios.post(window.contextPath + 'api/board/' + boardSeq + '/delete')
					 .then(() => { window.location.href = `${window.contextPath}board`; })
					 .catch(error => {
							if (error.response) {
								if (error.response.status === 403) {
									alert('타인의 게시글은 삭제할 수 없습니다.');
									window.location.href = `${window.contextPath}board`;
								}
							} else if (error.request) {
								notyf.error('서버가 응답하지 않습니다.');
							} else {
								notyf.error('요청 중 오류가 발생했습니다.');
							}
					 });
			}
		});
	}
	
	if (updateButton) {
		updateButton.addEventListener("click", function() {
			window.location.href = `${window.contextPath}board/${boardSeq}/update`;
		});
	}
	
	if (replyButton) {
		replyButton.addEventListener("click", async function(event) {
			
			event.preventDefault();
			
			const userInfo = document.querySelector(".detail-userinfo");
			
			// 로그인하지 않은 사용자가 댓글 등록 버튼을 눌렀을 경우
			if (userInfo && userInfo.textContent.trim() === "비회원") {
				notyf.error("로그인 후 댓글을 작성하세요.");
				return;
			}
			
			const replyContent = document.getElementById("reply-content-textarea");
			const data = { content: replyContent.value.trim() }
			
			if (!data.content) {
				notyf.error("댓글을 필수로 입력해야 합니다.");
				replyArea.focus();
				return;
			}
			
			try {
				const response = await axios.post(window.contextPath + 'api/board/' + boardSeq + '/reply/create', data);
				
				if (response.data.status == 'SUCCESS') {
					
					// 입력창 내용 초기화
					replyContent.value = '';
					
					// 댓글페이지 첫 페이지를 타겟으로 설정
					const targetPage = 0;
					
					// axios로 댓글 페이지의 Query String이 추가된 BoardDetail 페이지 GET요청
					axios.get(window.contextPath + 'board/' + boardSeq + '?reply-page=' + targetPage)
						 .then(replyResponse => {
							// 백엔드에서 넘어온 HTML 문자열을 가지고 가상 DOM 객체 생성
							const parser = new DOMParser();
							const newHTMLDocument = parser.parseFromString(replyResponse.data, 'text/html');
							
							// 새로운 HTML DOM에 있는 댓글목록 영역 추출
							const newReplyDisplay = newHTMLDocument.querySelector('.detail-reply-display');
							// 기존의 HTML DOM에 있는 댓글목록 영역 추출
							const currentReplyDisplay = document.querySelector('.detail-reply-display');
							// 새로운 HTML DOM에 있는 댓글 페이지블록 영역 추출
							const newReplyPagination = newHTMLDocument.querySelector('.detail-reply-pagenation-container');
							// 기존의 HTML DOM에 있는 댓글 페이지블록 영역 추출
							const currentReplyPagination = document.querySelector('.detail-reply-pagenation-container');
							
							if (newReplyDisplay) {
								// 댓글 데이터가 이미 있는 경우 새 댓글목록 영역으로 교체
								if (currentReplyDisplay) {
									currentReplyDisplay.replaceWith(newReplyDisplay);
								} else {
									// 댓글 데이터가 없어서 영역이 아예 없는 경우(th:if 때문에 render되지 않은 경우) 댓글 영역을 통째로 추가
									const detailContent = document.querySelector('.like-box');
									if (detailContent) {
										detailContent.after(newReplyDisplay);
									}
								}
							}
							
							if (currentReplyPagination && newReplyPagination) {
								// 댓글 페이지 블록 영역 교체
								currentReplyPagination.replaceWith(newReplyPagination);
							} else if (!currentReplyPagination && newReplyPagination) {
								// 영역이 없다가 새로 생겼을 경우 댓글목록 뒤에 삽입
								document.querySelector('.detail-reply-display')?.after(newReplyPagination);
							} else if (currentReplyPagination && !newReplyPagination)
								// 페이지블록이 사라져야 하는 경우 삭제
								currentReplyPagination.remove();
						 })
						 .catch(error => {
							console.error('댓글 목록 요청 중 오류 발생: ', error);
						 });
					
				}
			} catch (error) {
				console.error('error 발생: ', error);
				
				if (error.response) {
					notyf.error(error.response.data?.message || '댓글 등록 중 오류가 발생했습니다.');
				} else if (error.request) {
					notyf.error('서버가 일시적으로 응답하지 않습니다.\n잠시 후 다시 시도해 주세요.');
				} else {
					notyf.error('요청 중 오류 발생');
				}
			}
		});
	}
	
	// if문 대신 Optional Chaining 사용
	likeImage?.addEventListener("click", async function(event) {
		
		event.preventDefault();
		
		const userInfo = document.querySelector(".detail-userinfo");

		// 로그인하지 않은 사용자가 추천 버튼을 눌렀을 경우
		if (userInfo && userInfo.textContent.trim() === "비회원") {
			notyf.error("로그인한 사용자만 추천을 누를 수 있습니다.");
			return;
		}
		
		try {
			const response = await axios.post(window.contextPath + 'api/board/' + boardSeq + '/like');
			const likesData = response.data;
			
			const likes = document.getElementById("like-value");
			
			if (likes) {
				likes.textContent = likesData;
			}
		} catch (error) {
			console.error('error 발생: ', error);
			
			if (error.response) {
				notyf.error(error.response.data?.message || '작업 중 오류가 발생했습니다.');
			} else if (error.request) {
				notyf.error('서버가 일시적으로 응답하지 않습니다.\n잠시 후 다시 시도해 주세요.');
			} else {
				notyf.error('요청 중 오류 발생');
			}
		}
	});
	
	dislikeImage?.addEventListener("click", async function(event) {
		
		event.preventDefault();
		
		const userInfo = document.querySelector(".detail-userinfo");

		// 로그인하지 않은 사용자가 비추천 버튼을 눌렀을 경우
		if (userInfo && userInfo.textContent.trim() === "비회원") {
			notyf.error("로그인한 사용자만 비추천을 누를 수 있습니다.");
			
			return;
		}
		
		try {
			const response = await axios.post(window.contextPath + 'api/board/' + boardSeq + '/dislike');
			const dislikesData = response.data;

			const dislikes = document.getElementById("dislike-value");
			
			if (dislikes) {
				dislikes.textContent = dislikesData;
			}
		} catch (error) {
			console.error('error 발생: ', error);
			
			if (error.response) {
				notyf.error(error.response.data?.message || '작업 중 오류가 발생했습니다.');
			} else if (error.request) {
				notyf.error('서버가 일시적으로 응답하지 않습니다.\n잠시 후 다시 시도해 주세요.');
			} else {
				notyf.error('요청 중 오류 발생');
			}
		}
	});
	
	// document 전체 영역 대신 .detail-container 내부의 클릭만 감지하도록 영역 조절
	document.querySelector(".detail-container")?.addEventListener("click", async function(event) {
		// 클릭 이벤트 발생 시 그 클릭 이벤트의 발생 위치(event.target)에서 상위 태그로 계속 올라가며 .reply-delete-button 클래스를 가지고 있는 태그가 나올 때까지 탐색
		// 왜 closest()를 사용하는가? -> 사용자가 <button> 태그 대신 하위 태그인 <img> 태그를 클릭하여 지정한 클래스를 찾지 못 하는 경우를 대비하기 위함
		const replyDeleteButton = event.target.closest(".reply-delete-button");
		
		if (replyDeleteButton) {
			event.preventDefault();
			
			// 취소 버튼을 눌렀을 경우 현상복귀
			if (!confirm("댓글을 삭제하시겠습니까?")) {
	            return;
	        }
			// th:data-reply-seq="${reply.replySeq}"로 저장했던 replySeq 정보를 가져옴
			const replySeq = replyDeleteButton.dataset.replySeq;
			
			try {
				await axios.post(window.contextPath + 'api/board/' + boardSeq + '/reply/' + replySeq + '/delete');
				
				// 댓글을 삭제했을 경우 댓글 첫 페이지를 다시 불러옴
				const targetPage = 0;
				const replyResponse = await axios.get(window.contextPath + 'board/' + boardSeq + '?reply-page=' + targetPage);
				const parser = new DOMParser();
				const newHTMLDocument = parser.parseFromString(replyResponse.data, 'text/html');
				const newReplyDisplay = newHTMLDocument.querySelector('.detail-reply-display');
				// (댓글 페이징 화면도 같이 교체하기 위해 변수 지정)
				const newReplyPagination = newHTMLDocument.querySelector('.detail-reply-pagenation-container');
				// try문 초반의 axios.post 요청으로 백엔드(DB)에서는 이미 댓글이 삭제되었지만, 프론트엔드에서는 화면이 아직 갱신되지 않았기 때문에 태그 껍데기가 남아있다
				const currentReplyDisplay = document.querySelector('.detail-reply-display');
				const currentReplyPagination = document.querySelector('.detail-reply-pagenation-container');
				
				// 이전 댓글 데이터가 있고, 새로운 댓글 데이터도 남아있을 경우
				if (currentReplyDisplay && newReplyDisplay) {
					// 교체
					currentReplyDisplay.replaceWith(newReplyDisplay);
				// 댓글이 없다가 처음 작성됐을 경우
				} else if (!currentReplyDisplay && newReplyDisplay) {
					// 태그가 없는데 replace를 사용하면 오류가 발생하니까 바로 위쪽 태그인 .detail-content 밑에 붙여서 생성 (.detail-content 태그가 있을때만 after 실행)
					document.querySelector('.detail-content')?.after(newReplyDisplay);
				// 댓글이 전부 삭제돼서 남은 댓글 데이터가 없을 경우
				} else if (currentReplyDisplay && !newReplyDisplay) {
					// .detail-reply-display 태그 영역을 통째로 삭제
					currentReplyDisplay.remove();
				}
				
				if (currentReplyPagination && newReplyPagination) {
					currentReplyPagination.replaceWith(newReplyPagination);
				} else if (!currentReplyPagination && newReplyPagination) {
					document.querySelector('.detail-reply-display')?.after(newReplyPagination);
				} else if (currentReplyPagination && !newReplyPagination)
					currentReplyPagination.remove();
				
			} catch (error) {
				console.error('error 발생: ', error);
				
				if (error.response) {
					notyf.error(error.response.data?.message || '댓글 삭제 중 오류가 발생했습니다.');
				} else if (error.request) {
					notyf.error('서버가 일시적으로 응답하지 않습니다.\n잠시 후 다시 시도해 주세요.');
				} else {
					notyf.error('요청 중 오류 발생');
				}
			}
		}
	});
	
});