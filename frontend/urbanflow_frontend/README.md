
# UrbanFlow Frontend

Complete frontend implementation for the UrbanFlow traffic management system, providing comprehensive traffic control and management features.

### ***The project opens to the login page by default. You can log in with any username and password to pass authentication.

### ***To clear current login session: Open browser developer tools (F12), go to Console and run: `localStorage.removeItem('authToken')`, then refresh the page.

## Project Features

### **Completed Pages**

| Page | Path | Description |
|------|------|-------------|
| **Login Page** | `/login` | User authentication |
| **Forgot Password** | `/forget` | Password reset request |
| **Reset Password** | `/reset` | Password reset confirmation |
| **Traffic Control** | `/control` | Main traffic signal control page |
| **Dashboard** | `/dashboard` | System overview and data visualization |
| **User Management** | `/user` | System user management |
| **User Logs** | `/user/log` | User activity log viewer |
| **Help Documentation** | `/help` | System usage guide |

### **Page Navigation Methods**

####  **Authentication Pages**
- **Direct Access**: Enter the corresponding URL in the browser
- **Auto Redirect**: Unauthenticated users are automatically redirected to login page when accessing protected pages
- **Any username and password can be used to log in**

####  **Main Function Pages**
- **Sidebar Navigation**: All main function pages can be quickly switched through the left sidebar
- **Navigation Control**: Click the button on the left side of the Header to expand/collapse the navigation bar
- **Smart Highlighting**: Current page is automatically highlighted in the navigation bar

#### **Navigation Menu Structure**
```
 Main Navigation
├──  Control (Traffic Control)
├──  Dashboard (Data Dashboard)  
├──  Administration (Management Functions)
│   ├── User Management
│   └── User Logs
└──  Help (Help Documentation)
```

##  Authentication System

### **Login Authentication**
```javascript
// Set authentication token after successful login
localStorage.setItem('authToken', 'your-token-here')

// Clear authentication on logout
localStorage.removeItem('authToken')
```

### **Route Protection**
- Protected pages require login to access
- Unauthenticated users are automatically redirected to login page
- Authenticated users accessing login page are automatically redirected to control panel

##  Website URLs (localhost)

### **Authentication Pages**
- **Login**: http://localhost:5173/login
- **Forgot Password**: http://localhost:5173/forget  
- **Reset Password**: http://localhost:5173/reset

### **Function Pages**
- **Traffic Control**: http://localhost:5173/control
- **Dashboard**: http://localhost:5173/dashboard
- **User Management**: http://localhost:5173/user
- **User Logs**: http://localhost:5173/user/log
- **Help Documentation**: http://localhost:5173/help

##  **Getting Started**

### **⚠️ Important Note for GitHub Collaboration**
**DO NOT upload `node_modules` folder to GitHub.** This folder contains all dependencies and can be very large (hundreds of MB). It should be automatically ignored by `.gitignore`. Always run `npm install` to download dependencies locally.

### Install Dependencies
```bash
cd urbanflow_frontend
npm install
```

### Development
```bash
npm run dev
```
Runs on http://localhost:5173/ by default

### Build for Production
```bash
npm run build
```

### Lint Code
```bash
npm run lint
```

##  **Technical Features**

### **Toast Notification Component**
```javascript
import { toast } from '@/utils/ToastService'

// Usage examples
try {
  await axios.post('<your backend api>', requestBody)
  toast.success('Operation successful!')
} catch (error) {
  toast.error('Operation failed, please try again')
}
```

### **HTTP Request Configuration**
```javascript
// Basic usage
import axios from 'axios'
const response = await axios.get('<your backend api>')

// Vite proxy configuration (vite.config.ts)
'/api-status': {
  target: 'http://localhost:8087',
  changeOrigin: true,
  rewrite: path => path
}
```

### **Common Components Usage**

#### 🔸 Input Component (CommonInput)
```vue
<template>
  <div>
    <!-- Basic usage -->
    <CommonInput 
      v-model="username" 
      placeholder="Enter username" 
    />
    
    <!-- Different types -->
    <CommonInput 
      v-model="password" 
      type="password"
      placeholder="Enter password" 
    />
    
    <!-- Number input -->
    <CommonInput 
      v-model="age" 
      type="number"
      placeholder="Enter age" 
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import CommonInput from '@/components/common/CommonInput.vue'

const username = ref('')
const password = ref('')
const age = ref(0)
</script>
```

