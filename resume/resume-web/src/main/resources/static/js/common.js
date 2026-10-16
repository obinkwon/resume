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

/** API 실패 정보 : status (0 = 네트워크 오류), data (서버가 보낸 에러 body) */
class ApiError extends Error {
    constructor(status, data, cause) {
        const serverMsg = data && typeof data === 'object' ? data.message : '';
        super(serverMsg || (status ? `요청에 실패했어요. (${status})` : '서버에 연결할 수 없어요.'));
        this.name = 'ApiError';
        this.status = status;
        this.data = data;
        if (cause) this.cause = cause;
    }
}

/** Spring Security CSRF 메타 태그 → 헤더 (없으면 빈 객체) */
function getCsrfHeaders() {
    const token  = document.querySelector('meta[name="_csrf"]')?.content;
    const header = document.querySelector('meta[name="_csrf_header"]')?.content;
    return token && header ? { [header]: token } : {};
}

function toQueryString(data) {
    const params = new URLSearchParams();
    Object.entries(data).forEach(([key, value]) => {
        if (value == null) return;
        if (Array.isArray(value)) value.forEach(v => params.append(key, v));
        else params.append(key, value);
    });
    return params.toString();
}

async function readResponseBody(res) {
    if (res.status === 204) return null;
    const text = await res.text();
    if (!text) return null;
    const type = res.headers.get('Content-Type') || '';
    if (type.includes('json')) {
        try { return JSON.parse(text); } catch (e) { return text; }
    }
    return text;
}

function callApi({ url, method = 'GET', data, headers = {}, success, error, complete } = {}) {
    method = method.toUpperCase();
    const options = {
        method,
        headers: Object.assign({ Accept: 'application/json' }, headers)
    };

    if (data != null) {
        if (method === 'GET') {
            const qs = toQueryString(data);
            if (qs) url += (url.includes('?') ? '&' : '?') + qs;
        } else {
            options.headers['Content-Type'] = 'application/json';
            options.body = JSON.stringify(data);
        }
    }
    if (method !== 'GET') Object.assign(options.headers, getCsrfHeaders());

    const request = fetch(url, options)
        .catch(err => { throw new ApiError(0, null, err); })        // 네트워크 오류
        .then(async res => {
            if (!res.ok) throw new ApiError(res.status, await readResponseBody(res));
            const body = isBlob ? await res.blob() : await readResponseBody(res);
            return { body, res };
        });

    return request
        .then(
            ({ body, res }) => (success ? success(body, res) : body),
            err => {
                if (error) return error(err);
                console.error('[API]', method, url, err);
                throw err;
            }
        )
        .finally(() => { if (complete) complete(); });
}