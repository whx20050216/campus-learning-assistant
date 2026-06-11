<template>
  <div class="page-container exam-answer-page">
    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading" size="32"><Loading /></el-icon>
      <p>加载试卷中...</p>
    </div>

    <template v-else-if="paper">
      <!-- 顶部信息栏 -->
      <div class="exam-header">
        <div class="exam-header-left">
          <el-button size="small" @click="router.push('/exams')">
            <el-icon><ArrowLeft /></el-icon> 返回列表
          </el-button>
          <h2 class="exam-title">{{ paper.title }}</h2>
          <el-tag size="small" type="info">{{ paper.courseTag }}</el-tag>
        </div>
        <div class="exam-header-right">
          <div class="exam-timer">
            <el-icon><Timer /></el-icon>
            <span>{{ formatTimer(timer) }}</span>
          </div>
          <el-button v-if="!submitted" type="primary" @click="handleSubmit">
            提交试卷
          </el-button>
        </div>
      </div>

      <!-- 题目区域 -->
      <div class="question-area">
        <div v-if="currentQuestion" class="question-card">
          <div class="question-header">
            <div class="question-meta">
              <span class="question-index">第 {{ currentIndex + 1 }} / {{ questions.length }} 题</span>
              <el-tag size="small" :type="difficultyType(currentQuestion.difficulty)" effect="light">
                {{ difficultyLabel(currentQuestion.difficulty) }}
              </el-tag>
              <el-tag v-if="currentQuestion.knowledgePoint" size="small" type="info" effect="plain">
                {{ currentQuestion.knowledgePoint }}
              </el-tag>
            </div>
            <el-button v-if="!submitted" size="small" plain @click="handleRegenerate">
              <el-icon><RefreshRight /></el-icon> 换一题
            </el-button>
          </div>

          <div class="question-content">{{ currentQuestion.content }}</div>

          <!-- 单选题 -->
          <div v-if="currentQuestion.type === 'single'" class="question-options">
            <el-radio-group v-model="answers[currentQuestion.id]" :disabled="submitted" class="option-list">
              <el-radio v-for="(opt, idx) in currentQuestion.options" :key="idx" :label="opt.charAt(0)">
                {{ opt }}
              </el-radio>
            </el-radio-group>
          </div>

          <!-- 多选题 -->
          <div v-else-if="currentQuestion.type === 'multiple'" class="question-options">
            <el-checkbox-group v-model="multiAnswers[currentQuestion.id]" :disabled="submitted" class="option-list">
              <el-checkbox v-for="(opt, idx) in currentQuestion.options" :key="idx" :label="opt.charAt(0)">
                {{ opt }}
              </el-checkbox>
            </el-checkbox-group>
          </div>

          <!-- 判断题 -->
          <div v-else-if="currentQuestion.type === 'judge'" class="question-options">
            <el-radio-group v-model="answers[currentQuestion.id]" :disabled="submitted" class="option-list">
              <el-radio label="true">正确</el-radio>
              <el-radio label="false">错误</el-radio>
            </el-radio-group>
          </div>

          <!-- 简答题 -->
          <div v-else-if="currentQuestion.type === 'essay'" class="question-options">
            <el-input
              v-model="answers[currentQuestion.id]"
              type="textarea"
              :rows="4"
              placeholder="请输入您的答案..."
              :disabled="submitted"
            />
          </div>

          <!-- 提交后显示解析 -->
          <div v-if="submitted && currentQuestion" class="analysis-box">
            <el-divider />
            <div class="analysis-row">
              <span class="analysis-label">正确答案：</span>
              <span class="analysis-answer">{{ formatAnswer(currentQuestion) }}</span>
            </div>
            <div v-if="currentQuestion.analysis" class="analysis-row">
              <span class="analysis-label">解析：</span>
              <span class="analysis-text">{{ currentQuestion.analysis }}</span>
            </div>
            <div class="analysis-row">
              <span class="analysis-label">您的答案：</span>
              <span :class="isCorrect(currentQuestion) ? 'correct-text' : 'wrong-text'">
                {{ formatUserAnswer(currentQuestion) || '未作答' }}
              </span>
              <el-tag v-if="isCorrect(currentQuestion)" size="small" type="success" effect="light">正确</el-tag>
              <el-tag v-else size="small" type="danger" effect="light">错误</el-tag>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部导航 -->
      <div class="exam-footer">
        <el-button :disabled="currentIndex === 0" @click="currentIndex--">
          <el-icon><ArrowLeft /></el-icon> 上一题
        </el-button>
        <div class="question-dots">
          <span
            v-for="(q, idx) in questions"
            :key="q.id"
            class="dot"
            :class="{
              active: idx === currentIndex,
              answered: !!answers[q.id] || (multiAnswers[q.id] && multiAnswers[q.id].length > 0),
              correct: submitted && isCorrect(q),
              wrong: submitted && !isCorrect(q)
            }"
            @click="currentIndex = idx"
          >
            {{ idx + 1 }}
          </span>
        </div>
        <el-button :disabled="currentIndex === questions.length - 1" @click="currentIndex++">
          下一题 <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>
    </template>

    <!-- 结果弹窗 -->
    <el-dialog v-model="resultVisible" title="答题结果" width="500px" align-center :close-on-click-modal="false">
      <div v-if="record" class="result-body">
        <div class="result-score">{{ record.score }}<span class="result-unit">分</span></div>
        <div class="result-detail">
          <span>正确 {{ record.correctCount }} / {{ record.totalCount }} 题</span>
          <span>用时 {{ formatTimer(record.spentTimeSeconds) }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="resultVisible = false">查看解析</el-button>
        <el-button type="primary" @click="goBack">返回列表</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading, ArrowLeft, ArrowRight, Timer, RefreshRight } from '@element-plus/icons-vue'
