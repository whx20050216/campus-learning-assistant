<template>
  <div class="page-container about-view">
    <div class="about-card">
      <h1 class="page-title">关于校园智能学习助手</h1>
      <p class="about-text">
        校园智能学习助手是一款基于 OCR 与 AI 技术的智能化学习资料管理平台，
        旨在帮助高校学生更高效地管理学习资料、制定学习计划、追踪学习进度。
      </p>
      <p class="about-text">
        核心功能包括：资料上传与 OCR 识别、智能搜索与知识检索、个性化学习计划制定、
        学习数据分析与可视化展示。
      </p>
      <div class="about-footer">
        <span class="about-version">Version 1.0.0</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.about-view {
  min-height: calc(100vh - var(--navbar-height));
  display: flex;
  align-items: center;
  justify-content: center;
}

.about-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--space-10) var(--space-8);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
  max-width: 640px;
  width: 100%;
  text-align: center;
}

.about-card .page-title {
  margin-bottom: var(--space-6);
}

.about-text {
  color: var(--text-secondary);
  font-size: var(--text-base);
  line-height: 1.8;
  margin-bottom: var(--space-4);
}

.about-footer {
  margin-top: var(--space-8);
  padding-top: var(--space-4);
  border-top: 1px solid var(--border-color);
}

.about-version {
  color: var(--text-tertiary);
  font-size: var(--text-sm);
  font-family: var(--font-mono);
}

@media (max-width: 768px) {
  .about-card {
    padding: var(--space-8) var(--space-5);
  }
}
</style>
