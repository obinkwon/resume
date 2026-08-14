/* 이력서 폼 js */

function addBlock(listId, html) {
    document.getElementById(listId).insertAdjacentHTML('beforeend', html);
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
            name: document.querySelector('[name=name]').value,
            email: document.querySelector('[name=email]').value,
            phone: document.querySelector('[name=phone]').value,
            summary: document.querySelector('[name=summary]').value,

            experiences: [],
            educations: [],
            projects: [],
            skills: []
        };

        $('#experienceList .item').each(function(index, item) {
            data.experiences.push({
                company: item.querySelector('[name=company]').value,
                position: item.querySelector('[name=position]').value,
                startDate: item.querySelector('[name=startDate]').value,
                endDate: item.querySelector('[name=endDate]').value,
                description: item.querySelector('[name=description]').value
            });
        });

        $('#educationList .item').each(function(index, item) {
            data.educations.push({
                school: item.querySelector('[name=school]').value,
                major: item.querySelector('[name=major]').value,
                startDate: item.querySelector('[name=eduStart]').value,
                endDate: item.querySelector('[name=eduEnd]').value
            });
        });

        $('#projectList .item').each(function(index, item) {
            data.projects.push({
                projectName: item.querySelector('[name=projectName]').value,
                description: item.querySelector('[name=projectDescription]').value,
                techStack: item.querySelector('[name=techStack]').value
            });
        });

        $('#skillList input[name=skill]').each(function(index, input) {
            data.skills.push({
                skillName: input.value
            });
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