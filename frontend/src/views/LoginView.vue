<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register } from '@/api/auth'

const router = useRouter()
const isLogin = ref(true)
const loading = ref(false)

const loginForm = reactive({
  account: '',
  password: '',
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
    })
    if (res.code === 200) {
      localStorage.setItem('token', res.data)
      ElMessage.success('登录成功')
      router.push('/')
      // 通知 App.vue 更新登录状态
      window.dispatchEvent(new Event('auth-change'))
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
  if (registerForm.password.length < 6) {
    ElMessage.warning('密码至少6位')
    return
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    ElMessage.warning('两次密码输入不一致')
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
    <div class="auth-card">
      <div class="auth-header">
        <h2 class="auth-title">📚 校园智能学习助手</h2>
        <p class="auth-subtitle">{{ isLogin ? '欢迎回来，请登录' : '创建新账号' }}</p>
      </div>

      <!-- 登录表单 -->
      <template v-if="isLogin">
        <div class="form-group">
          <label>学号 / 邮箱</label>
          <el-input v-model="loginForm.account" placeholder="请输入学号或邮箱" @keyup.enter="handleLogin" />
        </div>
        <div class="form-group">
          <label>密码</label>
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password @keyup.enter="handleLogin" />
        </div>
        <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="handleLogin">
          登 录
        </el-button>
      </template>

      <!-- 注册表单 -->
      <template v-else>
        <div class="form-row">
          <div class="form-group">
            <label>学号 <span class="form-required">*</span></label>
            <el-input v-model="registerForm.studentNo" placeholder="请输入学号" />
          </div>
          <div class="form-group">
            <label>用户名 <span class="form-required">*</span></label>
            <el-input v-model="registerForm.username" placeholder="请输入用户名" />
          </div>
        </div>
        <div class="form-group">
          <label>邮箱 <span class="form-required">*</span></label>
          <el-input v-model="registerForm.email" placeholder="请输入邮箱" />
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>密码 <span class="form-required">*</span></label>
            <el-input v-model="registerForm.password" type="password" placeholder="至少6位，含字母和数字" show-password />
          </div>
          <div class="form-group">
            <label>确认密码 <span class="form-required">*</span></label>
            <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请再次输入密码" show-password />
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>专业</label>
            <el-input v-model="registerForm.major" placeholder="可选" />
          </div>
          <div class="form-group">
            <label>年级</label>
            <el-input v-model="registerForm.grade" placeholder="例如：2023级" />
          </div>
        </div>
        <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="handleRegister">
          注 册
        </el-button>
      </template>

      <div class="auth-footer">
        <span v-if="isLogin">还没有账号？<a @click="toggleMode">立即注册</a></span>
        <span v-else>已有账号？<a @click="toggleMode">去登录</a></span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-view {
  min-height: calc(100vh - var(--navbar-height));
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-8) var(--space-4);
  background: linear-gradient(135deg, var(--primary-50) 0%, var(--bg-body) 100%);
}

.auth-card {
  background: var(--bg-card);
  border-radius: var(--radius-xl);
  padding: var(--space-8) var(--space-6);
  width: 100%;
  max-width: 440px;
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
  color: var(--text-tertiary);
  margin: 0;
}

.submit-btn {
  width: 100%;
  margin-top: var(--space-2);
  font-size: var(--text-lg);
  font-weight: 500;
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

@media (max-width: 480px) {
  .auth-card {
    padding: var(--space-6) var(--space-4);
  }

  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
