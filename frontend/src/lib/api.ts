export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export type UserRole = 'ADMIN' | 'TEACHER' | 'STUDENT'

export interface UserInfo {
  id: number
  username: string
  email: string
  role: UserRole
  status: number
  avatar?: string
}

export interface AdminUser extends UserInfo {
  bio?: string
  teachingYears?: number
  expertise?: string
  createdAt?: string
  updatedAt?: string
}

export type TeacherProfile = AdminUser

export interface LoginResponse {
  token: string
  user: UserInfo
}

export interface Category {
  id: number
  name: string
  sort: number
}

export interface Chapter {
  id: number
  courseId: number
  title: string
  videoPath?: string
  duration: number
  sort: number
}

export interface Course {
  id: number
  title: string
  cover?: string
  intro?: string
  categoryId: number
  categoryName?: string
  teacherId: number
  teacherName?: string
  price: number
  status: string
  favoritesCount: number
  favorite?: boolean
  accessible?: boolean
  purchased?: boolean
  chapters?: Chapter[]
  createdAt?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
}

export interface LiveRoom {
  id: number
  title: string
  cover?: string
  intro?: string
  teacherId: number
  teacherName?: string
  status: 'SCHEDULED' | 'LIVE' | 'ENDED'
  scheduledTime?: string
  startTime?: string
  endTime?: string
  onlineCount: number
  agoraAppId?: string
  agoraChannel?: string
  agoraToken?: string
}

export interface LivePoll {
  id: number
  roomId: number
  question: string
  options: string
}

export interface LivePollOptionResult {
  optionIndex: number
  optionText: string
  count: number
}

export interface LivePollDetail {
  id: number
  roomId: number
  question: string
  options: string[]
  selectedOptionIndex?: number | null
  results: LivePollOptionResult[]
  totalVotes: number
  createdAt?: string
}

export interface LiveMicRequest {
  id: number
  roomId: number
  userId: number
  username?: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'ENDED'
  createdAt?: string
  updatedAt?: string
}

export interface LiveReplay {
  id: number
  roomId?: number
  roomTitle?: string
  teacherId: number
  teacherName?: string
  title: string
  intro?: string
  videoPath?: string
  playUrl?: string
  fileSize: number
  duration: number
  status: 'DRAFT' | 'PUBLISHED'
  createdAt?: string
  updatedAt?: string
}

export interface Homework {
  id: number
  courseId: number
  courseTitle?: string
  teacherId: number
  teacherName?: string
  title: string
  description?: string
  deadline: string
  submissionId?: number
  submissionStatus?: 'PENDING' | 'SUBMITTED' | 'REVIEWED'
  submittedAt?: string
  reviewedAt?: string
  createdAt?: string
}

export interface HomeworkSubmission {
  id: number
  homeworkId: number
  homeworkTitle?: string
  courseId: number
  courseTitle?: string
  studentId: number
  studentName?: string
  content?: string
  attachmentPath?: string
  attachmentUrl?: string
  score?: number
  comment?: string
  submittedAt?: string
  reviewedAt?: string
}

export interface Exam {
  id: number
  courseId: number
  courseTitle?: string
  teacherId: number
  teacherName?: string
  title: string
  description?: string
  startTime?: string
  endTime?: string
  durationMinutes?: number
  totalScore: number
  status: 'DRAFT' | 'PUBLISHED' | 'CLOSED'
  submissionId?: number
  submissionStatus?: 'PENDING' | 'SUBMITTED' | 'PENDING_REVIEW' | 'REVIEWED'
  score?: number
  submittedAt?: string
  reviewedAt?: string
  createdAt?: string
}

export interface ExamQuestion {
  id: number
  examId: number
  type: 'SINGLE' | 'MULTIPLE' | 'JUDGE' | 'SHORT'
  content: string
  options: string[]
  answer?: string
  score: number
  sort: number
  createdAt?: string
}

export interface ExamAnswer {
  id: number
  submissionId: number
  questionId: number
  questionType: 'SINGLE' | 'MULTIPLE' | 'JUDGE' | 'SHORT'
  questionContent: string
  options: string[]
  correctAnswer?: string
  answer?: string
  correct?: boolean
  score: number
  questionScore: number
  comment?: string
}

