/* 메인 js */

function logout() {
	console.log('logout')
    $.ajax({
        url: '/api/logout/process',
        method: 'POST',
        success: function(res) {
            window.location.href = '/';
        },
        error: function(err) {
            console.error("로그아웃 실패", err);
        }
    });
}