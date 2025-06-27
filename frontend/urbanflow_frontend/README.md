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

To configure the front-end and back-end interfaces, you need to configure the interfaces in `vite.config.ts`

```js
'/api-status': {
        target: 'http://localhost:8087',
        changeOrigin: true,
        rewrite: path => path, 
      },
```



How to use the Input input box component

```vue
<template>
  <div>
    <CommonInput v-model="username" placeholder="Enter username" />
  </div>
</template>
```



How to use the drop-down box component (junction example)

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
    console.error(err)
  }
})
</script>

```




# urbanflow

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
