<template>
  <el-dialog
    class="auth-dialog"
    :model-value="modelValue"
    width="min(460px, calc(100vw - 32px))"
    :show-close="true"
    destroy-on-close
    @close="close"
  >
    <template #header>
      <div class="auth-dialog__header">
        <p>{{ authHeader.eyebrow }}</p>
        <h2>{{ authHeader.title }}</h2>
        <span>{{ authHeader.subtitle }}</span>
      </div>
    </template>

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

      <div class="agreement-line" :class="{ 'agreement-line--shake': agreementShake && mode === 'login' }">
        <el-checkbox v-model="agreements.login" aria-label="同意用户协议和隐私协议" />
        <span>
          我已阅读并同意
          <button type="button" @click="showPolicy('terms')">《用户协议》</button>
          和
          <button type="button" @click="showPolicy('privacy')">《隐私协议》</button>
        </span>
      </div>

      <p v-if="message" class="auth-message">{{ message }}</p>
      <el-button class="auth-submit" type="primary" :loading="loading" @click="submitLogin">登录</el-button>

      <div class="auth-dialog__footer-links">
        <button type="button" @click="openForgot">忘记密码</button>
        <button type="button" @click="switchMode('register')">没有账号？立即注册</button>
      </div>
    </el-form>

    <el-form
      v-else-if="mode === 'register'"
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
      <el-form-item label="邮箱" prop="email">
        <el-input v-model.trim="registerForm.email" autocomplete="email" placeholder="用于后续账号安全能力" />
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

      <div class="agreement-line" :class="{ 'agreement-line--shake': agreementShake && mode === 'register' }">
        <el-checkbox v-model="agreements.register" aria-label="同意用户协议和隐私协议" />
        <span>
          我已阅读并同意
          <button type="button" @click="showPolicy('terms')">《用户协议》</button>
          和
          <button type="button" @click="showPolicy('privacy')">《隐私协议》</button>
        </span>
      </div>

      <p v-if="message" class="auth-message">{{ message }}</p>
      <el-button class="auth-submit" type="primary" :loading="loading" @click="submitRegister">注册并登录</el-button>

      <div class="auth-dialog__footer-links auth-dialog__footer-links--single">
        <button type="button" @click="switchMode('login')">已有账号？返回登录</button>
      </div>
    </el-form>

    <div v-else class="forgot-panel">
      <div class="forgot-panel__icon">?</div>
      <h3>忘记密码</h3>
      <p>邮箱找回密码功能将在后续开放。当前请联系管理员协助处理。</p>
      <el-button class="auth-submit" type="primary" plain @click="switchMode('login')">返回登录</el-button>
    </div>

    <div v-if="policyPreview" class="policy-preview" role="note">
      <strong>{{ policyPreview === 'terms' ? '用户协议' : '隐私协议' }}</strong>
      <p>
        本阶段提供简洁占位说明：请使用真实账号信息，妥善保管密码；系统仅按当前功能需要保存账号资料与购车决策数据。
      </p>
      <button type="button" @click="policyPreview = ''">知道了</button>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'

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
const emailPattern = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/

const mode = ref(props.initialMode === 'register' ? 'register' : 'login')
const loading = ref(false)
const message = ref('')
const agreementShake = ref(false)
const policyPreview = ref('')
const loginFormRef = ref(null)
const registerFormRef = ref(null)
let shakeTimer = 0

const loginForm = reactive({
  username: '',
  password: '',
})

const registerForm = reactive({
  username: '',
  nickname: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: '',
})

const agreements = reactive({
  login: false,
  register: false,
})

