import request from './request'

export interface ExamQuestionVO {
  id: number
  paperId: number
  materialId?: number
  type: string
  content: string
  options?: string[] | null
  answer?: string
  analysis?: string
  difficulty: string
  knowledgePoint?: string
  sortOrder: number
}

export interface ExamPaperVO {
  id: number
  userId: number
  studyPlanId?: number
  courseTag: string
  title: string
  materialIds: string
  questionCount: number
  difficulty: string
  status: string
  createdAt: string
  updatedAt: string
  questions?: ExamQuestionVO[]
}

export interface ExamRecordVO {
  id: number
  paperId: number
  userId: number
  answers?: Record<string, string>
  score: number
  correctCount: number
  totalCount: number
  spentTimeSeconds: number
  status: string
  createdAt: string
}

export interface GenerateExamDTO {
  materialIds: number[]
  courseTag: string
  questionCount: number
  types: string[]
  difficulty: string
}

export interface GenerateFromPlanDTO {
  studyPlanId: number
  materialIds: number[]
  courseTag: string
  questionCount?: number
  difficulty?: string
}

export interface SubmitAnswersDTO {
  answers: Record<number, string>
  spentTimeSeconds: number
}

export function generateExam(data: GenerateExamDTO) {
  return request.post<any, { code: number; msg: string; data: ExamPaperVO }>('/api/exam/generate', data)
}

export function generateExamFromPlan(data: GenerateFromPlanDTO) {
  return request.post<any, { code: number; msg: string; data: ExamPaperVO }>('/api/exam/generate-from-plan', data)
}

export function getExamList(page = 0, size = 10) {
  return request.get<any, { code: number; msg: string; data: { records: ExamPaperVO[]; total: number } }>(`/api/exam/papers?page=${page}&size=${size}`)
}

export function getExamDetail(id: number) {
  return request.get<any, { code: number; msg: string; data: ExamPaperVO }>(`/api/exam/papers/${id}`)
}

export function updateExam(id: number, title: string, status?: string) {
  const body: Record<string, string> = { title }
  if (status) body.status = status
  return request.put<any, { code: number; msg: string; data: any }>(`/api/exam/papers/${id}`, body)
}

export function deleteExam(id: number) {
  return request.delete<any, { code: number; msg: string; data: any }>(`/api/exam/papers/${id}`)
}

export function regenerateQuestion(paperId: number, questionId: number) {
  return request.post<any, { code: number; msg: string; data: ExamQuestionVO }>(`/api/exam/papers/${paperId}/regenerate/${questionId}`)
}

export function submitAnswers(paperId: number, data: SubmitAnswersDTO) {
  return request.post<any, { code: number; msg: string; data: ExamRecordVO }>(`/api/exam/papers/${paperId}/submit`, data)
}

export function getExamRecord(paperId: number) {
  return request.get<any, { code: number; msg: string; data: ExamRecordVO }>(`/api/exam/papers/${paperId}/record`)
}
