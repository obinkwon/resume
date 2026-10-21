/* =========================================================
   resume-form.js  (이력서 작성 폼)
   ========================================================= */
(() => {
    'use strict';

    // ---------- 유틸 ----------
    function addBlock(listId, html) {
        document.getElementById(listId).insertAdjacentHTML('beforeend', html);
    }

    /** 객체의 모든 값이 빈 문자열(공백만 있는 경우 포함)인지 */
    function isAllEmpty(obj) {
        return Object.values(obj).every(v => !v || v.trim() === '');
    }

    // ---------- 상태 ----------
    /** 저장 중복 클릭 방지 (핸들러 밖에 있어야 클릭 간에 값이 유지됨) */
    let isSaving = false;

    // ---------- 항목 추가 ----------
    // 외부 링크
    $('#addLink').on('click', function () {
        $('#linkHeader').removeClass('d-none');
        const linkHtml = `
            <div class="item-row link-cols">
                <select name="linkType">
                    <option value="">유형 선택</option>
                    <option value="github">GitHub</option>
                    <option value="linkedin">LinkedIn</option>
                    <option value="blog">블로그</option>
                    <option value="portfolio">포트폴리오</option>
                    <option value="website">웹사이트</option>
                    <option value="etc">기타</option>
                </select>
                <input type="text" name="displayName" placeholder="예: 기술 블로그">
                <input type="url" name="url" placeholder="https://">
                <button type="button" class="btn icon danger-ghost removeBtn" aria-label="삭제">
                    <i class="icon icon-close"></i>
                </button>
            </div>
        `;
        $('#linkList').append(linkHtml);
    });

    // 기술 스택
    $('#addSkill').on('click', function () {
        $('#skillHeader').removeClass('d-none');
        const skillHtml = `
            <div class="item-row">
                <input type="text" name="skillName" placeholder="기술 입력">
                <input type="text" name="category" placeholder="카테고리 (예: Backend)">
                <select name="proficiency">
                    <option value="">숙련도 선택</option>
                    <option value="1">1 - 초급</option>
                    <option value="2">2</option>
                    <option value="3">3 - 중급</option>
                    <option value="4">4</option>
                    <option value="5">5 - 고급</option>
                </select>
                <button type="button" class="btn icon danger-ghost removeBtn" aria-label="삭제">
                    <i class="icon icon-close"></i>
                </button>
            </div>
        `;
        $('#skillList').append(skillHtml);
    });

    // 학력
    $('#addEducation').on('click', function () {
        const educationHtml = `
            <div class="item-card">
                <button type="button" class="btn icon danger-ghost removeBtn" aria-label="삭제">
                    <i class="icon icon-close"></i>
                </button>
                <label class="field">
                    <span class="label">학교명</span>
                    <input type="text" name="schoolName" placeholder="학교">
                </label>
                <label class="field">
                    <span class="label">전공</span>
                    <input type="text" name="major" placeholder="전공">
                </label>
                <label class="field">
                    <span class="label">학위</span>
                    <input type="text" name="degree" placeholder="예: 학사">
                </label>
                <div class="date-row">
                    <label class="field">
                        <span class="label">입학일</span>
                        <input type="date" name="startDate">
                    </label>
                    <label class="field">
                        <span class="label">졸업일</span>
                        <input type="date" name="endDate">
                    </label>
                </div>
                <label class="field">
                    <span class="label">학력 설명</span>
                    <textarea name="description" placeholder="학력 설명" rows="3"></textarea>
                </label>
            </div>
        `;
        $('#educationList').append(educationHtml);
    });

    // 자격증
    $('#addCertificate').on('click', function () {
        const certificateHtml = `
            <div class="item-card">
                <button type="button" class="btn icon danger-ghost removeBtn" aria-label="삭제">
                    <i class="icon icon-close"></i>
                </button>
                <label class="field">
                    <span class="label">자격증명</span>
                    <input type="text" name="certificateName" placeholder="예: 정보처리기사">
                </label>
                <div class="date-row">
                    <label class="field">
                        <span class="label">발급 기관</span>
                        <input type="text" name="issuer" placeholder="예: 한국산업인력공단">
                    </label>
                    <label class="field">
                        <span class="label">취득일</span>
                        <input type="date" name="issuedDate">
                    </label>
                </div>
                <label class="field">
                    <span class="label">자격증 설명</span>
                    <textarea name="description" placeholder="자격증 설명" rows="3"></textarea>
                </label>
            </div>
        `;
        $('#certificateList').append(certificateHtml);
    });

    // 경력
    $('#addExperience').on('click', function () {
        const experienceHtml = `
            <div class="item-card">
                <button type="button" class="btn icon danger-ghost removeBtn" aria-label="삭제">
                    <i class="icon icon-close"></i>
                </button>
                <label class="field">
                    <span class="label">회사명</span>
                    <input type="text" name="companyName" placeholder="회사명">
                </label>
                <label class="field">
                    <span class="label">직무</span>
                    <input type="text" name="position" placeholder="직무">
                </label>
                <div class="date-row">
                    <label class="field">
                        <span class="label">입사일</span>
                        <input type="date" name="startDate">
                    </label>
                    <label class="field">
                        <span class="label">퇴사일</span>
                        <input type="date" name="endDate">
                    </label>
                </div>
                <label class="check">
                    <input type="checkbox" name="isCurrent">
                    <span>재직중</span>
                </label>
                <label class="field">
                    <span class="label">업무 내용</span>
                    <textarea name="description" placeholder="업무 내용"></textarea>
                </label>
            </div>
        `;
        $('#experienceList').append(experienceHtml);
    });

    // 프로젝트
    $('#addProject').on('click', function () {
        const projectHtml = `
            <div class="item-card">
                <button type="button" class="btn icon danger-ghost removeBtn" aria-label="삭제">
                    <i class="icon icon-close"></i>
                </button>
                <label class="field">
                    <span class="label">프로젝트명</span>
                    <input type="text" name="projectName" placeholder="프로젝트명">
                </label>
                <div class="date-row">
                    <label class="field">
                        <span class="label">시작일</span>
                        <input type="date" name="startDate">
                    </label>
                    <label class="field">
                        <span class="label">종료일</span>
                        <input type="date" name="endDate">
                    </label>
                </div>
                <label class="field">
                    <span class="label">담당 역할</span>
                    <input type="text" name="role" placeholder="역할">
                </label>
                <label class="field">
                    <span class="label">프로젝트 설명</span>
                    <textarea name="description" placeholder="설명"></textarea>
                </label>
                <label class="field">
                    <span class="label">사용 기술</span>
                    <input type="text" name="techStack" placeholder="Java, Spring Boot, MyBatis">
                </label>
                <label class="field">
                    <span class="label">URL</span>
                    <input type="text" name="demoUrl" placeholder="URL">
                </label>
            </div>
        `;
        $('#projectList').append(projectHtml);
    });

    // ---------- 저장 ----------
    $('#btnResumeSave').on('click', async function () {
        console.log('btnResumeSave')
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
            skills: []
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

    // ---------- 뒤로가기 ----------
    /** 이전 페이지가 있으면 뒤로, 직접 URL 로 들어온 경우엔 목록으로 */
    $('#btnBack').on('click', function () {
        if (document.referrer && history.length > 1) {
            history.back();
        } else {
            location.href = '/web/resume/list';
        }
    });
})();