**Props:**
- `modelValue`: `string | number` - Bound value
- `type`: `string` - Input type (default: 'text')
- Supports all native input attributes

#### 🔸 Select Component (CommonSelect)
```vue
<template>
  <div>
    <!-- Basic usage -->
    <CommonSelect
      v-model="selectedValue"
      :options="optionsList"
      placeholder="Please select an option"
    />
    
    <!-- Junction selection example -->

# urbanflow



#### This PR adds the `urbanflow_frontend` folder containing the complete frontend implementation for the UrbanFlow traffic management system.



#### **Currently completed page files**

`src\views\control` for ControlPage
`src\views\login` for LogIn ( The basic style of LogInPage is written, but it does not fully conform to the UL design )

To use your own iconfont, please create a new `iconfont.css` in `src\style` (use another file name, such as `iconfont_login.css`, and then add this css path to `src\style\index.scss`). 

Iconfont recommended website https://www.iconfont.cn/



#### Website URL（localhost）

The webpage url has been configured:

**Login** : http://localhost:5173/login
**Forget** : http://localhost:5173/forget
**Reset** : http://localhost:5173/reset

You can check the url in `src\router\index.ts`，These URLs can only be accessed after the project is started



#### **How to Run**

Install dependencies

```
bash

cd frontend/urbanflow_frontend
npm install
npm run dev
```

By default, the app runs on http://localhost:5173/



#### other

##### `src\components\BaseToast.vue` contains a designed pop-up window that can be imported and used

```js
import { toast } from '@/utils/ToastService'
try {
    await axios.post('<your backend api>', requestBody)
    toast.success('Traffic light settings updated successfully!')
    resetForm()
  } catch (error) {
    console.error('error)
    toast.error('Failed to send data to backend.')
  }
}
```



##### Use axios to connect to the backend interface

```js
import axios from 'axios'
const response = await axios.get('<your backend api>')
```

配置前后端接口，需要在`vite.config.ts`进行接口配置

```js
'/api-status': {
        target: 'http://localhost:8087',
        changeOrigin: true,
        rewrite: path => path, 
      },
```



Input 输入框组件使用方法

```vue
<template>
  <div>
    <CommonInput v-model="username" placeholder="Enter username" />
  </div>
</template>
```



drop-down box 下拉框组件使用方法（junction例子）

```vue
<template>
  <div>

    <CommonSelect
      v-model="selectedJunction"
      :options="junctionOptions"
      placeholder="Please select a junction"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import axios from 'axios'

import CommonSelect from '@/components/common/CommonSelect.vue'

const selectedValue = ref('')
const selectedJunction = ref('')

const optionsList = ref([
  { label: 'Option 1', value: 'option1' },
  { label: 'Option 2', value: 'option2' }
])

const junctionOptions = ref([])