export interface ExamSubmission {
  id: number
  examId: number
  examTitle?: string
  courseId: number
  courseTitle?: string
  studentId: number
  studentName?: string
  objectiveScore: number
  subjectiveScore: number
  totalScore: number
  status: 'SUBMITTED' | 'PENDING_REVIEW' | 'REVIEWED'
  comment?: string
  submittedAt?: string
  reviewedAt?: string
  answers?: ExamAnswer[]
}

export interface LearningRecord {
  courseId: number
  courseTitle?: string
  chapterId: number
  chapterTitle?: string
  progress: number
  lastWatchTime?: string
}

export type RecentLearningRecord = LearningRecord

export interface StudentStats {
  learnedCourseCount: number
  favoriteCount: number
  homeworkSubmissionCount: number
  examParticipationCount: number
  averageScore: number
  recentRecords: RecentLearningRecord[]
}

export interface TeacherStats {
  courseCount: number
  liveCount: number
  replayCount: number
  homeworkCount: number
  examCount: number
  studentSubmissionCount: number
  pendingHomeworkCount: number
  pendingExamCount: number
}

export interface TeacherCourseStats {
  courseId: number
  courseTitle?: string
  studentCount: number
  homeworkCount: number
  homeworkSubmissionCount: number
  homeworkSubmissionRate: number
  examCount: number
  examSubmissionCount: number
  examSubmissionRate: number
  examAverageScore: number
  liveCount: number
  replayCount: number
}

export interface PlatformStats {
  userCount: number
  teacherCount: number
  studentCount: number
  courseCount: number
  liveCount: number
  replayCount: number
  homeworkCount: number
  examCount: number
}

export interface Announcement {
  id: number
  title: string
  content: string
  type: string
  status: 'DRAFT' | 'PUBLISHED' | 'OFFLINE'
  pinned: number
  creatorId?: number
  creatorName?: string
  publishTime?: string
  createdAt?: string
}

export interface UserMessage {
  id: number
  senderId: number
  senderName?: string
  receiverId: number
  receiverName?: string
  title: string
  content: string
  readStatus: number
  readAt?: string
  createdAt?: string
}

export interface BarrageRecord {
  id: number
  roomId: number
  roomTitle?: string
  userId: number
  username?: string
  content: string
  createdAt?: string
}

export interface BarrageKeyword {
  id: number
  keyword: string
  createdAt?: string
}

export interface SiteBanner {
  id: number
  title: string
  subtitle?: string
  imageUrl: string
  linkUrl?: string
  sort: number
  status: 'ENABLED' | 'DISABLED'
  creatorId?: number
  creatorName?: string
  createdAt?: string
}

export interface CourseOrder {
  id: number
  orderNo: string
  courseId: number
  courseTitle?: string
  userId: number
  amount: number
  status: 'PENDING' | 'PAID' | 'CANCELLED'
  paidAt?: string
  createdAt?: string
}

// ─── 格式化工具 ────────────────────────────────────────────────
export function formatCourseStatus(status: string) {
  const map: Record<string, string> = { DRAFT: '草稿', REVIEWING: '审核中', PUBLISHED: '已发布', REJECTED: '已退回', OFFLINE: '已下线' }
  return map[status] ?? status
}

export function formatLiveStatus(status: string) {
  const map: Record<string, string> = { SCHEDULED: '待开播', LIVE: '直播中', ENDED: '已结束' }
  return map[status] ?? status
}

export function formatReplayStatus(status: string) {
  const map: Record<string, string> = { DRAFT: '草稿', PUBLISHED: '已发布' }
  return map[status] ?? status
}

export function formatExamStatus(status: string) {
  const map: Record<string, string> = { DRAFT: '草稿', PUBLISHED: '进行中', CLOSED: '已关闭' }
  return map[status] ?? status
}

export function formatDuration(seconds: number) {
  if (!seconds) return '--'
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  if (h > 0) return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  return `${m}:${String(s).padStart(2, '0')}`
}

