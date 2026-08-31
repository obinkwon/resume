/* 이력서 폼 js */

function addBlock(listId, html) {
    document.getElementById(listId).insertAdjacentHTML('beforeend', html);
}

// 객체의 모든 값이 빈 문자열(공백만 있는 경우 포함)인지 체크하는 헬퍼
function isAllEmpty(obj) {
    return Object.values(obj).every(v => !v || v.trim() === '');
}

$(function(){
    $('#addExperience').on('click', function(){
        addBlock('experienceList', '<div class="item"><input placeholder="회사명"></div>');
    });
    $('#addEducation').on('click', function(){
        addBlock('educationList', '<div class="item"><input placeholder="학교"></div>');
    });
    $('#addProject').on('click', function(){
        addBlock('projectList', '<div class="item"><input placeholder="프로젝트명"></div>');
    });
    $('#addSkill').on('click', function(){
        addBlock('skillList', '<input placeholder="기술 입력">');
    });
    $('#btnResumeSave').on('click', async function(){
        const data = {
            title: document.querySelector('[name=title]').value,
            profile: {
                name: document.querySelector('[name=name]').value,
                email: document.querySelector('[name=email]').value,
                phone: document.querySelector('[name=phone]').value
            },
            experiences: [],
            educations: [],
            projects: [],
            skills: []
        };

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

        $('#educationList .item').each(function(index, item) {
            const education = {
                school: item.querySelector('[name=school]').value,
                major: item.querySelector('[name=major]').value,
                startDate: item.querySelector('[name=eduStart]').value,
                endDate: item.querySelector('[name=eduEnd]').value
            };
            if (!isAllEmpty(education)) {
                data.educations.push(education);
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

        $('#skillList input[name=skill]').each(function(index, input) {
            const value = input.value.trim();
            if (value !== '') {
                data.skills.push({ skillName: value });
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
});