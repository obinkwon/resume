/* 이력서 폼 js */

function addBlock(listId, html) {
    document.getElementById(listId).insertAdjacentHTML('beforeend', html);
}

document.getElementById('addExperience').onclick = () => addBlock('experienceList', '<div class="item"><input placeholder="회사명"></div>');
document.getElementById('addEducation').onclick = () => addBlock('educationList', '<div class="item"><input placeholder="학교"></div>');
document.getElementById('addProject').onclick = () => addBlock('projectList', '<div class="item"><input placeholder="프로젝트명"></div>');
document.getElementById('addSkill').onclick = () => addBlock('skillList', '<input placeholder="기술 입력">');

document.getElementById('resumeForm').addEventListener('submit', async function(e) {
    e.preventDefault();

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

    document.querySelectorAll('#experienceList .item').forEach(item => {
        data.experiences.push({
            company: item.querySelector('[name=company]').value,
            position: item.querySelector('[name=position]').value,
            startDate: item.querySelector('[name=startDate]').value,
            endDate: item.querySelector('[name=endDate]').value,
            description: item.querySelector('[name=description]').value
        });
    });

    document.querySelectorAll('#educationList .item').forEach(item => {
        data.educations.push({
            school: item.querySelector('[name=school]').value,
            major: item.querySelector('[name=major]').value,
            startDate: item.querySelector('[name=eduStart]').value,
            endDate: item.querySelector('[name=eduEnd]').value
        });
    });

    document.querySelectorAll('#projectList .item').forEach(item => {
        data.projects.push({
            projectName: item.querySelector('[name=projectName]').value,
            description: item.querySelector('[name=projectDescription]').value,
            techStack: item.querySelector('[name=techStack]').value
        });
    });

    document.querySelectorAll('#skillList input[name=skill]').forEach(input => {
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

    location.href = '/template/' + result.resumeId;
});