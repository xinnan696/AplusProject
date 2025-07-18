<template>
  <div></div>
</template>

<script setup lang="ts">

</script>

<style lang="scss">

.btn-hover-icon {
  transition: all 0.3s cubic-bezier(0.4, 0.0, 0.2, 1);
  border-radius: 0.08rem;
  overflow: visible;

  &:hover {
    color: #00D4F8;
    transform: translateY(-2px) scale(1.1);
    background: rgba(0, 180, 216, 0.1);
    box-shadow: 0 4px 12px rgba(0, 180, 216, 0.4);

    .simple-tooltip {
      opacity: 1 !important;
      visibility: visible !important;
      transform: translateX(-50%) !important;
    }
  }

  &:active {
    transform: translateY(0) scale(0.95);
  }
}


.btn-hover-switch {
  &:hover .slider {
    box-shadow: 0 0 0.15rem rgba(0, 180, 216, 0.6);
    transform: scale(1.05);
  }

  .slider {
    transition: all 0.4s cubic-bezier(0.4, 0.0, 0.2, 1);
    background: linear-gradient(135deg, #2a2a3a 0%, #1a1a2a 100%);
    border: 0.02rem solid #444;
    overflow: hidden;

    &::after {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: linear-gradient(45deg, transparent 30%, rgba(255,255,255,0.1) 50%, transparent 70%);
      transform: translateX(-100%);
      transition: transform 0.6s ease;
    }

    &:hover::after {
      transform: translateX(100%);
    }
  }

  .slider::before {
    background: #ffffff !important;
    transition: all 0.4s cubic-bezier(0.68, -0.55, 0.265, 1.55);
    box-shadow:
      0 0.02rem 0.06rem rgba(0, 0, 0, 0.3),
      0 0 0 0.02rem rgba(255, 255, 255, 0.1) inset;
  }

  input:checked + .slider {
    background: linear-gradient(135deg, #00b4d8 0%, #0090aa 100%);
    border-color: #00d4f8;
    box-shadow:
      0 0 0.2rem rgba(0, 180, 216, 0.5),
      0 0.02rem 0.08rem rgba(0, 180, 216, 0.3);
  }

  input:checked + .slider::before {
    transform: translateX(0.23rem) rotate(360deg);
    background: #ffffff !important;
    box-shadow:
      0 0.02rem 0.08rem rgba(0, 0, 0, 0.3),
      0 0 0.08rem rgba(0, 180, 216, 0.4),
      0 0 0 0.02rem rgba(255, 255, 255, 0.8) inset;
  }

  input:focus + .slider {
    outline: none;
    box-shadow:
      0 0 0.15rem rgba(0, 180, 216, 0.6),
      0 0 0.25rem rgba(0, 180, 216, 0.3);
  }

  input:active + .slider::before {
    width: 0.28rem;
    transition: all 0.1s ease;
  }
}


.btn-hover-circle {
  transition: all 0.3s cubic-bezier(0.4, 0.0, 0.2, 1);
  overflow: visible;
  background: linear-gradient(135deg, #00B4D8 0%, #0090aa 100%);
  box-shadow: 0 2px 8px rgba(0, 180, 216, 0.4);

  &:hover {
    background: linear-gradient(135deg, #00D4F8 0%, #00a8cc 100%);
    transform: translateY(-2px) scale(1.05);
    box-shadow: 0 4px 16px rgba(0, 180, 216, 0.6);

    .simple-tooltip {
      opacity: 1 !important;
      visibility: visible !important;
      transform: translateX(-50%) !important;
    }
  }

  &:active {
    transform: translateY(0) scale(0.95);
  }
}


.btn-hover-solid {
  background: linear-gradient(135deg, #00B4D8 0%, #00D4F8 50%, #0090aa 100%);
  transition: all 0.3s cubic-bezier(0.4, 0.0, 0.2, 1);
  position: relative;
  overflow: hidden;
  box-shadow:
    0 4px 12px rgba(0, 180, 216, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
    transition: left 0.5s ease;
  }

  &:hover {
    background: linear-gradient(135deg, #00D4F8 0%, #00F0FF 50%, #00a8cc 100%);
    transform: translateY(-1px);
    box-shadow:
      0 6px 20px rgba(0, 180, 216, 0.4),
      inset 0 1px 0 rgba(255, 255, 255, 0.3);

    &::before {
      left: 100%;
    }
  }

  &:active {
    transform: translateY(0);
    box-shadow:
      0 2px 8px rgba(0, 180, 216, 0.4),
      inset 0 1px 0 rgba(255, 255, 255, 0.2);
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
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  min-height: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  letter-spacing: 0.01em;
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


.shimmer-effect {
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: -50%;
    left: -50%;
    width: 200%;
    height: 200%;
    background: linear-gradient(45deg, transparent 30%, rgba(255, 255, 255, 0.1) 50%, transparent 70%);
    animation: shimmer 3s infinite;
  }
}

@keyframes shimmer {
  0% { transform: translateX(-100%) translateY(-100%) rotate(45deg); }
  50% { transform: translateX(100%) translateY(100%) rotate(45deg); }
  100% { transform: translateX(-100%) translateY(-100%) rotate(45deg); }
}

.btn-active {
  color: #00D4F8;
  background: rgba(0, 180, 216, 0.1);
  box-shadow: 0 4px 12px rgba(0, 180, 216, 0.4);
  transform: translateY(-2px) scale(1.1);
}
</style>
