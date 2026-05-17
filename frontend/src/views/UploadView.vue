<template>
  <div style="padding: 20px; max-width: 500px; margin: 0 auto;">
    <h2>📁 文件上传测试</h2>

    <div style="margin: 20px 0;">
      <label>用户ID：</label>
      <input v-model="userId" type="number" value="1" style="padding: 5px; width: 200px;" />
    </div>

    <div style="margin: 20px 0;">
      <label>选择文件：</label>
      <input type="file" @change="handleFileChange" accept=".pdf,.ppt,.pptx,.jpg,.jpeg,.png" />
    </div>

    <button
      @click="uploadFile"
      :disabled="!file || uploading"
      style="padding: 10px 20px; background: #409eff; color: white; border: none; border-radius: 4px; cursor: pointer;"
    >
      {{ uploading ? '上传中...' : '上传文件' }}
    </button>

    <div v-if="result" style="margin-top: 20px; padding: 10px; background: #f0f9eb; border-radius: 4px;">
      <strong>结果：</strong>{{ result }}
    </div>

    <div v-if="error" style="margin-top: 20px; padding: 10px; background: #fef0f0; color: #f56c6c; border-radius: 4px;">
      <strong>错误：</strong>{{ error }}
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const userId = ref(1)
const file = ref(null)
const uploading = ref(false)
const result = ref('')
const error = ref('')

const handleFileChange = (e) => {
  file.value = e.target.files[0]
  result.value = ''
  error.value = ''
}

const uploadFile = async () => {
  if (!file.value) {
    error.value = '请先选择文件'
    return
  }

  uploading.value = true
  result.value = ''
  error.value = ''

  const formData = new FormData()
  formData.append('file', file.value)
  formData.append('userId', userId.value)

  try {
    const response = await fetch('/api/materials/upload', {
      method: 'POST',
      body: formData
    })

    const text = await response.text()
    if (response.ok) {
      result.value = text
    } else {
      error.value = text || '上传失败'
    }
  } catch (err) {
    error.value = '请求失败：' + err.message + '（请确认服务是否正常运行）'
  } finally {
    uploading.value = false
  }
}
</script>
