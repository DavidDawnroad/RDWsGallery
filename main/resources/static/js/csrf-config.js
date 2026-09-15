// (function() {})() -> 즉시 실행 함수 포맷
(function() {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

    if (csrfToken && csrfHeader) {
        axios.defaults.headers.common[csrfHeader] = csrfToken;
    }
})();