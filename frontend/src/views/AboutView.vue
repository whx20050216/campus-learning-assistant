<template>
  <div class="page-container about-view">
    <div class="about-card">
      <!-- 顶部 -->
      <div class="about-header">
        <div class="about-logo"><el-icon :size="48"><Collection /></el-icon></div>
        <h1 class="about-title">校园智能学习系统</h1>
        <el-tag type="primary" effect="light" class="about-version">v1.0.0</el-tag>
      </div>

      <!-- 项目介绍 -->
      <section class="about-section">
        <h2 class="section-title">项目介绍</h2>
        <blockquote class="about-quote">
          校园智能学习助手是一款基于 OCR 与 AI 技术的智能化学习资料管理平台，
          旨在帮助高校学生更高效地管理学习资料、制定学习计划、追踪学习进度。
          核心功能包括：资料上传与 OCR 识别、智能搜索与知识检索、个性化学习计划制定、
          学习数据分析与可视化展示。
        </blockquote>
      </section>

      <!-- 团队信息 -->
      <section class="about-section">
        <h2 class="section-title">开发团队</h2>
        <div class="team-grid">
          <div v-for="member in teamMembers" :key="member.name" class="team-card">
            <div class="team-avatar">{{ member.avatar }}</div>
            <div class="team-name">{{ member.name }}</div>
            <div class="team-role">{{ member.role }}</div>
            <div class="team-id">{{ member.id }}</div>
          </div>
        </div>
      </section>

      <!-- 技术栈 -->
      <section class="about-section">
        <h2 class="section-title">技术栈</h2>
        <div class="tech-stack">
          <el-tag
            v-for="tech in techStack"
            :key="tech"
            :type="['primary', 'success', 'warning', 'danger', 'info'][techStack.indexOf(tech) % 5]"
            effect="light"
            size="large"
            class="tech-tag"
          >
            {{ tech }}
          </el-tag>
        </div>
      </section>

      <!-- 底部版权 -->
      <div class="about-footer">
        <p>基于需求说明书 v1.0 开发</p>
        <p class="copyright">校园智能学习系统 · 课程设计项目</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Collection } from '@element-plus/icons-vue'
const teamMembers = [
  { name: '魏瑄', role: '项目经理 / 后端开发', id: '学号: 2021001', avatar: '魏' },
  { name: '姜凯恩', role: '前端开发 / UI 设计', id: '学号: 2021002', avatar: '姜' },
  { name: '唐杰', role: 'AI 引擎开发', id: '学号: 2021003', avatar: '唐' },
  { name: '王瑞涵', role: '测试 / 文档', id: '学号: 2021004', avatar: '王' },
  { name: '王和宵', role: '后端开发 / 运维', id: '学号: 2021005', avatar: '王' },
]

const techStack = [
  'Vue 3', 'TypeScript', 'Element Plus', 'ECharts',
  'Spring Boot', 'MyBatis-Plus', 'FastAPI',
  'MySQL', 'Redis', 'MinIO',
  'PaddleOCR', '智谱 AI', 'Docker'
]
</script>

<style scoped>
.about-view {
  min-height: calc(100vh - var(--navbar-height));
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-6);
}

.about-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--border-color);
  max-width: 800px;
  width: 100%;
  overflow: hidden;
}

/* 顶部 */
.about-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--space-10) var(--space-8);
  background: linear-gradient(135deg, #F8FAFC 0%, #EEF2FF 100%);
  border-bottom: 1px solid var(--border-color);
}

.about-logo {
  margin-bottom: var(--space-4);
  color: var(--primary-500);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.about-title {
  font-size: var(--text-3xl);
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 var(--space-2);
}

.about-version {
  font-family: var(--font-mono);
  font-weight: 500;
}

/* 内容区 */
.about-section {
  padding: var(--space-6) var(--space-8);
  border-bottom: 1px solid var(--border-color);
}

.about-section:last-of-type {
  border-bottom: none;
}

.section-title {
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 var(--space-4);
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

/* 引用块 */
.about-quote {
  margin: 0;
  padding: var(--space-4) var(--space-5);
  background: var(--bg-hover);
  border-left: 3px solid var(--primary-500);
  border-radius: 0 var(--radius-md) var(--radius-md) 0;
  font-size: var(--text-base);
  color: var(--text-secondary);
  line-height: 1.8;
}

/* 团队卡片 */
.team-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
}

.team-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-4);
  background: var(--bg-hover);
  border-radius: var(--radius-md);
  text-align: center;
  transition: all 0.2s var(--ease-out);
}

.team-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-sm);
}

.team-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary-500) 0%, var(--primary-600) 100%);
  color: #fff;
  font-size: 22px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.team-name {
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--text-primary);
}

.team-role {
  font-size: var(--text-xs);
  color: var(--text-secondary);
}

.team-id {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  font-family: var(--font-mono);
}

/* 技术栈 */
.tech-stack {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.tech-tag {
  font-size: var(--text-sm);
  font-weight: 500;
}

/* 底部 */
.about-footer {
  padding: var(--space-5) var(--space-8);
  text-align: center;
  background: var(--bg-hover);
  border-top: 1px solid var(--border-color);
  font-size: var(--text-sm);
  color: var(--text-secondary);
}

.about-footer p {
  margin: 0;
}

.copyright {
  margin-top: var(--space-1) !important;
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}

@media (max-width: 768px) {
  .team-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .about-header {
    padding: var(--space-8) var(--space-5);
  }

  .about-section {
    padding: var(--space-5);
  }

  .about-footer {
    padding: var(--space-4) var(--space-5);
  }
}

@media (max-width: 480px) {
  .team-grid {
    grid-template-columns: 1fr;
  }
}
</style>
