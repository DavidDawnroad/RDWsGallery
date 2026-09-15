document.addEventListener("DOMContentLoaded", function() {
	
    const buttonWrite = document.getElementById("button-write");
    
    if (buttonWrite) {
        buttonWrite.addEventListener("click", function(event) {
			
			// 1. 백엔드 SecurityConfig에서 로그인 성공 시 FilterChain에 추가된 setSecurityRepository 옵션이 Session에 로그인 유저 데이터를 저장
			// 2. board.html의 th:data-is-logged-in 속성(data-*)이 Session의 데이터 확인 (isAuthenticated()의 반환값을 true로 반환)
			// 3. board.js의 .dataset이 템플릿 페이지에 있는 data- 뒤의 이름을 객체의 Key로 변환(케밥표기->카멜표기)하여 사용 준비 
			
            // dataset에서 가져온 값은 무조건 String 타입이 되므로 주의
            const isLoggedIn = buttonWrite.dataset.isLoggedIn === 'true'; // boolean이 아니라 문자열 true와 직접 비교
            
            if (!isLoggedIn) { // 비로그인 상태일 경우
				event.preventDefault(); // <a>태그의 이동 기능을 막은 뒤
                alert("로그인이 필요한 서비스입니다.\n로그인 화면으로 이동합니다."); // alert창을 띄우고
                window.location.href = `${contextPath}login`; // 로그인 화면으로 리다이렉트
            }
        });
    }
});