// Dynamically load junction data
onMounted(async () => {
  try {
    const res = await axios.get('/api-status/junctions')
    const rawData = Object.values(res.data)
    


interface OptionItem {
  label: string
  value: string
}

interface RawJunctionData {
  junction_id: string
  junction_name: string
  tlsID: string
  connection: string[][][]
}

const selectedJunction = ref('')

const junctionOptions = ref<OptionItem[]>([])

onMounted(async () => {
  try {
    const res = await axios.get('/api-status/junctions')

    const rawData = Object.values(res.data as Record<string, RawJunctionData>)


    junctionOptions.value = rawData.map(j => ({
      label: j.junction_name || j.junction_id,
      value: j.junction_id
    }))
  } catch (err) {

    console.error('Failed to load junction data:', err)
  }
})
</script>
```

**Props:**
- `modelValue`: `string | number` - Bound value
- `options`: `OptionItem[]` - Options list
- `placeholder`: `string` - Placeholder text
- Supports all native select attributes

**OptionItem Interface:**
```typescript
interface OptionItem {
  label: string    // Display text
  value: string | number  // Option value
}
```

#### 🔸 Button Component (CommonButton)
```vue
<template>
  <div>
    <!-- Basic usage -->
    <CommonButton 
      label="Save" 
      @click="handleSave" 
    />
    
    <!-- Different style variants -->
    <CommonButton 
      label="Submit" 
      variant="primary" 
      @click="handleSubmit" 
    />
    
    <CommonButton 
      label="Cancel" 
      variant="secondary" 
      @click="handleCancel" 
    />
    
    <!-- Disabled state -->
    <CommonButton
      label="Submit"
      variant="primary"
      :disabled="!formValid"
      @click="handleSubmit"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import CommonButton from '@/components/common/CommonButton.vue'

const formData = ref({})
const formValid = computed(() => {
  // Form validation logic
  return true
})

const handleSave = () => {
  console.log('Save data')
}

const handleSubmit = () => {
  console.log('Submit form')
}

const handleCancel = () => {
  console.log('Cancel operation')
}
</script>
```

**Props:**
- `label`: `string` - Button text
- `variant`: `'primary' | 'secondary'` - Button style (default: 'primary')
- `disabled`: `boolean` - Whether disabled (default: false)

**Events:**
- `@click` - Click event

#### 🔸 Toast Notification Component (BaseToast)
```vue
<template>
  <div id="app">
    <!-- Add Toast component in App.vue -->
    <router-view />
    <BaseToast />
  </div>
</template>

<script setup lang="ts">
import BaseToast from '@/components/BaseToast.vue'
</script>
```

**Usage in any component:**
```javascript
import { toast } from '@/utils/ToastService'

// Success notification
toast.success('Operation successful!')

// Error notification
toast.error('Operation failed, please try again')

// Warning notification
toast.warning('Please check your input')

// Info notification
toast.info('Loading...')

// Custom title and duration
toast.success('Save successful', 'Data saved', 5000)
```

**Toast Methods:**
- `toast.success(message, title?, duration?)` - Success notification
- `toast.error(message, title?, duration?)` - Error notification
- `toast.warning(message, title?, duration?)` - Warning notification
- `toast.info(message, title?, duration?)` - Info notification

**Parameters:**
- `message`: `string` - Notification message
- `title`: `string` - Optional title
- `duration`: `number` - Display duration (milliseconds, default 3000)

#### 🔸 Navigation Components (NavItem & NavGroup)
Navigation-related components are mainly used internally by the system and generally do not need to be used directly in business pages.

**NavItem** - Navigation menu item
**NavGroup** - Expandable navigation menu group

## **Project Structure**
```
src/
├── components/         # Common components
│   ├── navCom/        # Navigation-related components
│   └── common/        # Common UI components
├── views/             # Page components
│   ├── login/         # Login-related pages
│   ├── control/       # Traffic control pages
│   ├── dashboard/     # Dashboard pages
│   ├── user/          # User management pages
│   ├── help/          # Help pages
│   └── error/         # Error pages
├── router/            # Route configuration
├── utils/             # Utility functions
├── stores/            # State management
└── style/             # Style files
```

## **Styling System**

### **Icon Fonts**
It is recommended to use [iconfont.cn](https://www.iconfont.cn/) to create custom icon font files and place them in the `src/style/` directory.

### **Style Configuration**
- Uses SCSS preprocessor
- Responsive design support
- Unified component style specifications

##  **Development Recommendations**

### **Recommended IDE Setup**
- [VSCode](https://code.visualstudio.com/) + [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar)
- Disable Vetur extension to avoid conflicts

### **TypeScript Support**
The project uses `vue-tsc` for type checking to ensure type safety for `.vue` files.

### **Configuration Reference**
For more configuration options, please refer to [Vite Configuration Reference](https://vite.dev/config/).

---

## **Technical Support**

For technical support or feature suggestions, please contact the development team through project Issues or relevant channels.

    console.error(err)
  }
})
</script>

```



This template should help get you started developing with Vue 3 in Vite.

## Recommended IDE Setup

[VSCode](https://code.visualstudio.com/) + [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (and disable Vetur).

## Type Support for `.vue` Imports in TS

TypeScript cannot handle type information for `.vue` imports by default, so we replace the `tsc` CLI with `vue-tsc` for type checking. In editors, we need [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) to make the TypeScript language service aware of `.vue` types.

## Customize configuration

See [Vite Configuration Reference](https://vite.dev/config/).

## Project Setup

```sh
npm install
```

### Compile and Hot-Reload for Development

```sh
npm run dev
```

### Type-Check, Compile and Minify for Production

```sh
npm run build
```

### Lint with [ESLint](https://eslint.org/)

```sh
npm run lint
```

