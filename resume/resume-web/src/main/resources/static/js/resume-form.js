/* =========================================================
   resume-form.js  (이력서 작성 폼)
   항목 추가·삭제 : 외부 링크 / 기술 스택 / 자기소개서 / 학력 / 자격증 / 경력 / 프로젝트
   가져오기       : 다른 이력서 내용을 골라 이 폼에 채움 (저장해야 반영)
   저장           : 화면 입력값을 모아 검사 후 /api/resume/save 로 전송
   ========================================================= */
(() => {
    'use strict';

    // ---------- 상수 ----------
    const API = {
        resumeList: '/api/resume/list',
        resumeDetail: id => `/api/resume/${encodeURIComponent(id)}`
    };

    const LINK_TYPES = [
        ['github', 'GitHub'],
        ['linkedin', 'LinkedIn'],
        ['blog', '블로그'],
        ['portfolio', '포트폴리오'],
        ['website', '웹사이트'],
        ['etc', '기타']
    ];

    const PROFICIENCY = [
        ['1', '1 - 초급'],
        ['2', '2'],
        ['3', '3 - 중급'],
        ['4', '4'],
        ['5', '5 - 고급']
    ];

    /** 인적 사항 입력칸 (가져올 때 이 칸들만 덮어씀) */
    const PROFILE_FIELDS = ['name', 'email', 'phone', 'address'];

    const REMOVE_BTN = `
                <button type="button" class="btn icon danger-ghost removeBtn" aria-label="삭제">
                    <i class="icon icon-close"></i>
                </button>`;

    const $form = $('#resumeForm');

    // ---------- 유틸 ----------
    function addBlock(listId, html) {
        document.getElementById(listId).insertAdjacentHTML('beforeend', html);
    }

    /** 객체의 모든 값이 빈 문자열(공백만 있는 경우 포함)인지 */
    function isAllEmpty(obj) {
        return Object.values(obj).every(v => !v || v.trim() === '');
    }

    /** 속성값·텍스트에 넣을 값 이스케이프 (null → '') */
    function esc(value) {
        return String(value ?? '').replace(/[&<>"']/g, c => (
            { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]
        ));
    }

    /**
     * <input type="date"> 용 'yyyy-MM-dd'
     *  - '2024-03-01', '2024-03-01T10:00:00', [2024, 3, 1] 모두 처리
     */
    function toDateValue(value) {
        if (!value) return '';
        if (Array.isArray(value)) {
            const [y, m, d] = value;
            return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
        }
        return String(value).slice(0, 10);
    }

    /** <option> 목록 (selected 값과 같은 항목 선택) */
    function optionsHtml(list, selected, placeholder) {
        const current = String(selected ?? '');
        return `<option value="">${placeholder}</option>` + list.map(([value, text]) =>
            `<option value="${value}"${current === value ? ' selected' : ''}>${text}</option>`).join('');
    }

    // ---------- 상태 ----------
    /** 저장 중복 클릭 방지 (핸들러 밖에 있어야 클릭 간에 값이 유지됨) */
    let isSaving = false;

    // ---------- 항목 HTML ----------
    // item 을 넘기면 값을 채운 항목, 안 넘기면 빈 항목
    // (PK hidden 은 넣지 않음 — 새 항목이므로)

    function linkRowHtml(item = {}) {
        return `
            <div class="item-row link-cols">
                <select name="linkType">${optionsHtml(LINK_TYPES, item.linkType, '유형 선택')}</select>
                <input type="text" name="displayName" placeholder="예: 기술 블로그" value="${esc(item.displayName)}">
                <input type="url" name="url" placeholder="https://" value="${esc(item.url)}">${REMOVE_BTN}
            </div>`;
    }

    function skillRowHtml(item = {}) {
        return `
            <div class="item-row">
                <input type="text" name="skillName" placeholder="기술 입력" value="${esc(item.skillName)}">
                <input type="text" name="category" placeholder="카테고리 (예: Backend)" value="${esc(item.category)}">
                <select name="proficiency">${optionsHtml(PROFICIENCY, item.proficiency, '숙련도 선택')}</select>${REMOVE_BTN}
            </div>`;
    }

    function educationCardHtml(item = {}) {
        return `
            <div class="item-card">${REMOVE_BTN}
                <label class="field">
                    <span class="label">학교명</span>
                    <input type="text" name="schoolName" placeholder="학교" value="${esc(item.schoolName)}">
                </label>
                <label class="field">
                    <span class="label">전공</span>
                    <input type="text" name="major" placeholder="전공" value="${esc(item.major)}">
                </label>
                <label class="field">
                    <span class="label">학위</span>
                    <input type="text" name="degree" placeholder="예: 학사" value="${esc(item.degree)}">
                </label>
                <div class="date-row">
                    <label class="field">
                        <span class="label">입학일</span>
                        <input type="date" name="startDate" value="${esc(toDateValue(item.startDate))}">
                    </label>
                    <label class="field">
                        <span class="label">졸업일</span>
                        <input type="date" name="endDate" value="${esc(toDateValue(item.endDate))}">
                    </label>
                </div>
                <label class="field">
                    <span class="label">학력 설명</span>
                    <textarea name="description" placeholder="학력 설명" rows="3">${esc(item.description)}</textarea>
                </label>
            </div>`;
    }

    function certificateCardHtml(item = {}) {
        return `
            <div class="item-card">${REMOVE_BTN}
                <label class="field">
                    <span class="label">자격증명</span>
                    <input type="text" name="certificateName" placeholder="예: 정보처리기사" value="${esc(item.certificateName)}">
                </label>
                <div class="date-row">
                    <label class="field">
                        <span class="label">발급 기관</span>
                        <input type="text" name="issuer" placeholder="예: 한국산업인력공단" value="${esc(item.issuer)}">
                    </label>
                    <label class="field">
                        <span class="label">취득일</span>
                        <input type="date" name="issuedDate" value="${esc(toDateValue(item.issuedDate))}">
                    </label>
                </div>
                <label class="field">
                    <span class="label">자격증 설명</span>
                    <textarea name="description" placeholder="자격증 설명" rows="3">${esc(item.description)}</textarea>
                </label>
            </div>`;
    }

    function experienceCardHtml(item = {}) {
        return `
            <div class="item-card">${REMOVE_BTN}
                <label class="field">
                    <span class="label">회사명</span>
                    <input type="text" name="companyName" placeholder="회사명" value="${esc(item.companyName)}">
                </label>
                <label class="field">
                    <span class="label">직무</span>
                    <input type="text" name="position" placeholder="직무" value="${esc(item.position)}">
                </label>
                <div class="date-row">
                    <label class="field">
                        <span class="label">입사일</span>
                        <input type="date" name="startDate" value="${esc(toDateValue(item.startDate))}">
                    </label>
                    <label class="field">
                        <span class="label">퇴사일</span>
                        <input type="date" name="endDate" value="${esc(toDateValue(item.endDate))}">
                    </label>
                </div>
                <label class="check">
                    <input type="checkbox" name="isCurrent"${item.isCurrent ? ' checked' : ''}>
                    <span>재직중</span>
                </label>
                <label class="field">
                    <span class="label">업무 내용</span>
                    <textarea name="description" placeholder="업무 내용">${esc(item.description)}</textarea>
                </label>
            </div>`;
    }

    function projectCardHtml(item = {}) {
        return `
            <div class="item-card">${REMOVE_BTN}
                <label class="field">
                    <span class="label">프로젝트명</span>
                    <input type="text" name="projectName" placeholder="프로젝트명" value="${esc(item.projectName)}">
                </label>
                <div class="date-row">
                    <label class="field">
                        <span class="label">시작일</span>
                        <input type="date" name="startDate" value="${esc(toDateValue(item.startDate))}">
                    </label>
                    <label class="field">
                        <span class="label">종료일</span>
                        <input type="date" name="endDate" value="${esc(toDateValue(item.endDate))}">
                    </label>
                </div>
                <label class="field">
                    <span class="label">담당 역할</span>
                    <input type="text" name="role" placeholder="역할" value="${esc(item.role)}">
                </label>
                <label class="field">
                    <span class="label">프로젝트 설명</span>
                    <textarea name="description" placeholder="설명">${esc(item.description)}</textarea>
                </label>
                <label class="field">
                    <span class="label">사용 기술</span>
                    <input type="text" name="techStack" placeholder="Java, Spring Boot, MyBatis" value="${esc(item.techStack)}">
                </label>
                <label class="field">
                    <span class="label">URL</span>
                    <input type="text" name="demoUrl" placeholder="URL" value="${esc(item.demoUrl)}">
                </label>
            </div>`;
    }

    function coverLetterCardHtml(item = {}) {
        return `
            <div class="item-card">${REMOVE_BTN}
                <div class="cover-head">
                    <label class="field">
                        <span class="label">항목 제목</span>
                        <input type="text" name="coverTitle" maxlength="200" placeholder="예: 지원동기" value="${esc(item.title)}">
                    </label>
                    <label class="field">
                        <span class="label">최대 글자 수</span>
                        <input type="number" name="coverMaxLength" min="1" placeholder="제한 없음" value="${esc(item.maxLength)}">
                    </label>
                </div>
                <label class="field">
                    <span class="label">내용</span>
                    <textarea name="coverContent" rows="8" placeholder="내용을 입력해 주세요">${esc(item.content)}</textarea>
                </label>
                <div class="char-count"></div>
            </div>`;
    }

    // ---------- 자기소개서 : 글자 수 ----------
    /** 글자 수 (이모지 등도 1자로) */
    function charLength(text, withSpace = true) {
        const v = withSpace ? text : text.replace(/\s/g, '');
        return [...v].length;
    }

    /** 카드의 글자 수 표시 갱신 (공백 포함 / 제외, 최대 글자 수 넘으면 강조) */
    function updateCharCount($card) {
        const text = $card.find('[name="coverContent"]').val() || '';
        const max = parseInt($card.find('[name="coverMaxLength"]').val(), 10);
        const withSpace = charLength(text);
        const noSpace = charLength(text, false);
        const limit = max > 0 ? ` / ${max.toLocaleString()}자` : '';
        $card.find('.char-count')
            .text(`공백 포함 ${withSpace.toLocaleString()}자${limit} · 공백 제외 ${noSpace.toLocaleString()}자`)
            .toggleClass('is-over', max > 0 && withSpace > max);
    }

    // ---------- 여러 개인 섹션 설정 ----------
    /**
     * label  : 화면 표시 이름
     * list   : 항목이 들어가는 목록
     * header : 한줄형 표 머리 (항목이 없으면 숨김)
     * html   : 항목 HTML 함수
     * from   : 상세 데이터에서 이 섹션 배열 꺼내기
     * unique : '추가' 로 가져올 때 같은 값이면 건너뛸 입력칸
     */
    const LIST_SECTIONS = {
        link:        { label: '외부 링크', list: '#linkList',        header: '#linkHeader',  html: linkRowHtml,         from: d => d.links,        unique: 'url' },
        skill:       { label: '기술 스택', list: '#skillList',       header: '#skillHeader', html: skillRowHtml,        from: d => d.skills,       unique: 'skillName' },
        coverLetter: { label: '자기소개서', list: '#coverLetterList',                       html: coverLetterCardHtml, from: d => d.coverLetters },
        education:   { label: '학력',     list: '#educationList',                           html: educationCardHtml,   from: d => d.educations },
        certificate: { label: '자격증',   list: '#certificateList',                         html: certificateCardHtml, from: d => d.certificates },
        experience:  { label: '경력',     list: '#experienceList',                          html: experienceCardHtml,  from: d => d.experiences },
        project:     { label: '프로젝트', list: '#projectList',                             html: projectCardHtml,     from: d => d.projects }
    };

    /**
     * 섹션에 항목 넣기 — [+ 추가] 버튼과 가져오기가 같이 사용
     * @returns 실제로 넣은 개수 (같은 값이라 건너뛴 건 제외)
     */
    function addItems(key, items, { replace = false, highlight = false } = {}) {
        const s = LIST_SECTIONS[key];
        const $list = $(s.list);
        if (replace) $list.empty();

        // '추가' 일 때 이미 있는 값(기술명·URL)은 건너뜀
        let list = items;
        if (s.unique && !replace) {
            const norm = v => String(v ?? '').trim().toLowerCase();
            const seen = new Set($list.find(`[name="${s.unique}"]`).toArray().map(el => norm(el.value)));
            list = items.filter(item => {
                const v = norm(item[s.unique]);
                if (!v) return true;              // 빈 항목([+ 추가])은 그대로
                if (seen.has(v)) return false;
                seen.add(v);
                return true;
            });
        }

        const $items = $($.parseHTML(list.map(s.html).join(''))).filter('.item-row, .item-card');
        $list.append($items);

        if (s.header) $(s.header).toggleClass('d-none', $list.children('.item-row').length === 0);
        if (key === 'experience') $items.find('input[name="isCurrent"]').trigger('change');
        if (key === 'coverLetter') $items.each((_, el) => updateCharCount($(el)));
        if (highlight) {
            $items.addClass('is-imported');
            setTimeout(() => $items.removeClass('is-imported'), 2000);
        }
        return $items.length;
    }

    // ---------- 항목 추가 ----------
    $('#addLink').on('click', () => addItems('link', [{}]));
    $('#addSkill').on('click', () => addItems('skill', [{}]));
    $('#addEducation').on('click', () => addItems('education', [{}]));
    $('#addCertificate').on('click', () => addItems('certificate', [{}]));
    $('#addExperience').on('click', () => addItems('experience', [{}]));
    $('#addProject').on('click', () => addItems('project', [{}]));
    $('#addCoverLetter').on('click', () => {
        addItems('coverLetter', [{}]);
        $('#coverLetterList .item-card').last().find('[name="coverTitle"]').trigger('focus');
    });
    // 빠른 추가 : 제목이 채워진 항목 추가 후 내용칸으로 이동
    $('.coverPresetBtn').on('click', function () {
        addItems('coverLetter', [{ title: this.dataset.title }]);
        $('#coverLetterList .item-card').last().find('[name="coverContent"]').trigger('focus');
    });

    // ---------- 저장 ----------
    $('#btnResumeSave').on('click', async function () {
        if (isSaving) return;                    // 중복 클릭 방지
        const $doc = $(document);
        const errors = [];

        const data = {
            resumeId: getVal($doc, 'resumeId'),
            title: getVal($doc, 'title'),
            profile: null,
            links: [],
            experiences: [],
            educations: [],
            certificates: [],
            projects: [],
            skills: [],
            coverLetters: []
        };

        // 1) 기본 정보
        if (getVal($doc, 'name') != null) {
            data.profile = {
                profileId: getVal($doc, 'profileId'),
                name: getVal($doc, 'name'),
                email: getVal($doc, 'email'),
                phone: getVal($doc, 'phone'),
                address: getVal($doc, 'address'),
                introduction: getVal($doc, 'introduction')
            };
        }

        // 2) 외부 링크 (URL 이 비어 있으면 건너뜀)
        $('#linkList .item-row').each(function () {
            const $item = $(this);
            const url = getVal($item, 'url');
            if (url === null) return;
            const linkType = getVal($item, 'linkType');
            if (linkType === null) {
                errors.push(`링크 [${url}] 유형을 선택해주세요.`);
            }
            data.links.push({
                linkId: getVal($item, 'linkId'),
                linkType: linkType,
                displayName: getVal($item, 'displayName'),
                url: url
            });
        });

        // 3) 기술 스택 (기술명이 비어 있으면 건너뜀)
        $('#skillList .item-row').each(function () {
            const $item = $(this);
            const skillName = getVal($item, 'skillName');
            if (skillName === null) return;
            data.skills.push({
                skillId: getVal($item, 'skillId'),
                skillName: skillName,
                category: getVal($item, 'category'),
                proficiency: getNumber($item, 'proficiency')
            });
        });

        // 3-1) 자기소개서 (제목·내용 모두 비어 있으면 건너뜀)
        $('#coverLetterList .item-card').each(function (i) {
            const $item = $(this);
            const title = getVal($item, 'coverTitle');
            const content = getVal($item, 'coverContent');
            if (title === null && content === null) return;
            const label = title || `${i + 1}번째 항목`;
            if (title === null) {
                errors.push(`자기소개서 [${label}] 제목을 입력해주세요.`);
            }
            const maxLength = getNumber($item, 'coverMaxLength');
            if (maxLength != null && content != null && charLength(content) > maxLength) {
                errors.push(`자기소개서 [${label}] 내용이 최대 글자 수(${maxLength}자)를 넘었습니다.`);
            }
            data.coverLetters.push({
                coverLetterId: getVal($item, 'coverLetterId'),
                title: title,
                content: content,
                maxLength: maxLength
            });
        });

        // 4) 학력 (학교명이 비어 있으면 건너뜀)
        $('#educationList .item-card').each(function () {
            const $item = $(this);
            const schoolName = getVal($item, 'schoolName');
            if (schoolName === null) return;
            const startDate = getVal($item, 'startDate');
            const endDate = getVal($item, 'endDate');
            if (isInvalidPeriod(startDate, endDate)) {
                errors.push(`학력 [${schoolName}] 종료일이 시작일보다 빠릅니다.`);
            }
            data.educations.push({
                educationId: getVal($item, 'educationId'),
                schoolName: schoolName,
                major: getVal($item, 'major'),
                degree: getVal($item, 'degree'),
                startDate: startDate,
                endDate: endDate,
                description: getVal($item, 'description')
            });
        });

        // 5) 자격증 (자격증명이 비어 있으면 건너뜀)
        $('#certificateList .item-card').each(function () {
            const $item = $(this);
            const certificateName = getVal($item, 'certificateName');
            if (certificateName === null) return;
            data.certificates.push({
                certificateId: getVal($item, 'certificateId'),
                certificateName: certificateName,
                issuer: getVal($item, 'issuer'),
                issuedDate: getVal($item, 'issuedDate'),
                description: getVal($item, 'description')
            });
        });

        // 6) 경력 (회사명이 비어 있으면 건너뜀, 재직중이면 퇴사일 없음)
        $('#experienceList .item-card').each(function () {
            const $item = $(this);
            const companyName = getVal($item, 'companyName');
            if (companyName === null) return;
            const isCurrent = $item.find('[name=isCurrent]').prop('checked') === true;
            const startDate = getVal($item, 'startDate');
            const endDate = isCurrent ? null : getVal($item, 'endDate');
            if (isInvalidPeriod(startDate, endDate)) {
                errors.push(`경력 [${companyName}] 종료일이 시작일보다 빠릅니다.`);
            }
            data.experiences.push({
                experienceId: getVal($item, 'experienceId'),
                companyName: companyName,
                position: getVal($item, 'position'),
                startDate: startDate,
                endDate: endDate,
                isCurrent: isCurrent,
                description: getVal($item, 'description')
            });
        });

        // 7) 프로젝트 (프로젝트명이 비어 있으면 건너뜀)
        $('#projectList .item-card').each(function () {
            const $item = $(this);
            const projectName = getVal($item, 'projectName');
            if (projectName === null) return;
            const startDate = getVal($item, 'startDate');
            const endDate = getVal($item, 'endDate');
            if (isInvalidPeriod(startDate, endDate)) {
                errors.push(`프로젝트 [${projectName}] 종료일이 시작일보다 빠릅니다.`);
            }
            data.projects.push({
                projectId: getVal($item, 'projectId'),
                projectName: projectName,
                startDate: startDate,
                endDate: endDate,
                role: getVal($item, 'role'),
                description: getVal($item, 'description'),
                techStack: getVal($item, 'techStack'),
                demoUrl: getVal($item, 'demoUrl')
            });
        });

        if (errors.length > 0) {
            alert(errors.join('\n'));
            return;
        }

        isSaving = true;
        const $btn = $(this).prop('disabled', true);
        callApi({
            url: '/api/resume/save',
            method: 'POST',
            data: data,
            success: result => {
                alert('저장 완료');
                location.href = '/web/resume/list';
            },
            error: err => {
                console.error('이력서 저장 실패', err);
                // 서버가 텍스트로 에러를 보내면 err.data, JSON 이면 err.message(서버 message 우선)
                const msg = typeof err.data === 'string' && err.data ? err.data : err.message;
                alert('저장 중 오류가 발생했습니다.\n' + msg);
            },
            complete: () => {
                isSaving = false;
                $btn.prop('disabled', false);
            }
        });
    });

    // ---------- 항목 삭제 ----------
    // 외부 링크 (남은 항목이 없으면 헤더 숨김)
    $('#linkList').on('click', '.removeBtn', function () {
        $(this).closest('.item-row').remove();
        $('#linkHeader').toggleClass('d-none', $('#linkList .item-row').length === 0);
    });

    // 기술 스택 (남은 항목이 없으면 헤더 숨김)
    $('#skillList').on('click', '.removeBtn', function () {
        $(this).closest('.item-row').remove();
        $('#skillHeader').toggleClass('d-none', $('#skillList .item-row').length === 0);
    });

    // 자기소개서
    $('#coverLetterList').on('click', '.removeBtn', function () {
        $(this).closest('.item-card').remove();
    });

    // 자기소개서 : 입력할 때마다 글자 수 갱신
    $('#coverLetterList').on('input', '[name="coverContent"], [name="coverMaxLength"]', function () {
        updateCharCount($(this).closest('.item-card'));
    });

    // 학력
    $('#educationList').on('click', '.removeBtn', function () {
        $(this).closest('.item-card').remove();
    });

    // 자격증
    $('#certificateList').on('click', '.removeBtn', function () {
        $(this).closest('.item-card').remove();
    });

    // 경력
    $('#experienceList').on('click', '.removeBtn', function () {
        $(this).closest('.item-card').remove();
    });

    // 프로젝트
    $('#projectList').on('click', '.removeBtn', function () {
        $(this).closest('.item-card').remove();
    });

    // ---------- 경력 : 재직중 체크 ----------
    /** 재직중이면 퇴사일을 비우고 입력 막기 */
    $('#experienceList').on('change', 'input[name="isCurrent"]', function () {
        const $endDate = $(this).closest('.item-card').find('input[name="endDate"]');
        if (this.checked) {
            $endDate.val('').prop('disabled', true);
        } else {
            $endDate.prop('disabled', false);
        }
    });

    // ---------- 다른 이력서에서 가져오기 ----------
    const importModal  = document.getElementById('importModal');
    const $resumeList  = $('#importResumeList');
    const $options     = $('#importOptions');
    const $sections    = $('#importSections');
    const $importAll   = $('#importAll');
    const $summary     = $('#importSummary');
    const $applyBtn    = $('#btnImportApply');

    /** 하나뿐인 섹션 (덮어쓰기만 가능) */
    const SINGLE_SECTIONS = {
        profile: { label: '인적 사항', count: d => PROFILE_FIELDS.some(k => d.profile?.[k]) ? 1 : 0 },
        intro:   { label: '간단 소개', count: d => d.profile?.introduction ? 1 : 0 }
    };

    /** 모달에 보이는 섹션 순서 (폼 순서와 같게) */
    const IMPORT_ORDER = ['profile', 'link', 'intro', 'coverLetter', 'skill', 'education', 'certificate', 'experience', 'project'];

    /** 이력서 목록 (한 번 불러오면 재사용) */
    let resumes = null;
    /** 불러온 이력서 상세 캐시 : 'resumeId' → detail */
    const resumeDetails = new Map();
    /** 고른 이력서 : { title, detail } */
    let picked = null;
    /** 이력서를 빠르게 바꿔 눌렀을 때 늦게 온 응답 무시용 */
    let pickSeq = 0;

    function sectionLabel(key) {
        return (SINGLE_SECTIONS[key] || LIST_SECTIONS[key]).label;
    }

    function sectionCount(key, detail) {
        if (SINGLE_SECTIONS[key]) return SINGLE_SECTIONS[key].count(detail);
        return (LIST_SECTIONS[key].from(detail) || []).length;
    }

    function checkedKeys() {
        return $sections.find('input[data-key]:checked').toArray().map(el => el.dataset.key);
    }

    function openImport() {
        picked = null;
        pickSeq++;
        $options.addClass('d-none');
        $applyBtn.prop('disabled', true);
        $summary.text('가져올 이력서를 선택해 주세요');
        importModal.showModal();

        if (resumes) renderResumeList();
        else loadResumeList();
    }

    // ① 이력서 목록
    function loadResumeList() {
        $resumeList.html('<li class="import-empty">이력서 목록을 불러오는 중…</li>');
        callApi({
            url: API.resumeList,
            success: list => {
                resumes = Array.isArray(list) ? list : [];
                renderResumeList();
            },
            error: err => {
                console.error('이력서 목록 불러오기 실패', err);
                $resumeList.html(`
                    <li class="import-empty">
                        이력서 목록을 불러오지 못했어요.
                        <button type="button" class="btn secondary sm" id="btnImportRetry">다시 시도</button>
                    </li>`);
            }
        });
    }

    function renderResumeList() {
        // 지금 작성 중인 이력서는 빼고 보여줌
        const currentId = getVal($(document), 'resumeId');
        const others = resumes.filter(r => String(r.resumeId) !== String(currentId));

        if (!others.length) {
            $resumeList.html('<li class="import-empty">가져올 수 있는 다른 이력서가 없어요.</li>');
            return;
        }
        $resumeList.html(others.map(r => `
            <li>
                <label class="resume-option">
                    <input type="radio" name="importResume" value="${esc(r.resumeId)}">
                    <span class="resume-option__title">${esc(r.title || '제목 없음')}</span>
                    <span class="resume-option__date">${esc(toDateValue(r.updatedAt))}</span>
                </label>
            </li>`).join(''));
    }

    // ② 이력서 고름 → 내용 불러와서 섹션 목록 그리기
    async function pickResume(id) {
        const seq = ++pickSeq;
        const key = String(id);
        const summary = resumes.find(r => String(r.resumeId) === key);

        picked = null;
        $options.removeClass('d-none');
        $sections.html('<li class="import-empty">이력서 내용을 불러오는 중…</li>');
        $applyBtn.prop('disabled', true);
        $summary.text('');

        let detail = resumeDetails.get(key);
        if (!detail) {
            try {
                detail = await callApi({
                    url: API.resumeDetail(key),
                    success: data => data || {},
                    error: err => { throw err; }
                });
                resumeDetails.set(key, detail);
            } catch (err) {
                console.error('이력서 불러오기 실패', err);
                if (seq !== pickSeq) return;
                $sections.html('<li class="import-empty">이력서 내용을 불러오지 못했어요. 다시 선택해 주세요.</li>');
                return;
            }
        }
        if (seq !== pickSeq) return;               // 그 사이 다른 이력서를 고름

        picked = { title: summary?.title || '이력서', detail };
        renderSections(detail);
    }

    function renderSections(detail) {
        $sections.html(IMPORT_ORDER.map(key => {
            const n = sectionCount(key, detail);
            const countText = !n ? '없음' : (SINGLE_SECTIONS[key] ? '있음' : `${n}개`);
            return `
                <li>
                    <label class="section-option${n ? '' : ' is-empty'}">
                        <input type="checkbox" data-key="${key}" ${n ? 'checked' : 'disabled'}>
                        <span class="section-option__name">${sectionLabel(key)}</span>
                        <span class="section-option__count">${countText}</span>
                    </label>
                </li>`;
        }).join(''));
        updateSummary();
    }

    /** 선택 개수 표시 + 전체 선택 체크 상태 + 가져오기 버튼 */
    function updateSummary() {
        if (!picked) return;
        const keys = checkedKeys();
        const total = keys.reduce((sum, k) => sum + sectionCount(k, picked.detail), 0);
        const $enabled = $sections.find('input[data-key]:not(:disabled)');

        $importAll
            .prop('disabled', !$enabled.length)
            .prop('checked', $enabled.length > 0 && keys.length === $enabled.length);
        $summary.text(total ? `${keys.length}개 섹션 · ${total}개 항목` : '가져올 항목을 선택해 주세요');
        $applyBtn.prop('disabled', !total);
    }

    // ③ 폼에 채우기
    function applyImport() {
        if (!picked) return;
        const keys = checkedKeys();
        if (!keys.length) return;
        const mode = $('input[name="importMode"]:checked').val();
        const d = picked.detail;

        // 바뀌거나 지워지는 내용이 있으면 확인
        const changed = [];
        if (keys.includes('profile') && PROFILE_FIELDS.some(k => d.profile?.[k] && getVal($form, k) != null)) {
            changed.push('인적 사항');
        }
        if (keys.includes('intro') && getVal($form, 'introduction') != null) {
            changed.push('간단 소개');
        }
        if (mode === 'replace') {
            keys.filter(k => LIST_SECTIONS[k] && $(LIST_SECTIONS[k].list).children().length)
                .forEach(k => changed.push(LIST_SECTIONS[k].label));
        }
        if (changed.length && !confirm(`지금 입력된 ${changed.join(', ')} 내용이 바뀌어요.\n가져올까요?`)) return;

        let added = 0;
        let skipped = 0;

        // 인적 사항 : 가져올 이력서에 값이 있는 칸만 덮어씀
        if (keys.includes('profile')) {
            PROFILE_FIELDS.forEach(k => {
                if (d.profile?.[k]) $form.find(`[name="${k}"]`).val(d.profile[k]);
            });
            added++;
        }
        if (keys.includes('intro')) {
            $form.find('[name="introduction"]').val(d.profile.introduction);
            added++;
        }

        // 여러 개인 섹션
        keys.filter(k => LIST_SECTIONS[k]).forEach(k => {
            const items = LIST_SECTIONS[k].from(d) || [];
            const n = addItems(k, items, { replace: mode === 'replace', highlight: true });
            added += n;
            skipped += items.length - n;
        });

        importModal.close();
        showNotice(`'${picked.title}'에서 ${added}개 항목을 가져왔어요.`
            + (skipped ? ` 이미 있는 기술·링크 ${skipped}개는 건너뛰었어요.` : '')
            + ' [저장]을 눌러야 반영돼요.');
    }

    function showNotice(text) {
        $('#importNoticeText').text(text);
        $('#importNotice').removeClass('d-none');
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    // 이벤트
    $('#btnImportOpen').on('click', openImport);
    $(importModal).on('click', '[data-close]', () => importModal.close());
    importModal.addEventListener('click', e => {
        if (e.target === importModal) importModal.close();     // 바깥(배경) 클릭
    });

    $resumeList.on('change', 'input[name="importResume"]', function () { pickResume(this.value); });
    $resumeList.on('click', '#btnImportRetry', loadResumeList);

    $sections.on('change', 'input[data-key]', updateSummary);
    $importAll.on('change', function () {
        $sections.find('input[data-key]:not(:disabled)').prop('checked', this.checked);
        updateSummary();
    });

    $applyBtn.on('click', applyImport);
    $('#importNoticeClose').on('click', () => $('#importNotice').addClass('d-none'));

    // ---------- 뒤로가기 ----------
    /** 이전 페이지가 있으면 뒤로, 직접 URL 로 들어온 경우엔 목록으로 */
    $('#btnBack').on('click', function () {
        if (document.referrer && history.length > 1) {
            history.back();
        } else {
            location.href = '/web/resume/list';
        }
    });

    // ---------- 시작 ----------
    // 저장돼 있던 '재직중' 경력은 처음부터 퇴사일 칸 막기
    $('#experienceList input[name="isCurrent"]').trigger('change');
    // 저장돼 있던 자기소개서 글자 수 표시
    $('#coverLetterList .item-card').each((_, el) => updateCharCount($(el)));
})();