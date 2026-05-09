<template>
  <el-dialog
    class="auth-dialog"
    :model-value="modelValue"
    width="440px"
    :show-close="true"
    destroy-on-close
    @close="close"
  >
    <template #header>
      <div class="auth-dialog__header">
        <p>{{ mode === 'login' ? '欢迎回来' : '创建普通用户账号' }}</p>
        <h2>{{ mode === 'login' ? '登录后继续购车决策' : '注册后保存需求和结果' }}</h2>
      </div>
    </template>

    <div class="auth-switch" role="tablist" aria-label="登录注册切换">
      <button :class="{ active: mode === 'login' }" type="button" @click="switchMode('login')">登录</button>
      <button :class="{ active: mode === 'register' }" type="button" @click="switchMode('register')">注册</button>
    </div>

    <el-form
      v-if="mode === 'login'"
      ref="loginFormRef"
      class="auth-form"
      :model="loginForm"
      :rules="loginRules"
      label-position="top"
      @keyup.enter="submitLogin"
    >
      <el-form-item label="用户名" prop="username">
        <el-input v-model.trim="loginForm.username" autocomplete="username" placeholder="请输入用户名" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="loginForm.password"
          autocomplete="current-password"
          placeholder="请输入密码"
          show-password
          type="password"
        />
      </el-form-item>
      <p v-if="message" class="auth-message">{{ message }}</p>
      <el-button class="auth-submit" type="primary" :loading="loading" @click="submitLogin">登录</el-button>
    </el-form>

    <el-form
      v-else
      ref="registerFormRef"
      class="auth-form"
      :model="registerForm"
      :rules="registerRules"
      label-position="top"
      @keyup.enter="submitRegister"
    >
      <el-form-item label="用户名" prop="username">
        <el-input v-model.trim="registerForm.username" autocomplete="username" placeholder="4-32 位字母、数字或下划线" />
      </el-form-item>
      <el-form-item label="昵称" prop="nickname">
        <el-input v-model.trim="registerForm.nickname" maxlength="32" show-word-limit placeholder="可选，默认使用用户名" />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model.trim="registerForm.phone" autocomplete="tel" placeholder="可选，11 位手机号" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="registerForm.password"
          autocomplete="new-password"
          placeholder="8-32 位，至少包含字母和数字"
          show-password
          type="password"
        />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input
          v-model="registerForm.confirmPassword"
          autocomplete="new-password"
          placeholder="再次输入密码"
          show-password
          type="password"
        />
      </el-form-item>
      <p v-if="message" class="auth-message">{{ message }}</p>
      <el-button class="auth-submit" type="primary" :loading="loading" @click="submitRegister">注册并登录</el-button>
    </el-form>
  </el-dialog>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'

import { useAuthStore } from '@/stores/auth'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  initialMode: {
    type: String,
    default: 'login',
  },
})

const emit = defineEmits(['update:modelValue', 'success', 'mode-change'])
const authStore = useAuthStore()

const mode = ref(props.initialMode === 'register' ? 'register' : 'login')
const loading = ref(false)
const message = ref('')
const loginFormRef = ref(null)
const registerFormRef = ref(null)

const loginForm = reactive({
  username: '',
  password: '',
})

const registerForm = reactive({
  username: '',
  nickname: '',
  phone: '',
  password: '',
  confirmPassword: '',
})

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 32, message: '用户名需为 4-32 位', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 32, message: '密码需为 8-32 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入的密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

watch(
  () => props.initialMode,
  (value) => {
    mode.value = value === 'register' ? 'register' : 'login'
    message.value = ''
  },
)

watch(
  () => props.modelValue,
  (value) => {
    if (value) {
      mode.value = props.initialMode === 'register' ? 'register' : 'login'
      message.value = ''
    }
  },
)

function switchMode(nextMode) {
  mode.value = nextMode
  message.value = ''
  emit('mode-change', nextMode)
}

async function submitLogin() {
  message.value = ''
  try {
    await loginFormRef.value.validate()
  } catch {
    message.value = '请先填写用户名和密码。'
    return
  }
  loading.value = true
  try {
    const data = await authStore.loginUnified({
      username: loginForm.username,
      password: loginForm.password,
    })
    emit('success', data)
    close()
  } catch (error) {
    message.value = error?.response?.data?.message || error?.message || '登录失败，请检查用户名和密码。'
  } finally {
    loading.value = false
  }
}

async function submitRegister() {
  message.value = ''
  try {
    await registerFormRef.value.validate()
  } catch {
    message.value = '请先完成注册信息。'
    return
  }
  loading.value = true
  try {
    const data = await authStore.register({ ...registerForm })
    emit('success', data)
    close()
  } catch (error) {
    message.value = error?.response?.data?.message || error?.message || '注册失败，请检查填写内容。'
  } finally {
    loading.value = false
  }
}

function close() {
  emit('update:modelValue', false)
}
</script>

<style scoped>
.auth-dialog__header p {
  margin: 0 0 8px;
  color: #0b65c2;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.12em;
}

.auth-dialog__header h2 {
  margin: 0;
  color: var(--color-primary-dark);
  font-size: 24px;
  letter-spacing: -0.03em;
}

.auth-switch {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
  margin-bottom: 20px;
  padding: 6px;
  border: 1px solid var(--color-border);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
}

.auth-switch button {
  min-height: 38px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: var(--color-muted);
  cursor: pointer;
  font-weight: 850;
}

.auth-switch button.active {
  background: #111827;
  color: #fff;
}

.auth-form {
  display: grid;
  gap: 2px;
}

.auth-message {
  margin: 0 0 12px;
  padding: 10px 12px;
  border-radius: 12px;
  background: #fef2f2;
  color: #b42318;
  font-size: 13px;
}

.auth-submit {
  width: 100%;
  min-height: 44px;
  border-radius: 999px;
  font-weight: 850;
}
</style>
