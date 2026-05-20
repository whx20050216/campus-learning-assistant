<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const features = [
  {
    icon: '📤',
    title: '上传资料',
    desc: '支持 PDF、PPT、图片等多种格式，自动进行 OCR 文字识别与知识提取',
    path: '/upload',
    color: '#409eff'
  },
  {
    icon: '🔍',
    title: '智能搜索',
    desc: '基于 OCR 和 NLP 的知识库检索，支持关键词、标签、全文多模式搜索',
    path: '/search',
    color: '#67c23a'
  },
  {
    icon: '📝',
    title: '学习计划',
    desc: '创建个性化学习计划，跟踪进度，高效管理你的学习资料与任务',
    path: '/plans',
    color: '#e6a23c'
  },
  {
    icon: '📊',
    title: '数据分析',
    desc: '可视化展示学习数据，分析资料分布、学习进度与知识掌握情况',
    path: '/analysis',
    color: '#f56c6c'
  }
]

function goTo(path: string) {
  router.push(path)
}
</script>

<template>
  <div class="home-view">
    <!-- Hero 区域 -->
    <section class="hero-section">
      <div class="hero-content">
        <h1 class="hero-title">📚 校园智能学习助手</h1>
        <p class="hero-subtitle">
          基于 OCR 与 AI 技术的智能化学习资料管理平台<br />
          让知识获取更高效、学习规划更科学
        </p>
        <div class="hero-actions">
          <button class="hero-btn hero-btn-primary" @click="goTo('/upload')">
            📤 上传资料
          </button>
          <button class="hero-btn hero-btn-secondary" @click="goTo('/search')">
            🔍 智能搜索
          </button>
        </div>
      </div>
    </section>

    <!-- 功能入口 -->
    <section class="features-section">
      <h2 class="section-title-center">核心功能</h2>
      <div class="features-grid">
        <div
          v-for="item in features"
          :key="item.path"
          class="feature-card card-hover"
          @click="goTo(item.path)"
        >
          <div class="feature-icon" :style="{ background: item.color + '15', color: item.color }">
            {{ item.icon }}
          </div>
          <h3 class="feature-title">{{ item.title }}</h3>
          <p class="feature-desc">{{ item.desc }}</p>
          <span class="feature-link" :style="{ color: item.color }">
            立即使用 →
          </span>
        </div>
      </div>
    </section>

    <!-- 特色介绍 -->
    <section class="highlights-section">
      <div class="highlight-item">
        <div class="highlight-icon">🤖</div>
        <h3>AI 智能识别</h3>
        <p>基于 OCR 技术自动提取 PDF、图片中的文字内容，支持中英文混合识别</p>
      </div>
      <div class="highlight-item">
        <div class="highlight-icon">⚡</div>
        <h3>极速检索</h3>
        <p>多维度全文检索，秒级返回搜索结果，精准定位知识点</p>
      </div>
      <div class="highlight-item">
        <div class="highlight-icon">📈</div>
        <h3>学习追踪</h3>
        <p>量化分析学习进度，数据驱动，科学规划学习路径</p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.home-view {
  width: 100%;
  background: var(--bg-body);
}

/* Hero 区域 */
.hero-section {
  background: linear-gradient(135deg, var(--primary-500) 0%, var(--primary-600) 100%);
  padding: var(--space-16) var(--space-6);
  text-align: center;
  color: var(--text-on-primary);
}

.hero-content {
  max-width: 800px;
  margin: 0 auto;
}

.hero-title {
  font-size: var(--text-5xl);
  font-weight: 700;
  margin-bottom: var(--space-5);
  letter-spacing: 2px;
  line-height: 1.2;
}

.hero-subtitle {
  font-size: var(--text-lg);
  line-height: 1.8;
  opacity: 0.95;
  margin-bottom: var(--space-8);
}

.hero-actions {
  display: flex;
  gap: var(--space-4);
  justify-content: center;
  flex-wrap: wrap;
}

.hero-btn {
  padding: 14px var(--space-8);
  border-radius: var(--radius-md);
  font-size: var(--text-lg);
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
  line-height: 1;
}

.hero-btn-primary {
  background: var(--text-on-primary);
  color: var(--primary-500);
  box-shadow: var(--shadow-md);
}

.hero-btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
}

.hero-btn-secondary {
  background: rgba(255, 255, 255, 0.15);
  color: var(--text-on-primary);
  border: 1.5px solid rgba(255, 255, 255, 0.5);
}

.hero-btn-secondary:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: translateY(-2px);
}

/* 功能入口 */
.features-section {
  padding: var(--space-12) var(--space-6);
  max-width: var(--page-max-width);
  margin: 0 auto;
}

.section-title-center {
  text-align: center;
  font-size: var(--text-3xl);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--space-8);
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: var(--space-6);
}

.feature-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--space-8) var(--space-6);
  text-align: center;
  cursor: pointer;
  transition: all 0.25s ease;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
}

.feature-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-lg);
  border-color: transparent;
}

.feature-icon {
  width: 64px;
  height: 64px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  margin: 0 auto var(--space-4);
}

.feature-title {
  font-size: var(--text-xl);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--space-2);
}

.feature-desc {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
  line-height: 1.6;
  margin-bottom: var(--space-4);
}

.feature-link {
  font-size: var(--text-sm);
  font-weight: 600;
}

/* 特色介绍 */
.highlights-section {
  background: var(--bg-card);
  padding: var(--space-12) var(--space-6);
  display: flex;
  justify-content: center;
  gap: var(--space-12);
  flex-wrap: wrap;
  border-top: 1px solid var(--border-color);
}

.highlight-item {
  text-align: center;
  max-width: 280px;
}

.highlight-icon {
  font-size: 40px;
  line-height: 1;
  margin-bottom: var(--space-3);
}

.highlight-item h3 {
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--space-2);
}

.highlight-item p {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
  line-height: 1.6;
}

/* 响应式 */
@media (max-width: 768px) {
  .hero-section {
    padding: var(--space-10) var(--space-4);
  }

  .hero-title {
    font-size: var(--text-4xl);
  }

  .hero-subtitle {
    font-size: var(--text-base);
  }

  .features-section,
  .highlights-section {
    padding: var(--space-8) var(--space-4);
  }

  .section-title-center {
    font-size: var(--text-2xl);
  }

  .highlights-section {
    gap: var(--space-8);
  }
}
</style>
