// src/utils/rem.js

function setRem() {
  const baseWidth = 1920;
  const html = document.documentElement;
  const clientWidth = html.clientWidth;
  const scale = clientWidth / baseWidth;
  html.style.fontSize = `${scale * 100}px`;
}
setRem();
window.addEventListener('resize', setRem);
window.addEventListener('DOMContentLoaded', setRem);


