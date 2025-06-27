// src/utils/rem.js

function setRem() {
  const baseWidth = 1920; // 设计稿宽度
  const html = document.documentElement;
  const clientWidth = html.clientWidth;
  const scale = clientWidth / baseWidth;
  html.style.fontSize = `${scale * 100}px`;
}
setRem();
window.addEventListener('resize', setRem);
window.addEventListener('DOMContentLoaded', setRem);


// rem.ts - 自动设置 rem 根字体大小，兼容宽高适配
// function setRem() {
//   const baseWidth = 1920;
//   const baseHeight = 1080;
//   const minScale = 0.7; // ✅ 页面最小缩放比例（等于最小宽度 1344）

//   const html = document.documentElement;
//   const clientWidth = html.clientWidth;
//   const clientHeight = html.clientHeight;

//   // 分别计算宽高缩放比
//   const scaleW = clientWidth / baseWidth;
//   const scaleH = clientHeight / baseHeight;

//   // 使用较小方向的缩放因子
//   const scale = Math.min(scaleW, scaleH);

//   // 限制最小缩放比例
//   const finalScale = Math.max(scale, minScale);

//   // 设置 html 的根字体
//   html.style.fontSize = `${finalScale * 100}px`;
// }

// // 初始执行一次
// setRem();

// // 页面尺寸变更时动态调整
// window.addEventListener('resize', setRem);
// window.addEventListener('DOMContentLoaded', setRem);

// // 导出供组件内部调用（如果需要）
// export default setRem;