import { getExamDetail, submitAnswers, regenerateQuestion, type ExamPaperVO, type ExamQuestionVO, type ExamRecordVO } from '@/api/exam'

const route = useRoute()
const router = useRouter()
const paperId = Number(route.params.id)

const loading = ref(false)
const paper = ref<ExamPaperVO | null>(null)
const questions = ref<ExamQuestionVO[]>([])
const answers = ref<Record<number, string>>({})
const multiAnswers = ref<Record<number, string[]>>({})
const currentIndex = ref(0)
const submitted = ref(false)
const resultVisible = ref(false)
const record = ref<ExamRecordVO | null>(null)
const timer = ref(0)
let timerInterval: ReturnType<typeof setInterval> | null = null

const currentQuestion = computed(() => questions.value[currentIndex.value] || null)

onMounted(() => {
  loadPaper()
  timerInterval = setInterval(() => timer.value++, 1000)
})

onBeforeUnmount(() => {
  if (timerInterval) clearInterval(timerInterval)
})

async function loadPaper() {
  loading.value = true
  try {
    const res = await getExamDetail(paperId)
    paper.value = res.data
    questions.value = res.data.questions || []
  } catch {
    ElMessage.error('加载试卷失败')
  } finally {
    loading.value = false
  }
}

function difficultyType(d: string) {
  const map: Record<string, string> = { easy: 'success', medium: 'warning', hard: 'danger' }
  return map[d] || 'info'
}
function difficultyLabel(d: string) {
  const map: Record<string, string> = { easy: '简单', medium: '中等', hard: '困难' }
  return map[d] || d
}

function formatTimer(sec: number) {
  const m = Math.floor(sec / 60).toString().padStart(2, '0')
  const s = (sec % 60).toString().padStart(2, '0')
  return `${m}:${s}`
}

function formatAnswer(q: ExamQuestionVO) {
  if (q.type === 'judge') return q.answer === 'true' ? '正确' : '错误'
  return q.answer || '-'
}

function formatUserAnswer(q: ExamQuestionVO) {
  if (q.type === 'multiple') {
    const arr = multiAnswers.value[q.id] || []
    return arr.sort().join('')
  }
  const ans = answers.value[q.id]
  if (q.type === 'judge') return ans === 'true' ? '正确' : ans === 'false' ? '错误' : ans
  return ans
}

function isCorrect(q: ExamQuestionVO) {
  const user = formatUserAnswer(q)
  if (!user) return false
  if (q.type === 'multiple') {
    const correct = (q.answer || '').split('').sort().join('')
    return user === correct
  }
  if (q.type === 'essay') return false // 简答不自动评分
  return user === q.answer
}

