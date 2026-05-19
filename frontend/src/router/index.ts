import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '@/pages/HomePage.vue'
import LoginPage from '@/pages/LoginPage.vue'
import RegisterPage from '@/pages/RegisterPage.vue'
import DashboardPage from '@/pages/DashboardPage.vue'
import CourseListPage from '@/pages/CourseListPage.vue'
import CourseDetailPage from '@/pages/CourseDetailPage.vue'
import TeacherCoursesPage from '@/pages/TeacherCoursesPage.vue'
import LiveListPage from '@/pages/LiveListPage.vue'
import LiveRoomPage from '@/pages/LiveRoomPage.vue'
import TeacherLivePage from '@/pages/TeacherLivePage.vue'
import ReplayListPage from '@/pages/ReplayListPage.vue'
import ReplayDetailPage from '@/pages/ReplayDetailPage.vue'
import TeacherReplaysPage from '@/pages/TeacherReplaysPage.vue'
import HomeworkListPage from '@/pages/HomeworkListPage.vue'
import HomeworkDetailPage from '@/pages/HomeworkDetailPage.vue'
import TeacherHomeworksPage from '@/pages/TeacherHomeworksPage.vue'
import TeacherHomeworkSubmissionsPage from '@/pages/TeacherHomeworkSubmissionsPage.vue'
import ExamListPage from '@/pages/ExamListPage.vue'
import ExamDetailPage from '@/pages/ExamDetailPage.vue'
import ExamResultPage from '@/pages/ExamResultPage.vue'
import TeacherExamsPage from '@/pages/TeacherExamsPage.vue'
import TeacherExamQuestionsPage from '@/pages/TeacherExamQuestionsPage.vue'
import TeacherExamSubmissionsPage from '@/pages/TeacherExamSubmissionsPage.vue'
import StudentStatsPage from '@/pages/StudentStatsPage.vue'
import StudentLearningPage from '@/pages/StudentLearningPage.vue'
import StudentFavoritesPage from '@/pages/StudentFavoritesPage.vue'
import TeacherStatsPage from '@/pages/TeacherStatsPage.vue'
import AdminStatsPage from '@/pages/AdminStatsPage.vue'
import AdminHomePage from '@/pages/AdminHomePage.vue'
import AdminUsersPage from '@/pages/AdminUsersPage.vue'
import AdminCoursesPage from '@/pages/AdminCoursesPage.vue'
import AdminCategoriesPage from '@/pages/AdminCategoriesPage.vue'
import AdminLivesPage from '@/pages/AdminLivesPage.vue'
import AdminBarragesPage from '@/pages/AdminBarragesPage.vue'
import AdminBannersPage from '@/pages/AdminBannersPage.vue'
import AnnouncementListPage from '@/pages/AnnouncementListPage.vue'
import AnnouncementDetailPage from '@/pages/AnnouncementDetailPage.vue'
import AdminAnnouncementsPage from '@/pages/AdminAnnouncementsPage.vue'
import MessagesPage from '@/pages/MessagesPage.vue'
import ProfilePage from '@/pages/ProfilePage.vue'
import { getToken } from '@/lib/api'

