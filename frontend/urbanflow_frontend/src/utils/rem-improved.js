function setRem() {
  const baseWidth = 1920;
  const html = document.documentElement;
  const clientWidth = html.clientWidth;
  const clientHeight = html.clientHeight;

  let scale;

  if (clientWidth <= 1366) {

    scale = Math.max(clientWidth / baseWidth, 0.75);
  } else if (clientWidth <= 1920) {

    scale = clientWidth / baseWidth;
  } else if (clientWidth <= 2560) {

    scale = 1 + (clientWidth - 1920) / (2560 - 1920) * 0.25;
  } else {

    scale = 1.25;
  }

  html.style.fontSize = `${scale * 100}px`;

  html.style.setProperty('--screen-width', `${clientWidth}px`);
  html.style.setProperty('--screen-height', `${clientHeight}px`);
  html.style.setProperty('--scale-factor', scale);

  html.className = html.className.replace(/screen-\w+/g, '');
  if (clientWidth <= 1366) {
    html.classList.add('screen-small');
  } else if (clientWidth <= 1600) {
    html.classList.add('screen-medium');
  } else if (clientWidth <= 2560) {
    html.classList.add('screen-large');
  } else {
    html.classList.add('screen-xlarge');
  }

  console.log(`📱 Screen: ${clientWidth}x${clientHeight}, Scale: ${scale.toFixed(3)}, Font: ${(scale * 100).toFixed(1)}px`);
}

let resizeTimer;
function debouncedSetRem() {
  clearTimeout(resizeTimer);
  resizeTimer = setTimeout(setRem, 100);
}

setRem();
window.addEventListener('resize', debouncedSetRem);
window.addEventListener('DOMContentLoaded', setRem);

window.addEventListener('orientationchange', () => {
  setTimeout(setRem, 500);
});
