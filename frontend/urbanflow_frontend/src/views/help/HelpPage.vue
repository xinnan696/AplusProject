<template>
  <div class="help-page">
    <ControlHeader @toggle-nav="toggleNav" />
    <ControlNav :isVisible="isNavVisible" />

    <div class="main-area">
      <div class="help-container">
        <div class="page-header">
          <h1>Help & Documentation</h1>
          <p>Get help with UrbanFlow system features</p>
        </div>
        
        <div class="help-content">
          <div class="help-sections">
            <div class="help-section">
              <h2>🚦 Traffic Control</h2>
              <p>Learn how to manage traffic signals and optimize flow patterns.</p>
              <ul>
                <li>Manual signal control</li>
                <li>AI-powered optimization</li>
                <li>Real-time monitoring</li>
                <li>Emergency override</li>
              </ul>
            </div>
            
            <div class="help-section">
              <h2>📊 Dashboard</h2>
              <p>Understanding your system dashboard and analytics.</p>
              <ul>
                <li>Traffic flow metrics</li>
                <li>System performance</li>
                <li>Historical data</li>
                <li>Alert management</li>
              </ul>
            </div>
            
            <div class="help-section">
              <h2>👥 User Management</h2>
              <p>Manage users, roles, and system permissions.</p>
              <ul>
                <li>Adding new users</li>
                <li>Role assignments</li>
                <li>Access control</li>
                <li>Activity monitoring</li>
              </ul>
            </div>
            
            <div class="help-section">
              <h2>⚙️ System Settings</h2>
              <p>Configure system preferences and parameters.</p>
              <ul>
                <li>Signal timing</li>
                <li>AI parameters</li>
                <li>Notification settings</li>
                <li>System maintenance</li>
              </ul>
            </div>
          </div>
          
          <div class="contact-info">
            <h2>Need More Help?</h2>
            <div class="contact-card">
              <div class="contact-item">
                <strong>📧 Email Support:</strong>
                <span>support@urbanflow.com</span>
              </div>
              <div class="contact-item">
                <strong>📱 Phone:</strong>
                <span>+1 (555) 123-4567</span>
              </div>
              <div class="contact-item">
                <strong>🕒 Hours:</strong>
                <span>Mon-Fri 9AM-6PM EST</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import ControlHeader from '@/views/control/ControlHeader.vue'
import ControlNav from '@/views/control/ControlNav.vue'
import { isNavVisible, toggleNav } from '@/utils/navState'
</script>

<style scoped lang="scss">
.help-page {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-color: #1E1E2F;
  z-index: 1;
}

.main-area {
  height: calc(100% - 0.64rem);
  display: flex;
  overflow: hidden;
}

.help-container {
  flex: 1;
  padding: 2rem;
  background-color: #f5f5f5;
  overflow-y: auto;
}

.page-header {
  margin-bottom: 2rem;
}

.page-header h1 {
  color: #2d3748;
  margin-bottom: 0.5rem;
  font-size: 2rem;
}

.page-header p {
  color: #718096;
  font-size: 1.1rem;
}

.help-content {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 2rem;
}

.help-sections {
  display: grid;
  gap: 1.5rem;
}

.help-section {
  background: white;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.help-section h2 {
  color: #2d3748;
  margin-bottom: 1rem;
  font-size: 1.25rem;
}

.help-section p {
  color: #718096;
  margin-bottom: 1rem;
  line-height: 1.6;
}

.help-section ul {
  list-style: none;
  padding: 0;
}

.help-section li {
  padding: 0.5rem 0;
  border-bottom: 1px solid #f1f5f9;
  color: #4a5568;
}

.help-section li:before {
  content: "✓ ";
  color: #00e3ff;
  font-weight: bold;
  margin-right: 0.5rem;
}

.contact-info {
  position: sticky;
  top: 2rem;
  height: fit-content;
}

.contact-info h2 {
  color: #2d3748;
  margin-bottom: 1rem;
}

.contact-card {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.contact-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  margin-bottom: 1rem;
}

.contact-item:last-child {
  margin-bottom: 0;
}

.contact-item strong {
  color: #2d3748;
  font-size: 0.875rem;
}

.contact-item span {
  color: #4a5568;
}

@media (max-width: 768px) {
  .help-content {
    grid-template-columns: 1fr;
  }
  
  .contact-info {
    position: static;
  }
}
</style>