const routes = [
  { path: '/', name: 'home', component: HomePage },
  { path: '/login', name: 'login', component: LoginPage },
  { path: '/register', name: 'register', component: RegisterPage },
  { path: '/courses', name: 'courses', component: CourseListPage },
  { path: '/courses/:id', name: 'course-detail', component: CourseDetailPage },
  { path: '/announcements', name: 'announcements', component: AnnouncementListPage },
  { path: '/announcements/:id', name: 'announcement-detail', component: AnnouncementDetailPage },
  { path: '/messages', name: 'messages', component: MessagesPage, meta: { requiresAuth: true } },
  { path: '/profile', name: 'profile', component: ProfilePage, meta: { requiresAuth: true } },
  { path: '/teacher/courses', name: 'teacher-courses', component: TeacherCoursesPage, meta: { requiresAuth: true, requiresRole: 'TEACHER' } },
  { path: '/live', name: 'live-list', component: LiveListPage },
  { path: '/live/:id', name: 'live-room', component: LiveRoomPage },
  { path: '/teacher/live', name: 'teacher-live', component: TeacherLivePage, meta: { requiresAuth: true, requiresRole: 'TEACHER' } },
  { path: '/replays', name: 'replay-list', component: ReplayListPage },
  { path: '/replays/:id', name: 'replay-detail', component: ReplayDetailPage },
  { path: '/teacher/replays', name: 'teacher-replays', component: TeacherReplaysPage, meta: { requiresAuth: true, requiresRole: 'TEACHER' } },
  { path: '/homeworks', name: 'homework-list', component: HomeworkListPage, meta: { requiresAuth: true } },
  { path: '/homeworks/:id', name: 'homework-detail', component: HomeworkDetailPage, meta: { requiresAuth: true } },
  { path: '/teacher/homeworks', name: 'teacher-homeworks', component: TeacherHomeworksPage, meta: { requiresAuth: true, requiresRole: 'TEACHER' } },
  { path: '/teacher/homeworks/:id/submissions', name: 'teacher-homework-submissions', component: TeacherHomeworkSubmissionsPage, meta: { requiresAuth: true, requiresRole: 'TEACHER' } },
  { path: '/exams', name: 'exam-list', component: ExamListPage, meta: { requiresAuth: true } },
  { path: '/exams/:id', name: 'exam-detail', component: ExamDetailPage, meta: { requiresAuth: true } },
  { path: '/exams/:id/result', name: 'exam-result', component: ExamResultPage, meta: { requiresAuth: true } },
  { path: '/teacher/exams', name: 'teacher-exams', component: TeacherExamsPage, meta: { requiresAuth: true, requiresRole: 'TEACHER' } },
  { path: '/teacher/exams/:id/questions', name: 'teacher-exam-questions', component: TeacherExamQuestionsPage, meta: { requiresAuth: true, requiresRole: 'TEACHER' } },
  { path: '/teacher/exams/:id/submissions', name: 'teacher-exam-submissions', component: TeacherExamSubmissionsPage, meta: { requiresAuth: true, requiresRole: 'TEACHER' } },
  { path: '/dashboard/student-stats', name: 'student-stats', component: StudentStatsPage, meta: { requiresAuth: true } },
  { path: '/dashboard/my-learning', name: 'student-learning', component: StudentLearningPage, meta: { requiresAuth: true } },
  { path: '/dashboard/my-favorites', name: 'student-favorites', component: StudentFavoritesPage, meta: { requiresAuth: true } },
  { path: '/dashboard/teacher-stats', name: 'teacher-stats', component: TeacherStatsPage, meta: { requiresAuth: true, requiresRole: 'TEACHER' } },
  { path: '/admin', name: 'admin-home', component: AdminHomePage, meta: { requiresAuth: true, requiresRole: 'ADMIN' } },
  { path: '/admin/users', name: 'admin-users', component: AdminUsersPage, meta: { requiresAuth: true, requiresRole: 'ADMIN' } },
  { path: '/admin/courses', name: 'admin-courses', component: AdminCoursesPage, meta: { requiresAuth: true, requiresRole: 'ADMIN' } },
  { path: '/admin/categories', name: 'admin-categories', component: AdminCategoriesPage, meta: { requiresAuth: true, requiresRole: 'ADMIN' } },
  { path: '/admin/lives', name: 'admin-lives', component: AdminLivesPage, meta: { requiresAuth: true, requiresRole: 'ADMIN' } },
  { path: '/admin/barrages', name: 'admin-barrages', component: AdminBarragesPage, meta: { requiresAuth: true, requiresRole: 'ADMIN' } },
  { path: '/admin/banners', name: 'admin-banners', component: AdminBannersPage, meta: { requiresAuth: true, requiresRole: 'ADMIN' } },
  { path: '/admin/announcements', name: 'admin-announcements', component: AdminAnnouncementsPage, meta: { requiresAuth: true, requiresRole: 'ADMIN' } },
  { path: '/admin/stats', name: 'admin-stats', component: AdminStatsPage, meta: { requiresAuth: true, requiresRole: 'ADMIN' } },
  { path: '/dashboard', name: 'dashboard', component: DashboardPage, meta: { requiresAuth: true } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

function getStoredRole(): string | null {
  try {
    const raw = localStorage.getItem('edu_user')
    if (raw) return JSON.parse(raw).role ?? null
  } catch {
    // ignore
  }
  return null
}

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !getToken()) {
    return '/login'
  }
  const role = to.meta.requiresRole as string | undefined
  if (role) {
    const storedRole = getStoredRole()
    if (storedRole && storedRole !== role) {
      return '/dashboard'
    }
  }
})

export default router