export function formatDateTime(isoStr?: string) {
  if (!isoStr) return '--'
  const d = new Date(isoStr)
  if (isNaN(d.getTime())) return isoStr
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export function formatDeadline(isoStr?: string) {
  if (!isoStr) return '--'
  const d = new Date(isoStr)
  if (isNaN(d.getTime())) return isoStr
  const now = new Date()
  const diffMs = d.getTime() - now.getTime()
  const diffDays = Math.ceil(diffMs / 86400000)
  const base = `${d.getMonth() + 1}月${d.getDate()}日 ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  if (diffMs < 0) return `${base}（已截止）`
  if (diffDays <= 1) return `${base}（今日截止）`
  if (diffDays <= 3) return `${base}（还有${diffDays}天）`
  return base
}

const API_BASE = '/api'

export function getToken() {
  return localStorage.getItem('token')
}

export function setToken(token: string) {
  localStorage.setItem('token', token)
}

export function clearToken() {
  localStorage.removeItem('token')
}

export async function request<T>(url: string, options: RequestInit = {}) {
  const token = getToken()
  const headers = new Headers(options.headers)
  if (!headers.has('Content-Type') && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }
  const response = await fetch(`${API_BASE}${url}`, {
    ...options,
    headers,
  })
  const result = (await response.json()) as ApiResult<T>
  if (!response.ok || result.code !== 200) {
    throw new Error(result.message || '请求失败')
  }
  return result.data
}

export function apiUrl(path: string) {
  return `${API_BASE}${path}`
}

export const authApi = {
  register(payload: { username: string; password: string; email: string; role: string }) {
    return request<UserInfo>('/auth/register', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },
  login(payload: { username: string; password: string }) {
    return request<LoginResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },
  me() {
    return request<UserInfo>('/auth/me')
  },
  changePassword(payload: { oldPassword: string; newPassword: string }) {
    return request<void>('/auth/password', { method: 'PUT', body: JSON.stringify(payload) })
  },
  uploadAvatar(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return request<string>('/auth/avatar', { method: 'POST', body: formData })
  },
  teacherProfile() {
    return request<TeacherProfile>('/auth/teacher/profile')
  },
  updateTeacherProfile(payload: { bio?: string; teachingYears?: number; expertise?: string }) {
    return request<TeacherProfile>('/auth/teacher/profile', { method: 'PUT', body: JSON.stringify(payload) })
  },
}

export const categoryApi = {
  list() {
    return request<Category[]>('/categories')
  },
  create(payload: { name: string; sort: number }) {
    return request<Category>('/categories', { method: 'POST', body: JSON.stringify(payload) })
  },
  update(id: number, payload: { name: string; sort: number }) {
    return request<Category>(`/categories/${id}`, { method: 'PUT', body: JSON.stringify(payload) })
  },
  remove(id: number) {
    return request<void>(`/categories/${id}`, { method: 'DELETE' })
  },
}

export const announcementApi = {
  page(params: { current?: number; size?: number; keyword?: string; type?: string } = {}) {
    return request<PageResult<Announcement>>(`/announcements?${toSearch(params)}`)
  },
  detail(id: number) {
    return request<Announcement>(`/announcements/${id}`)
  },
  create(payload: { title: string; content: string; type: string; status: string; pinned: number; syncMessage?: boolean }) {
    return request<Announcement>('/announcements', { method: 'POST', body: JSON.stringify(payload) })
  },
  update(id: number, payload: { title: string; content: string; type: string; status: string; pinned: number; syncMessage?: boolean }) {
    return request<Announcement>(`/announcements/${id}`, { method: 'PUT', body: JSON.stringify(payload) })
  },
  remove(id: number) {
    return request<void>(`/announcements/${id}`, { method: 'DELETE' })
  },
  adminPage(params: { current?: number; size?: number; keyword?: string; type?: string; status?: string } = {}) {
    return request<PageResult<Announcement>>(`/announcements/admin?${toSearch(params)}`)
  },
  publish(id: number, syncMessage = false) {
    return request<Announcement>(`/announcements/${id}/publish?syncMessage=${syncMessage}`, { method: 'POST' })
  },
  offline(id: number) {
    return request<Announcement>(`/announcements/${id}/offline`, { method: 'POST' })
  },
}

export const bannerApi = {
  list() {
    return request<SiteBanner[]>('/banners')
  },
  adminList(params: { status?: string } = {}) {
    return request<SiteBanner[]>(`/banners/admin?${toSearch(params)}`)
  },
  create(payload: { title: string; subtitle?: string; imageUrl: string; linkUrl?: string; sort: number; status: string }) {
    return request<SiteBanner>('/banners', { method: 'POST', body: JSON.stringify(payload) })
  },
  update(id: number, payload: { title: string; subtitle?: string; imageUrl: string; linkUrl?: string; sort: number; status: string }) {
    return request<SiteBanner>(`/banners/${id}`, { method: 'PUT', body: JSON.stringify(payload) })
  },
  remove(id: number) {
    return request<void>(`/banners/${id}`, { method: 'DELETE' })
  },
}

export const messageApi = {
  page(params: { current?: number; size?: number; readStatus?: number | '' } = {}) {
    return request<PageResult<UserMessage>>(`/messages?${toSearch(params)}`)
  },
  unreadCount() {
    return request<number>('/messages/unread-count')
  },
  markRead(id: number) {
    return request<void>(`/messages/${id}/read`, { method: 'POST' })
  },
  markAllRead() {
    return request<void>('/messages/read-all', { method: 'POST' })
  },
}

export const adminApi = {
  users(params: { current?: number; size?: number; role?: UserRole | ''; status?: number | ''; keyword?: string } = {}) {
    return request<PageResult<AdminUser>>(`/admin/users?${toSearch(params)}`)
  },
  userDetail(id: number) {
    return request<AdminUser>(`/admin/users/${id}`)
  },
  createTeacher(payload: { username: string; password: string; email: string; bio?: string; teachingYears?: number; expertise?: string }) {
    return request<AdminUser>('/admin/teachers', { method: 'POST', body: JSON.stringify(payload) })
  },
  createAdmin(payload: { username: string; password: string; email: string }) {
    return request<AdminUser>('/admin/admins', { method: 'POST', body: JSON.stringify(payload) })
  },
  updateUserStatus(id: number, status: number) {
    return request<void>(`/admin/users/${id}/status?status=${status}`, { method: 'POST' })
  },
  resetUserPassword(id: number, payload: { newPassword: string }) {
    return request<void>(`/admin/users/${id}/password`, { method: 'POST', body: JSON.stringify(payload) })
  },
  deleteUser(id: number) {
    return request<void>(`/admin/users/${id}`, { method: 'DELETE' })
  },
  updateTeacherProfile(id: number, payload: { bio?: string; teachingYears?: number; expertise?: string }) {
    return request<AdminUser>(`/admin/teachers/${id}/profile`, { method: 'PUT', body: JSON.stringify(payload) })
  },
  courses(params: { current?: number; size?: number; status?: string } = {}) {
    return request<PageResult<Course>>(`/admin/courses?${toSearch(params)}`)
  },
  reviewCourse(id: number, approved: boolean) {
    return request<void>(`/admin/courses/${id}/review?approved=${approved}`, { method: 'POST' })
  },
  barrages(params: { current?: number; size?: number; roomId?: number | ''; keyword?: string; startTime?: string; endTime?: string } = {}) {
    return request<PageResult<BarrageRecord>>(`/admin/barrages?${toSearch(params)}`)
  },
  deleteBarrage(id: number) {
    return request<void>(`/admin/barrages/${id}`, { method: 'DELETE' })
  },
  barrageKeywords() {
    return request<BarrageKeyword[]>('/admin/barrage-keywords')
  },
  createBarrageKeyword(payload: { keyword: string }) {
    return request<BarrageKeyword>('/admin/barrage-keywords', { method: 'POST', body: JSON.stringify(payload) })
  },
  updateBarrageKeyword(id: number, payload: { keyword: string }) {
    return request<BarrageKeyword>(`/admin/barrage-keywords/${id}`, { method: 'PUT', body: JSON.stringify(payload) })
  },
  deleteBarrageKeyword(id: number) {
    return request<void>(`/admin/barrage-keywords/${id}`, { method: 'DELETE' })
  },
}

function toSearch(params: Record<string, unknown>) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== '') search.set(key, String(value))
  })
  return search.toString()
}

export const courseApi = {
  page(params: { current?: number; size?: number; categoryId?: number | ''; keyword?: string; status?: string; teacherId?: number } = {}) {
    return request<PageResult<Course>>(`/courses?${toSearch(params)}`)
  },
  detail(id: number) {
    return request<Course>(`/courses/${id}`)
  },
  create(payload: { title: string; cover?: string; intro?: string; categoryId: number; price: number }) {
    return request<Course>('/courses', { method: 'POST', body: JSON.stringify(payload) })
  },
  submitReview(id: number) {
    return request<void>(`/courses/${id}/submit-review`, { method: 'POST' })
  },
  review(id: number, approved: boolean) {
    return request<void>(`/courses/${id}/review?approved=${approved}`, { method: 'POST' })
  },
  createChapter(courseId: number, payload: { title: string; videoPath?: string; duration?: number; sort?: number }) {
    return request<Chapter>(`/courses/${courseId}/chapters`, { method: 'POST', body: JSON.stringify(payload) })
  },
  uploadChapterVideo(courseId: number, chapterId: number, file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return request<Chapter>(`/courses/${courseId}/chapters/${chapterId}/video`, { method: 'POST', body: formData })
  },
  chapterVideoUrl(courseId: number, chapterId: number) {
    return apiUrl(`/courses/${courseId}/chapters/${chapterId}/stream`)
  },
  favorite(id: number) {
    return request<void>(`/courses/${id}/favorite`, { method: 'POST' })
  },
  unfavorite(id: number) {
    return request<void>(`/courses/${id}/favorite`, { method: 'DELETE' })
  },
  favoriteCourses(params: { current?: number; size?: number } = {}) {
    return request<PageResult<Course>>(`/courses/favorites?${toSearch(params)}`)
  },
  learningRecords(params: { current?: number; size?: number } = {}) {
    return request<PageResult<LearningRecord>>(`/courses/learning-records?${toSearch(params)}`)
  },
  saveRecord(payload: { courseId: number; chapterId: number; progress: number }) {
    return request<void>('/courses/learning-records', { method: 'POST', body: JSON.stringify(payload) })
  },
}

export const replayApi = {
  page(params: { current?: number; size?: number; teacherId?: number } = {}) {
    return request<PageResult<LiveReplay>>(`/replays?${toSearch(params)}`)
  },
  detail(id: number) {
    return request<LiveReplay>(`/replays/${id}`)
  },
  mine(params: { current?: number; size?: number } = {}) {
    return request<PageResult<LiveReplay>>(`/replays/teacher/mine?${toSearch(params)}`)
  },
  create(payload: { roomId?: number; title: string; intro?: string; status?: string }) {
    return request<LiveReplay>('/replays', { method: 'POST', body: JSON.stringify(payload) })
  },
  update(id: number, payload: { roomId?: number; title: string; intro?: string; status?: string }) {
    return request<LiveReplay>(`/replays/${id}`, { method: 'PUT', body: JSON.stringify(payload) })
  },
  remove(id: number) {
    return request<void>(`/replays/${id}`, { method: 'DELETE' })
  },
  uploadVideo(id: number, file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return request<LiveReplay>(`/replays/${id}/video`, { method: 'POST', body: formData })
  },
}

export const liveApi = {
  page(params: { current?: number; size?: number; status?: string; teacherId?: number } = {}) {
    return request<PageResult<LiveRoom>>(`/live/rooms?${toSearch(params)}`)
  },
  detail(id: number) {
    return request<LiveRoom>(`/live/rooms/${id}`)
  },
  create(payload: { title: string; intro?: string; cover?: string; scheduledTime?: string }) {
    return request<LiveRoom>('/live/rooms', { method: 'POST', body: JSON.stringify(payload) })
  },
  start(id: number) {
    return request<LiveRoom>(`/live/rooms/${id}/start`, { method: 'POST' })
  },
  end(id: number) {
    return request<LiveRoom>(`/live/rooms/${id}/end`, { method: 'POST' })
  },
  chat(id: number, content: string) {
    return request<void>(`/live/rooms/${id}/chat`, { method: 'POST', body: JSON.stringify({ content }) })
  },
  barrage(id: number, content: string) {
    return request<void>(`/live/rooms/${id}/barrage`, { method: 'POST', body: JSON.stringify({ content }) })
  },
  createPoll(id: number, payload: { question: string; options: string[] }) {
    return request<LivePoll>(`/live/rooms/${id}/polls`, { method: 'POST', body: JSON.stringify(payload) })
  },
  polls(id: number) {
    return request<LivePollDetail[]>(`/live/rooms/${id}/polls`)
  },
  pollDetail(id: number) {
    return request<LivePollDetail>(`/live/polls/${id}`)
  },
  pollResults(id: number) {
    return request<LivePollDetail>(`/live/polls/${id}/results`)
  },
  vote(pollId: number, optionIndex: number) {
    return request<void>(`/live/polls/${pollId}/vote`, { method: 'POST', body: JSON.stringify({ optionIndex }) })
  },
  requestMic(id: number) {
    return request<LiveMicRequest>(`/live/rooms/${id}/mic-requests`, { method: 'POST' })
  },
  micRequests(id: number) {
    return request<LiveMicRequest[]>(`/live/rooms/${id}/mic-requests`)
  },
  handleMic(id: number, approved: boolean) {
    return request<LiveMicRequest>(`/live/mic-requests/${id}/handle?approved=${approved}`, { method: 'POST' })
  },
  endMic(id: number) {
    return request<LiveMicRequest>(`/live/mic-requests/${id}/end`, { method: 'POST' })
  },
  forceClose(id: number) {
    return request<void>(`/live/rooms/${id}/force-close`, { method: 'POST' })
  },
}

export const homeworkApi = {
  page(params: { current?: number; size?: number } = {}) {
    return request<PageResult<Homework>>(`/homeworks?${toSearch(params)}`)
  },
  detail(id: number) {
    return request<Homework>(`/homeworks/${id}`)
  },
  mine(params: { current?: number; size?: number; courseId?: number } = {}) {
    return request<PageResult<Homework>>(`/homeworks/teacher/mine?${toSearch(params)}`)
  },
  create(payload: { courseId: number; title: string; description?: string; deadline: string }) {
    return request<Homework>('/homeworks', { method: 'POST', body: JSON.stringify(payload) })
  },
  update(id: number, payload: { courseId: number; title: string; description?: string; deadline: string }) {
    return request<Homework>(`/homeworks/${id}`, { method: 'PUT', body: JSON.stringify(payload) })
  },
  remove(id: number) {
    return request<void>(`/homeworks/${id}`, { method: 'DELETE' })
  },
  submit(id: number, payload: { content?: string; file?: File }) {
    const formData = new FormData()
    if (payload.content) formData.append('content', payload.content)
    if (payload.file) formData.append('file', payload.file)
    return request<HomeworkSubmission>(`/homeworks/${id}/submit`, { method: 'POST', body: formData })
  },
  submissions(id: number, params: { current?: number; size?: number } = {}) {
    return request<PageResult<HomeworkSubmission>>(`/homeworks/${id}/submissions?${toSearch(params)}`)
  },
  grade(homeworkId: number, submissionId: number, payload: { score: number; comment?: string }) {
    return request<HomeworkSubmission>(`/homeworks/${homeworkId}/submissions/${submissionId}/grade`, { method: 'POST', body: JSON.stringify(payload) })
  },
  mySubmissions(params: { current?: number; size?: number } = {}) {
    return request<PageResult<HomeworkSubmission>>(`/homeworks/submissions/mine?${toSearch(params)}`)
  },
}

export const statsApi = {
  student() {
    return request<StudentStats>('/stats/student/me')
  },
  teacher() {
    return request<TeacherStats>('/stats/teacher/me')
  },
  teacherCourses(params: { courseId?: number } = {}) {
    return request<TeacherCourseStats[]>(`/stats/teacher/courses?${toSearch(params)}`)
  },
  platform() {
    return request<PlatformStats>('/stats/admin/platform')
  },
}

export const examApi = {
  page(params: { current?: number; size?: number } = {}) {
    return request<PageResult<Exam>>(`/exams?${toSearch(params)}`)
  },
  detail(id: number) {
    return request<Exam>(`/exams/${id}`)
  },
  questions(id: number) {
    return request<ExamQuestion[]>(`/exams/${id}/questions`)
  },
  mine(params: { current?: number; size?: number; courseId?: number } = {}) {
    return request<PageResult<Exam>>(`/exams/teacher/mine?${toSearch(params)}`)
  },
  create(payload: { courseId: number; title: string; description?: string; startTime?: string; endTime?: string; durationMinutes?: number }) {
    return request<Exam>('/exams', { method: 'POST', body: JSON.stringify(payload) })
  },
  update(id: number, payload: { courseId: number; title: string; description?: string; startTime?: string; endTime?: string; durationMinutes?: number }) {
    return request<Exam>(`/exams/${id}`, { method: 'PUT', body: JSON.stringify(payload) })
  },
  remove(id: number) {
    return request<void>(`/exams/${id}`, { method: 'DELETE' })
  },
  publish(id: number) {
    return request<Exam>(`/exams/${id}/publish`, { method: 'POST' })
  },
  close(id: number) {
    return request<Exam>(`/exams/${id}/close`, { method: 'POST' })
  },
  createQuestion(id: number, payload: { type: string; content: string; options?: string[]; answer: string; score: number; sort?: number }) {
    return request<ExamQuestion>(`/exams/${id}/questions`, { method: 'POST', body: JSON.stringify(payload) })
  },
  updateQuestion(id: number, questionId: number, payload: { type: string; content: string; options?: string[]; answer: string; score: number; sort?: number }) {
    return request<ExamQuestion>(`/exams/${id}/questions/${questionId}`, { method: 'PUT', body: JSON.stringify(payload) })
  },
  removeQuestion(id: number, questionId: number) {
    return request<void>(`/exams/${id}/questions/${questionId}`, { method: 'DELETE' })
  },
  submit(id: number, answers: { questionId: number; answer: string }[]) {
    return request<ExamSubmission>(`/exams/${id}/submit`, { method: 'POST', body: JSON.stringify({ answers }) })
  },
  result(id: number) {
    return request<ExamSubmission>(`/exams/${id}/result`)
  },
  submissions(id: number, params: { current?: number; size?: number } = {}) {
    return request<PageResult<ExamSubmission>>(`/exams/${id}/submissions?${toSearch(params)}`)
  },
  submissionDetail(examId: number, submissionId: number) {
    return request<ExamSubmission>(`/exams/${examId}/submissions/${submissionId}`)
  },
  grade(examId: number, submissionId: number, payload: { comment?: string; answers?: { answerId: number; score: number; comment?: string }[] }) {
    return request<ExamSubmission>(`/exams/${examId}/submissions/${submissionId}/grade`, { method: 'POST', body: JSON.stringify(payload) })
  },
  mySubmissions(params: { current?: number; size?: number } = {}) {
    return request<PageResult<ExamSubmission>>(`/exams/submissions/mine?${toSearch(params)}`)
  },
}

export const orderApi = {
  create(courseId: number) {
    return request<CourseOrder>(`/orders/courses/${courseId}`, { method: 'POST' })
  },
  pay(orderId: number) {
    return request<CourseOrder>(`/orders/mine/${orderId}/pay`, { method: 'POST' })
  },
  cancel(orderId: number) {
    return request<CourseOrder>(`/orders/mine/${orderId}/cancel`, { method: 'POST' })
  },
  mine(params: { current?: number; size?: number; status?: string } = {}) {
    return request<PageResult<CourseOrder>>(`/orders/mine?${toSearch(params)}`)
  },
}