const authHeader = computed(() => {
  if (mode.value === 'register') {
    return {
      eyebrow: '创建账号',
      title: '注册后保存需求和结果',
      subtitle: '注册只创建普通用户账号。',
    }
  }
  if (mode.value === 'forgot') {
    return {
      eyebrow: '账号协助',
      title: '忘记密码',
      subtitle: '本阶段先提供处理提示。',
    }
  }
  return {
    eyebrow: '欢迎回来',
    title: '登录后继续购车决策',
    subtitle: '普通用户和管理员使用同一个入口。',
  }
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
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (!emailPattern.test(String(value || '').trim())) {
          callback(new Error('请输入有效邮箱'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
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
    resetInlineState()
  },
)

watch(
  () => props.modelValue,
  (value) => {
    if (value) {
      mode.value = props.initialMode === 'register' ? 'register' : 'login'
      resetInlineState()
    }
  },
)

onBeforeUnmount(() => {
  window.clearTimeout(shakeTimer)
})

function switchMode(nextMode) {
  mode.value = nextMode
  resetInlineState()
  if (nextMode === 'login' || nextMode === 'register') {
    emit('mode-change', nextMode)
  }
}

function openForgot() {
  mode.value = 'forgot'
  resetInlineState()
}

function showPolicy(type) {
  policyPreview.value = type
}

async function submitLogin() {
  message.value = ''
  if (!ensureAgreement('login')) {
    return
  }
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
  if (!ensureAgreement('register')) {
    return
  }
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

function ensureAgreement(scope) {
  if (agreements[scope]) {
    return true
  }
  triggerAgreementWarning()
  return false
}

function triggerAgreementWarning() {
  message.value = '请先阅读并同意用户协议和隐私协议。'
  agreementShake.value = false
  window.clearTimeout(shakeTimer)
  requestAnimationFrame(() => {
    agreementShake.value = true
    shakeTimer = window.setTimeout(() => {
      agreementShake.value = false
    }, 1000)
  })
}

function resetInlineState() {
  message.value = ''
  policyPreview.value = ''
  agreementShake.value = false
}

function close() {
  emit('update:modelValue', false)
}
</script>

<style scoped>
.auth-dialog :deep(.el-dialog) {
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 30px;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.92), rgba(245, 247, 250, 0.82)),
    radial-gradient(circle at 12% 0%, rgba(10, 132, 255, 0.12), transparent 34%);
  box-shadow: 0 34px 90px rgba(15, 23, 42, 0.18);
  backdrop-filter: blur(28px) saturate(150%);
}

.auth-dialog :deep(.el-dialog__header) {
  padding: 30px 30px 6px;
}

.auth-dialog :deep(.el-dialog__body) {
  padding: 18px 30px 30px;
}

.auth-dialog :deep(.el-dialog__headerbtn) {
  top: 18px;
  right: 18px;
  width: 34px;
  height: 34px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.68);
}

.auth-dialog :deep(.el-form-item) {
  margin-bottom: 14px;
}

.auth-dialog :deep(.el-form-item__label) {
  margin-bottom: 7px;
  color: #374151;
  font-size: 13px;
  font-weight: 760;
}

.auth-dialog :deep(.el-input__wrapper) {
  min-height: 44px;
  border-radius: 16px;
  background: rgba(246, 248, 251, 0.9);
  box-shadow: inset 0 0 0 1px rgba(148, 163, 184, 0.16);
  transition:
    box-shadow 180ms ease,
    background 180ms ease,
    transform 180ms ease;
}

.auth-dialog :deep(.el-input__wrapper.is-focus) {
  background: rgba(255, 255, 255, 0.96);
  box-shadow:
    inset 0 0 0 1px rgba(10, 132, 255, 0.34),
    0 0 0 4px rgba(10, 132, 255, 0.11);
}

.auth-dialog__header {
  display: grid;
  gap: 7px;
  padding-right: 26px;
}

.auth-dialog__header p {
  margin: 0;
  color: #0b65c2;
  font-size: 12px;
  font-weight: 850;
  letter-spacing: 0.12em;
}

.auth-dialog__header h2 {
  margin: 0;
  color: var(--color-primary-dark);
  font-size: 25px;
  line-height: 1.1;
  letter-spacing: -0.035em;
}

.auth-dialog__header span {
  color: var(--color-muted);
  font-size: 13px;
}

.auth-form {
  display: grid;
  gap: 2px;
}

.agreement-line {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin: 2px 0 12px;
  padding: 12px 13px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.58);
  color: #475569;
  font-size: 12px;
  line-height: 1.6;
}

.agreement-line :deep(.el-checkbox) {
  height: auto;
  margin-top: 1px;
}

.agreement-line button,
.auth-dialog__footer-links button,
.policy-preview button {
  padding: 0;
  border: 0;
  background: transparent;
  color: #0b65c2;
  cursor: pointer;
  font: inherit;
  font-weight: 780;
}

.agreement-line--shake {
  border-color: rgba(220, 38, 38, 0.28);
  background: rgba(254, 242, 242, 0.72);
  animation: agreement-shake 460ms ease;
}

.auth-message {
  margin: 0 0 12px;
  padding: 10px 12px;
  border: 1px solid rgba(220, 38, 38, 0.1);
  border-radius: 14px;
  background: rgba(254, 242, 242, 0.76);
  color: #b42318;
  font-size: 13px;
}

.auth-submit {
  width: 100%;
  min-height: 46px;
  border-radius: 999px;
  font-weight: 850;
  letter-spacing: -0.01em;
}

.auth-dialog__footer-links {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-top: 16px;
  color: var(--color-muted);
  font-size: 13px;
}

.auth-dialog__footer-links--single {
  justify-content: center;
}

.forgot-panel {
  display: grid;
  justify-items: center;
  gap: 14px;
  padding: 12px 0 4px;
  text-align: center;
}

.forgot-panel__icon {
  display: grid;
  width: 52px;
  height: 52px;
  place-items: center;
  border: 1px solid rgba(10, 132, 255, 0.16);
  border-radius: 18px;
  background: rgba(10, 132, 255, 0.08);
  color: #0b65c2;
  font-size: 26px;
  font-weight: 800;
}

.forgot-panel h3 {
  margin: 0;
  color: var(--color-primary-dark);
  font-size: 21px;
  letter-spacing: -0.02em;
}

.forgot-panel p {
  margin: 0;
  color: var(--color-muted);
  line-height: 1.8;
}

.policy-preview {
  display: grid;
  gap: 8px;
  margin-top: 16px;
  padding: 14px;
  border: 1px solid rgba(10, 132, 255, 0.16);
  border-radius: 18px;
  background: rgba(239, 246, 255, 0.68);
  color: #334155;
  font-size: 13px;
}

.policy-preview strong {
  color: #0b65c2;
}

.policy-preview p {
  margin: 0;
  line-height: 1.7;
}

@keyframes agreement-shake {
  0%,
  100% {
    transform: translateX(0);
  }
  20% {
    transform: translateX(-7px);
  }
  40% {
    transform: translateX(6px);
  }
  60% {
    transform: translateX(-4px);
  }
  80% {
    transform: translateX(3px);
  }
}

@media (max-width: 560px) {
  .auth-dialog :deep(.el-dialog__header) {
    padding: 26px 22px 4px;
  }

  .auth-dialog :deep(.el-dialog__body) {
    padding: 16px 22px 24px;
  }

  .auth-dialog__footer-links {
    display: grid;
    justify-content: stretch;
    text-align: center;
  }
}
</style>
