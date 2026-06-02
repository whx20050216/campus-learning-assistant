<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register } from '@/api/auth'
import {
  User,
  Lock,
  Message,
  School,
  Collection,
  Cpu,
  Lightning,
  TrendCharts,
} from '@element-plus/icons-vue'

const router = useRouter()
const isLogin = ref(true)
const loading = ref(false)

const loginForm = reactive({
  account: '',
  password: '',
  rememberMe: false,
})

const registerForm = reactive({
  studentNo: '',
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  major: '',
  grade: '',
})

async function handleLogin() {
  if (!loginForm.account.trim()) {
    ElMessage.warning('请输入学号或邮箱')
    return
  }
  if (!loginForm.password) {
    ElMessage.warning('请输入密码')
    return
  }

  loading.value = true
  try {
    const res = await login({
      account: loginForm.account.trim(),
      password: loginForm.password,
      rememberMe: loginForm.rememberMe,
    })
    if (res.code === 200 && res.data) {
      const token = typeof res.data === 'string' ? res.data : (res.data.accessToken || res.data.token)
      const refreshToken = typeof res.data === 'string' ? '' : (res.data.refreshToken || '')
      if (token) {
        localStorage.setItem('token', token)
        if (refreshToken) {
          localStorage.setItem('refreshToken', refreshToken)
        }
        ElMessage.success('登录成功')
        router.push('/')
        // 通知 App.vue 更新登录状态
        window.dispatchEvent(new Event('auth-change'))
      } else {
        ElMessage.error('登录失败：未获取到有效令牌')
      }
    } else {
      ElMessage.error(res.msg || '登录失败')
    }
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.msg || '登录失败')
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!registerForm.studentNo.trim()) {
    ElMessage.warning('请输入学号')
    return
  }
  if (!registerForm.username.trim()) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (!registerForm.email.trim()) {
    ElMessage.warning('请输入邮箱')
    return
  }
  if (!registerForm.password) {
    ElMessage.warning('请输入密码')
    return
  }
  const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,}$/
  if (!PASSWORD_PATTERN.test(registerForm.password)) {
    ElMessage.warning('密码至少6位，且必须同时包含字母和数字')
    return
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }

  loading.value = true
  try {
    const res = await register({
      studentNo: registerForm.studentNo.trim(),
      username: registerForm.username.trim(),
      email: registerForm.email.trim(),
      password: registerForm.password,
      major: registerForm.major.trim() || undefined,
      grade: registerForm.grade.trim() || undefined,
    })
    if (res.code === 200) {
      ElMessage.success('注册成功，请登录')
      isLogin.value = true
      // 清空表单
      registerForm.studentNo = ''
      registerForm.username = ''
      registerForm.email = ''
      registerForm.password = ''
      registerForm.confirmPassword = ''
      registerForm.major = ''
      registerForm.grade = ''
    } else {
      ElMessage.error(res.msg || '注册失败')
    }
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.msg || '注册失败')
  } finally {
    loading.value = false
  }
}

function toggleMode() {
  isLogin.value = !isLogin.value
}
</script>

<template>
  <div class="login-view">
    <!-- 左侧品牌区 -->
    <div class="login-left">
      <div class="brand-shapes">
        <div class="brand-shape brand-shape-1"></div>
        <div class="brand-shape brand-shape-2"></div>
      </div>
      <div class="brand-content">
        <div class="brand-icon">
          <el-icon :size="64"><Collection /></el-icon>
        </div>
        <h1 class="brand-title">校园智能学习助手</h1>
        <p class="brand-slogan">基于 OCR 与 AI 技术的智能化学习资料管理平台<br />让知识获取更高效、学习规划更科学</p>
        <div class="brand-features">
          <div class="brand-feature">
            <span class="brand-feature-icon"><el-icon :size="28"><Cpu /></el-icon></span>
            <span>AI 智能识别</span>
          </div>
          <div class="brand-feature">
            <span class="brand-feature-icon"><el-icon :size="28"><Lightning /></el-icon></span>
            <span>极速检索</span>
          </div>
          <div class="brand-feature">
            <span class="brand-feature-icon"><el-icon :size="28"><TrendCharts /></el-icon></span>
            <span>学习追踪</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧表单区 -->
    <div class="login-right">
      <div class="auth-card">
        <div class="auth-header">
          <h2 class="auth-title">{{ isLogin ? '欢迎回来' : '创建账号' }}</h2>
          <p class="auth-subtitle">{{ isLogin ? '请登录您的账号' : '填写以下信息完成注册' }}</p>
        </div>

        <transition name="fade" mode="out-in">
          <!-- 登录表单 -->
          <div v-if="isLogin" key="login" class="auth-form">
            <div class="form-group">
              <label>学号 / 邮箱</label>
              <el-input
                v-model="loginForm.account"
                placeholder="请输入学号或邮箱"
                :prefix-icon="User"
                size="large"
                @keyup.enter="handleLogin"
              />
            </div>
            <div class="form-group">
              <label>密码</label>
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                :prefix-icon="Lock"
                size="large"
                show-password
                @keyup.enter="handleLogin"
              />
            </div>
            <div class="form-group" style="margin-bottom: var(--space-2);">
              <el-checkbox v-model="loginForm.rememberMe">记住我（7天）</el-checkbox>
            </div>
            <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="handleLogin">
              登 录
            </el-button>
          </div>

          <!-- 注册表单 -->
          <div v-else key="register" class="auth-form">
            <div class="form-row">
              <div class="form-group">
                <label>学号 <span class="form-required">*</span></label>
                <el-input v-model="registerForm.studentNo" placeholder="请输入学号" :prefix-icon="School" size="large" />
              </div>
              <div class="form-group">
                <label>用户名 <span class="form-required">*</span></label>
                <el-input v-model="registerForm.username" placeholder="请输入用户名" :prefix-icon="User" size="large" />
              </div>
            </div>
            <div class="form-group">
              <label>邮箱 <span class="form-required">*</span></label>
              <el-input v-model="registerForm.email" placeholder="请输入邮箱" :prefix-icon="Message" size="large" />
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>密码 <span class="form-required">*</span></label>
                <el-input v-model="registerForm.password" type="password" placeholder="至少6位，含字母和数字" :prefix-icon="Lock" size="large" show-password />
              </div>
              <div class="form-group">
                <label>确认密码 <span class="form-required">*</span></label>
                <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请再次输入密码" :prefix-icon="Lock" size="large" show-password />
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>专业</label>
                <el-input v-model="registerForm.major" placeholder="可选" :prefix-icon="School" size="large" />
              </div>
              <div class="form-group">
                <label>年级</label>
                <el-select v-model="registerForm.grade" placeholder="请选择年级" size="large" style="width: 100%">
                  <el-option label="大一" value="大一" />
                  <el-option label="大二" value="大二" />
                  <el-option label="大三" value="大三" />
                  <el-option label="大四" value="大四" />
                  <el-option label="研一" value="研一" />
                  <el-option label="研二" value="研二" />
                  <el-option label="研三" value="研三" />
                </el-select>
              </div>
            </div>
            <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="handleRegister">
              注 册
            </el-button>
          </div>
        </transition>

        <div class="auth-footer">
          <span v-if="isLogin">还没有账号？<a @click="toggleMode">立即注册</a></span>
          <span v-else>已有账号？<a @click="toggleMode">去登录</a></span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-view {
  min-height: 100vh;
  display: flex;
  background: var(--bg-page);
}

