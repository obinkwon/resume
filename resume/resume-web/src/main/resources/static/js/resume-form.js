/* 이력서 폼 js */

function addBlock(listId, html) {
    document.getElementById(listId)
        .insertAdjacentHTML('beforeend', html);
}

document.getElementById('addExperience').onclick = () =>
    addBlock('experienceList',
        '<div class="item"><input placeholder="회사명"></div>');

document.getElementById('addEducation').onclick = () =>
    addBlock('educationList',
        '<div class="item"><input placeholder="학교"></div>');

document.getElementById('addProject').onclick = () =>
    addBlock('projectList',
        '<div class="item"><input placeholder="프로젝트명"></div>');

document.getElementById('addSkill').onclick = () =>
    addBlock('skillList',
        '<input placeholder="기술 입력">');