/* 이력서 폼 js */

function addBlock(listId, html) {
    document.getElementById(listId).insertAdjacentHTML('beforeend', html);
}

// 객체의 모든 값이 빈 문자열(공백만 있는 경우 포함)인지 체크하는 헬퍼
function isAllEmpty(obj) {
    return Object.values(obj).every(v => !v || v.trim() === '');
}

$(function(){
    // 기술 추가
    $('#addSkill').on('click', function() {
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
    // 학력 추가
    $('#addEducation').on('click', function() {
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
    $('#addExperience').on('click', function(){
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
    $('#addProject').on('click', function(){
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
    $('#btnResumeSave').on('click', async function(){
        let isSaving = false;
        if (isSaving) return; // 중복 클릭 방지
        const $doc = $(document);
        const errors = [];

        const data = {
            resumeId: getVal($doc, 'resumeId'),
            title: getVal($doc, 'title'),
            profile: {
                profileId: getVal($doc, 'profileId'),
                name: getVal($doc, 'name'),
                email: getVal($doc, 'email'),
                phone: getVal($doc, 'phone'),
                address: getVal($doc, 'address'),
                introduction: getVal($doc, 'introduction')
            },
            experiences: [],
            educations: [],
            projects: [],
            skills: []
        };

        // 기술 스택
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

        // 학력
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

        // 경력
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

        // 프로젝트
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
        try {
            const response = await fetch('/api/resume/save', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });

            // 4xx/5xx도 fetch는 성공으로 처리하므로 직접 확인
            if (!response.ok) {
                const msg = await response.text();
                throw new Error(msg || `HTTP ${response.status}`);
            }

            // 응답 본문이 비어 있어도 JSON 파싱 에러가 나지 않도록
            const text = await response.text();
            const result = text ? JSON.parse(text) : null;

            // 신규 저장 후 다시 저장할 때 insert가 중복되지 않도록 발급된 ID 반영 (응답에 resumeId가 있을 경우)
            if (result && result.resumeId) {
                $('[name=resumeId]').val(result.resumeId);
            }

            alert('저장 완료');
            // location.href = '/web/resume/main';
        } catch (e) {
            console.error('이력서 저장 실패', e);
            alert('저장 중 오류가 발생했습니다.\n' + e.message);
        } finally {
            isSaving = false;
        }
    });
    // 기술 제거
    $('#skillList').on('click', '.removeBtn', function() {
        $(this).closest('.item-row').remove();

        // 남은 항목이 없으면 헤더 숨김
        $('#skillHeader').toggleClass('d-none', $('#skillList .item-row').length === 0);
    });
    // 학력 제거
    $('#educationList').on('click', '.removeBtn', function() {
        $(this).closest('.item-card').remove();
    });
    // 경력 제거
    $('#experienceList').on('click', '.removeBtn', function() {
        $(this).closest('.item-card').remove();
    });
    $('#experienceList').on('change', 'input[name="isCurrent"]', function () {
        const $endDate = $(this).closest('.item-card').find('input[name="endDate"]');
        if (this.checked) {
            $endDate.val('').prop('disabled', true);
        } else {
            $endDate.prop('disabled', false);
        }
    });
    // 프로젝트 제거
    $('#projectList').on('click', '.removeBtn', function() {
        $(this).closest('.item-card').remove();
    });
    $('#btnBack').on('click', function () {
        // 이전 페이지가 있으면 뒤로, 직접 URL로 들어온 경우엔 목록으로
        if (document.referrer && history.length > 1) {
            history.back();
        } else {
            location.href = '/web/resume/main';
        }
    });
});