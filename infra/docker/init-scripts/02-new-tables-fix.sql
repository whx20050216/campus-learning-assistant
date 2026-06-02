-- =============================================
-- 批次 Y 修复脚本：01-init.sql 已包含完整最新表结构
-- 本文件仅保留必要的兼容性/增量修改
-- =============================================

USE learning_assistant;

-- 注意：users 表已由 01-init.sql 创建，包含完整字段（student_no/major/grade/role/status/storage_quota/used_storage/avatar_url）
-- 如需对 materials 表做额外 ALTER，请在此添加（保持幂等）
