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
                <button type="button" class="removeBtn" aria-label="삭제">&times;</button>
            </div>
        `;
        $('#skillList').append(skillHtml);
    });
    // 학력 추가
    $('#addEducation').on('click', function() {
        const educationHtml = `
            <div class="item-card">
                <button type="button" class="removeBtn" aria-label="삭제">&times;</button>
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
                <button type="button" class="removeBtn" aria-label="삭제">&times;</button>
                <label class="field">
                    <span class="label">회사명</span>
                    <input type="text" name="company" placeholder="회사명">
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
                <button type="button" class="removeBtn" aria-label="삭제">&times;</button>
                <label class="field">
                    <span class="label">프로젝트명</span>
                    <input type="text" name="projectName" placeholder="프로젝트명">
                </label>
                <label class="field">
                    <span class="label">프로젝트 설명</span>
                    <textarea name="projectDescription" placeholder="설명"></textarea>
                </label>
                <label class="field">
                    <span class="label">사용 기술</span>
                    <input type="text" name="techStack" placeholder="Java, Spring Boot, MyBatis">
                </label>
            </div>
        `;
        $('#projectList').append(projectHtml);
    });
    $('#btnResumeSave').on('click', async function(){
        const data = {
            title: document.querySelector('[name=title]').value,
            profile: {
                name: document.querySelector('[name=name]').value,
                email: document.querySelector('[name=email]').value,
                phone: document.querySelector('[name=phone]').value,
                address: document.querySelector('[name=address]').value,
                introduction: document.querySelector('[name=introduction]').value
            },
            experiences: [],
            educations: [],
            projects: [],
            skills: []
        };
        // 기술 스택 추가
        $('#skillList .item-row').each(function(index, item) {
            const skillName = $(item).find('[name=skillName]').val().trim();
            const category = $(item).find('[name=category]').val().trim();
            const proficiency = $(item).find('[name=proficiency]').val();
            if (skillName !== '') {
                data.skills.push({
                    skillName: skillName,
                    category: category !== '' ? category : null,
                    proficiency: proficiency !== '' ? Number(proficiency) : null
                });
            }
        });
        // 학력 추가
        $('#educationList .item-card').each(function(index, item) {
            const schoolName = $(item).find('[name=schoolName]').val().trim();
            const major = $(item).find('[name=major]').val().trim();
            const degree = $(item).find('[name=degree]').val().trim();
            const startDate = $(item).find('[name=startDate]').val();
            const endDate = $(item).find('[name=endDate]').val();
            const description = $(item).find('[name=description]').val().trim();
            // 학교명이 있는 경우만 저장
            if (schoolName !== '') {
                data.educations.push({
                    schoolName: schoolName,
                    major: major !== '' ? major : null,
                    degree: degree !== '' ? degree : null,
                    startDate: startDate !== '' ? startDate : null,
                    endDate: endDate !== '' ? endDate : null,
                    description: description !== '' ? description : null
                });
            }
        });
        // 경력 추가
        $('#experienceList .item-card').each(function (index, item) {
            const companyName = $(item).find('[name=companyName]').val().trim();
            const position = $(item).find('[name=position]').val().trim();
            const startDate = $(item).find('[name=startDate]').val();
            const endDate = $(item).find('[name=endDate]').val();
            const isCurrent = $(item).find('[name=isCurrent]').prop('checked');
            const description = $(item).find('[name=description]').val().trim();
            // 회사명이 있는 경우만 저장
            if (companyName !== '') {
                data.experiences.push({
                    companyName: companyName,
                    position: position !== '' ? position : null,
                    startDate: startDate !== '' ? startDate : null,
                    endDate: (!isCurrent && endDate !== '') ? endDate : null,
                    isCurrent: isCurrent,
                    description: description !== '' ? description : null,
                    sortOrder: data.experiences.length
                });
            }
        });

        $('#projectList .item-card').each(function(index, item) {
            const project = {
                projectName: item.querySelector('[name=projectName]').value,
                description: item.querySelector('[name=projectDescription]').value,
                techStack: item.querySelector('[name=techStack]').value
            };
            if (!isAllEmpty(project)) {
                data.projects.push(project);
            }
        });

        const response = await fetch('/api/resume/save', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        });

        const result = await response.json();

        alert('저장 완료');

        //location.reload();
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
});