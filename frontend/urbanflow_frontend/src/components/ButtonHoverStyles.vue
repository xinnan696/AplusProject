<template>
  <div></div>
</template>

<script setup lang="ts">

</script>

<style lang="scss">

.btn-hover-icon {
  transition: all 0.3s ease;
  border-radius: 0.08rem;
  overflow: visible;
  box-shadow: none !important;
  filter: none !important;

  &:hover {
    color: #00D4F8;
    transform: translateY(-1px) scale(1.02);
    box-shadow: none !important;
    filter: none !important;

    .simple-tooltip {
      opacity: 1 !important;
      visibility: visible !important;
      transform: translateX(-50%) !important;
    }
  }

  &:active {
    transform: translateY(0) scale(0.98);
  }
}


.btn-hover-switch {
  .slider {
    transition: all 0.3s ease;
    background: #2a2a3a;
    border: 0.02rem solid #444;
    overflow: hidden;
    box-shadow: none !important;
    filter: none !important;
  }

  .slider::before {
    background: #ffffff !important;
    transition: all 0.3s ease;
  }

  input:checked + .slider {
    background: #00b4d8;
    border-color: #00d4f8;
    box-shadow: none !important;
    filter: none !important;
  }

  input:checked + .slider::before {
    transform: translateX(0.23rem);
    background: #ffffff !important;
  }

  input:focus + .slider {
    outline: none;
    border-color: #00d4f8;
    box-shadow: none !important;
    filter: none !important;
  }

  input:active + .slider::before {
    width: 0.28rem;
    transition: all 0.1s ease;
  }
}


.btn-hover-circle {
  transition: all 0.3s ease;
  overflow: visible;
  background: #00B4D8;
  box-shadow: none !important;
  filter: none !important;

  &:hover {
    transform: translateY(-1px) scale(1.02);
    box-shadow: none !important;
    filter: none !important;

    .simple-tooltip {
      opacity: 1 !important;
      visibility: visible !important;
      transform: translateX(-50%) !important;
    }
  }

  &:active {
    transform: translateY(0) scale(0.98);
  }
}


.btn-hover-solid {
  background: #00B4D8;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  box-shadow: none !important;
  filter: none !important;

  &:hover {
    transform: translateY(-1px);
    box-shadow: none !important;
    filter: none !important;
  }

  &:active {
    transform: translateY(0);
  }
}


.simple-tooltip {
  position: absolute;
  left: 50%;
  top: calc(100% + 8px);
  transform: translateX(-50%);
  padding: 6px 10px;
  background: rgba(45, 45, 45, 0.95) !important;
  color: #ffffff !important;
  font-size: 12px !important;
  font-weight: 500 !important;
  font-family: 'Inter', 'Segoe UI', 'Arial', 'Helvetica Neue', 'Roboto', sans-serif !important;
  line-height: 1.2 !important;
  border-radius: 4px;
  white-space: nowrap;
  opacity: 0;
  visibility: hidden;
  transition: all 0.2s ease;
  z-index: 99999;
  pointer-events: none;
  min-height: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  letter-spacing: 0.01em;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}


.panel-slide-down {
  animation: slideDown 0.4s cubic-bezier(0.4, 0.0, 0.2, 1);
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-15px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}


.btn-active {
  color: #00D4F8;
  transform: translateY(-1px) scale(1.02);
  box-shadow: none !important;
  filter: none !important;
}
</style>