async function handleSubmit() {
  try {
    await ElMessageBox.confirm('提交后不可修改，确认提交？', '提交试卷', { type: 'info' })
  } catch {
    return
  }

  if (timerInterval) {
    clearInterval(timerInterval)
    timerInterval = null
  }

  // 合并多选答案
  const payloadAnswers: Record<number, string> = {}
  for (const q of questions.value) {
    if (q.type === 'multiple') {
      payloadAnswers[q.id] = (multiAnswers.value[q.id] || []).sort().join('')
    } else {
      payloadAnswers[q.id] = answers.value[q.id] || ''
    }
  }

  try {
    const res = await submitAnswers(paperId, {
      answers: payloadAnswers,
      spentTimeSeconds: timer.value
    })
    record.value = res.data
    submitted.value = true
    resultVisible.value = true
    ElMessage.success('提交成功')
  } catch {
    ElMessage.error('提交失败')
  }
}

async function handleRegenerate() {
  const q = currentQuestion.value
  if (!q) return
  try {
    await ElMessageBox.confirm('换题后当前答案将丢失，确认更换？', '提示', { type: 'warning' })
    const res = await regenerateQuestion(paperId, q.id)
    questions.value[currentIndex.value] = res.data
    delete answers.value[q.id]
    delete multiAnswers.value[q.id]
    ElMessage.success('换题成功')
  } catch {
    // 取消
  }
}

function goBack() {
  router.push('/exams')
}
</script>

<style scoped>
.exam-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-6);
  padding: var(--space-4);
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color);
}
.exam-header-left {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}
.exam-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}
.exam-header-right {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}
.exam-timer {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  font-size: 16px;
  font-weight: 500;
  color: var(--primary-500);
  font-family: monospace;
}
.question-area {
  max-width: 800px;
  margin: 0 auto var(--space-6);
}
.question-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  border: 1px solid var(--border-color);
}
.question-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-4);
}
.question-meta {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}
.question-index {
  font-weight: 600;
  color: var(--text-primary);
}
.question-content {
  font-size: 16px;
  line-height: 1.7;
  color: var(--text-primary);
  margin-bottom: var(--space-4);
  white-space: pre-wrap;
}
.option-list {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-3);
  width: 100%;
}
:deep(.option-list .el-radio),
:deep(.option-list .el-checkbox) {
  height: auto;
  align-items: flex-start;
  white-space: normal;
  line-height: 1.6;
  padding: var(--space-2) 0;
  margin-right: 0;
}
.analysis-box {
  margin-top: var(--space-4);
  padding: var(--space-4);
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
}
.analysis-row {
  margin-bottom: var(--space-2);
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-wrap: wrap;
}
.analysis-label {
  font-weight: 600;
  color: var(--text-secondary);
}
.analysis-answer {
  color: var(--success-500);
  font-weight: 600;
}
.correct-text {
  color: var(--success-500);
}
.wrong-text {
  color: var(--danger-500);
}
.exam-footer {
  position: sticky;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4) var(--space-6);
  background: var(--bg-card);
  border-top: 1px solid var(--border-color);
  border-radius: var(--radius-lg) var(--radius-lg) 0 0;
  margin: 0 calc(-1 * var(--space-4));
}
.question-dots {
  display: flex;
  gap: var(--space-2);
  flex-wrap: wrap;
  justify-content: center;
}
.dot {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border: 1px solid var(--border-color);
  background: var(--bg-secondary);
  color: var(--text-secondary);
  transition: all 0.2s;
}
.dot.active {
  border-color: var(--primary-500);
  color: var(--primary-500);
  background: var(--primary-50);
}
.dot.answered {
  background: var(--primary-100);
  color: var(--primary-600);
}
.dot.correct {
  background: var(--success-100);
  border-color: var(--success-500);
  color: var(--success-600);
}
.dot.wrong {
  background: var(--danger-100);
  border-color: var(--danger-500);
  color: var(--danger-600);
}
.result-body {
  text-align: center;
  padding: var(--space-6);
}
.result-score {
  font-size: 48px;
  font-weight: 700;
  color: var(--primary-500);
  line-height: 1;
}
.result-unit {
  font-size: 20px;
  font-weight: 400;
  color: var(--text-secondary);
  margin-left: 4px;
}
.result-detail {
  margin-top: var(--space-4);
  display: flex;
  justify-content: center;
  gap: var(--space-6);
  color: var(--text-secondary);
  font-size: 14px;
}
</style>
