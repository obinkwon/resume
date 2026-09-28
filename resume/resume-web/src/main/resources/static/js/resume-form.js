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
        const skillHtml = `
            <div class="skill-item">
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
                <button type="button" class="removeBtn">삭제</button>
            </div>
        `;
        $('#skillList').append(skillHtml);
    });
    // 학력 추가
    $('#addEducation').on('click', function() {
        const educationHtml = `
            <div class="item">
                <input type="text" name="schoolName" placeholder="학교">
                <input type="text" name="major" placeholder="전공">
                <input type="text" name="degree" placeholder="학위 (예: 학사)">
                <input type="date" name="startDate">
                <input type="date" name="endDate">
                <textarea name="description" placeholder="학력 설명" rows="3"></textarea>
                <button type="button" class="removeBtn">삭제</button>
            </div>
        `;
        $('#educationList').append(educationHtml);
    });
    $('#addExperience').on('click', function(){
        addBlock('experienceList', '<div class="item"><input placeholder="회사명"></div>');
    });
    $('#addProject').on('click', function(){
        addBlock('projectList', '<div class="item"><input placeholder="프로젝트명"></div>');
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
        $('#skillList .skill-item').each(function(index, item) {
            const skillName = $(item).find('[name=skillName]').val().trim();
            const category = $(item).find('[name=category]').val().trim();
            const proficiency = $(item).find('[name=proficiency]').val();
            if (skillName !== '') {
                data.skills.push({
                    skillName: skillName,
                    category: category !== '' ? category : null,
                    proficiency: proficiency !== '' ? Number(proficiency) : null,
                    sortOrder: index
                });
            }
        });
        // 학력 추가
        $('#educationList .item').each(function(index, item) {
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
                    description: description !== '' ? description : null,
                    sortOrder: index
                });
            }
        });

        $('#experienceList .item').each(function(index, item) {
            const experience = {
                company: item.querySelector('[name=company]').value,
                position: item.querySelector('[name=position]').value,
                startDate: item.querySelector('[name=startDate]').value,
                endDate: item.querySelector('[name=endDate]').value,
                description: item.querySelector('[name=description]').value
            };
            if (!isAllEmpty(experience)) {
                data.experiences.push(experience);
            }
        });

        $('#projectList .item').each(function(index, item) {
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
        $(this).closest('.skill-item').remove();
    });
    // 학력 제거
    $('#educationList').on('click', '.removeBtn', function() {
        $(this).closest('.item').remove();
    });
});