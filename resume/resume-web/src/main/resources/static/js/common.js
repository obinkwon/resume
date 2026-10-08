/* 공통 js */
// 요소가 없거나 val()이 null(옵션 없는 select 등)이어도 에러 없이 처리, 빈 문자열은 null로 변환
function getVal($scope, name) {
    const v = $scope.find(`[name=${name}]`).val();
    if (v === undefined || v === null) return null;
    const s = String(v).trim();
    return s === '' ? null : s;
}
 
function getNumber($scope, name) {
    const v = getVal($scope, name);
    if (v === null) return null;
    const n = Number(v);
    return Number.isNaN(n) ? null : n;
}
 
// 시작일 > 종료일 검사 (yyyy-MM-dd / yyyy-MM 형식은 문자열 비교로 충분)
function isInvalidPeriod(startDate, endDate) {
    return startDate !== null && endDate !== null && startDate > endDate;
}