/* 左侧品牌区 */
.login-left {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #6366F1 0%, #4F46E5 100%);
  color: #fff;
  position: relative;
  overflow: hidden;
  padding: var(--space-8);
}

.brand-shapes {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.brand-shape {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}

.brand-shape-1 {
  width: 500px;
  height: 500px;
  top: -150px;
  right: -150px;
}

.brand-shape-2 {
  width: 350px;
  height: 350px;
  bottom: -100px;
  left: -100px;
}

.brand-content {
  position: relative;
  z-index: 1;
  text-align: center;
  max-width: 420px;
}

.brand-icon {
  margin-bottom: var(--space-4);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.brand-title {
  font-size: var(--text-3xl);
  font-weight: 700;
  margin: 0 0 var(--space-3);
  letter-spacing: 1px;
}

.brand-slogan {
  font-size: var(--text-base);
  line-height: 1.7;
  opacity: 0.9;
  margin: 0 0 var(--space-8);
}

.brand-features {
  display: flex;
  justify-content: center;
  gap: var(--space-6);
  flex-wrap: wrap;
}

.brand-feature {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  opacity: 0.9;
}

.brand-feature-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

/* 右侧表单区 */
.login-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-8);
}

.auth-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 40px;
  width: 100%;
  max-width: 460px;
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--border-color);
}

.auth-header {
  text-align: center;
  margin-bottom: var(--space-6);
}

.auth-title {
  font-size: var(--text-2xl);
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 var(--space-2);
}

.auth-subtitle {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  margin: 0;
}

.submit-btn {
  width: 100%;
  margin-top: var(--space-2);
  font-size: var(--text-lg);
  font-weight: 500;
  border-radius: var(--radius-md);
  transition: transform 0.2s var(--ease-out);
}

.submit-btn:hover {
  transform: scale(1.02);
}

/* 表单布局 */
.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-4);
  margin-bottom: var(--space-4);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  margin-bottom: var(--space-4);
}

.form-row .form-group {
  margin-bottom: 0;
}

.form-group label {
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--text-secondary);
}

.form-required {
  color: var(--danger-500);
}

.auth-footer {
  text-align: center;
  margin-top: var(--space-5);
  font-size: var(--text-sm);
  color: var(--text-secondary);
}

.auth-footer a {
  color: var(--primary-500);
  cursor: pointer;
  font-weight: 500;
}

.auth-footer a:hover {
  text-decoration: underline;
}

/* 输入框聚焦品牌色发光 */
:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.15) !important;
}

/* 响应式 */
@media (max-width: 900px) {
  .login-view {
    flex-direction: column;
  }

  .login-left {
    min-height: 200px;
    padding: var(--space-8) var(--space-4);
  }

  .brand-title {
    font-size: var(--text-2xl);
  }

  .brand-slogan {
    font-size: var(--text-sm);
  }

  .login-right {
    padding: var(--space-6) var(--space-4);
  }

  .auth-card {
    padding: var(--space-6);
  }
}

@media (max-width: 480px) {
  .auth-card {
    padding: var(--space-5) var(--space-4);
  }

  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
