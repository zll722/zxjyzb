import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '@/pages/HomePage.vue'
import LoginPage from '@/pages/LoginPage.vue'
import RegisterPage from '@/pages/RegisterPage.vue'
import DashboardPage from '@/pages/DashboardPage.vue'
import CourseListPage from '@/pages/CourseListPage.vue'
import CourseDetailPage from '@/pages/CourseDetailPage.vue'
import LiveListPage from '@/pages/LiveListPage.vue'
import LiveRoomPage from '@/pages/LiveRoomPage.vue'
import ReplayListPage from '@/pages/ReplayListPage.vue'
import ReplayDetailPage from '@/pages/ReplayDetailPage.vue'
import HomeworkListPage from '@/pages/HomeworkListPage.vue'
import HomeworkDetailPage from '@/pages/HomeworkDetailPage.vue'
import ExamListPage from '@/pages/ExamListPage.vue'
import ExamDetailPage from '@/pages/ExamDetailPage.vue'
import ExamResultPage from '@/pages/ExamResultPage.vue'
import StudentStatsPage from '@/pages/StudentStatsPage.vue'
import StudentLearningPage from '@/pages/StudentLearningPage.vue'
import StudentFavoritesPage from '@/pages/StudentFavoritesPage.vue'
import AnnouncementListPage from '@/pages/AnnouncementListPage.vue'
import AnnouncementDetailPage from '@/pages/AnnouncementDetailPage.vue'
import MessagesPage from '@/pages/MessagesPage.vue'
import ProfilePage from '@/pages/ProfilePage.vue'

// Teacher pages
import TeacherLayout from '@/components/layout/TeacherLayout.vue'
import TeacherCoursesPage from '@/pages/TeacherCoursesPage.vue'
import TeacherLivePage from '@/pages/TeacherLivePage.vue'
import TeacherReplaysPage from '@/pages/TeacherReplaysPage.vue'
import TeacherHomeworksPage from '@/pages/TeacherHomeworksPage.vue'
import TeacherHomeworkSubmissionsPage from '@/pages/TeacherHomeworkSubmissionsPage.vue'
import TeacherExamsPage from '@/pages/TeacherExamsPage.vue'
import TeacherExamQuestionsPage from '@/pages/TeacherExamQuestionsPage.vue'
import TeacherExamSubmissionsPage from '@/pages/TeacherExamSubmissionsPage.vue'
import TeacherStatsPage from '@/pages/TeacherStatsPage.vue'

// Admin pages
import AdminLayout from '@/components/layout/AdminLayout.vue'
import AdminHomePage from '@/pages/AdminHomePage.vue'
import AdminUsersPage from '@/pages/AdminUsersPage.vue'
import AdminCoursesPage from '@/pages/AdminCoursesPage.vue'
import AdminCategoriesPage from '@/pages/AdminCategoriesPage.vue'
import AdminLivesPage from '@/pages/AdminLivesPage.vue'
import AdminBarragesPage from '@/pages/AdminBarragesPage.vue'
import AdminBannersPage from '@/pages/AdminBannersPage.vue'
import AdminAnnouncementsPage from '@/pages/AdminAnnouncementsPage.vue'
import AdminStatsPage from '@/pages/AdminStatsPage.vue'

import { getToken } from '@/lib/api'

const routes = [
  // Public routes
  { path: '/', name: 'home', component: HomePage },
  { path: '/login', name: 'login', component: LoginPage },
  { path: '/register', name: 'register', component: RegisterPage },
  { path: '/courses', name: 'courses', component: CourseListPage },
  { path: '/courses/:id', name: 'course-detail', component: CourseDetailPage },
  { path: '/announcements', name: 'announcements', component: AnnouncementListPage },
  { path: '/announcements/:id', name: 'announcement-detail', component: AnnouncementDetailPage },
  { path: '/live', name: 'live-list', component: LiveListPage },
  { path: '/live/:id', name: 'live-room', component: LiveRoomPage },
  { path: '/replays', name: 'replay-list', component: ReplayListPage },
  { path: '/replays/:id', name: 'replay-detail', component: ReplayDetailPage },

  // Auth-required routes (any role)
  { path: '/messages', name: 'messages', component: MessagesPage, meta: { requiresAuth: true } },
  { path: '/profile', name: 'profile', component: ProfilePage, meta: { requiresAuth: true } },
  { path: '/homeworks', name: 'homework-list', component: HomeworkListPage, meta: { requiresAuth: true } },
  { path: '/homeworks/:id', name: 'homework-detail', component: HomeworkDetailPage, meta: { requiresAuth: true } },
  { path: '/exams', name: 'exam-list', component: ExamListPage, meta: { requiresAuth: true } },
  { path: '/exams/:id', name: 'exam-detail', component: ExamDetailPage, meta: { requiresAuth: true } },
  { path: '/exams/:id/result', name: 'exam-result', component: ExamResultPage, meta: { requiresAuth: true } },
  { path: '/dashboard', name: 'dashboard', component: DashboardPage, meta: { requiresAuth: true } },
  { path: '/dashboard/student-stats', name: 'student-stats', component: StudentStatsPage, meta: { requiresAuth: true } },
  { path: '/dashboard/my-learning', name: 'student-learning', component: StudentLearningPage, meta: { requiresAuth: true } },
  { path: '/dashboard/my-favorites', name: 'student-favorites', component: StudentFavoritesPage, meta: { requiresAuth: true } },

  // Teacher workspace (nested layout)
  {
    path: '/teacher',
    component: TeacherLayout,
    meta: { requiresAuth: true, requiresRole: 'TEACHER' },
    children: [
      { path: 'courses', name: 'teacher-courses', component: TeacherCoursesPage },
      { path: 'live', name: 'teacher-live', component: TeacherLivePage },
      { path: 'replays', name: 'teacher-replays', component: TeacherReplaysPage },
      { path: 'homeworks', name: 'teacher-homeworks', component: TeacherHomeworksPage },
      { path: 'homeworks/:id/submissions', name: 'teacher-homework-submissions', component: TeacherHomeworkSubmissionsPage },
      { path: 'exams', name: 'teacher-exams', component: TeacherExamsPage },
      { path: 'exams/:id/questions', name: 'teacher-exam-questions', component: TeacherExamQuestionsPage },
      { path: 'exams/:id/submissions', name: 'teacher-exam-submissions', component: TeacherExamSubmissionsPage },
      { path: 'stats', name: 'teacher-stats', component: TeacherStatsPage },
    ],
  },

  // Admin workspace (nested layout)
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAuth: true, requiresRole: 'ADMIN' },
    children: [
      { path: '', name: 'admin-home', component: AdminHomePage },
      { path: 'users', name: 'admin-users', component: AdminUsersPage },
      { path: 'courses', name: 'admin-courses', component: AdminCoursesPage },
      { path: 'categories', name: 'admin-categories', component: AdminCategoriesPage },
      { path: 'lives', name: 'admin-lives', component: AdminLivesPage },
      { path: 'barrages', name: 'admin-barrages', component: AdminBarragesPage },
      { path: 'banners', name: 'admin-banners', component: AdminBannersPage },
      { path: 'announcements', name: 'admin-announcements', component: AdminAnnouncementsPage },
      { path: 'stats', name: 'admin-stats', component: AdminStatsPage },
    ],
  },

  // 404 fallback
  { path: '/:pathMatch(.*)*', name: 'not-found', redirect: '/' },
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
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  const requiredRole = to.meta.requiresRole as string | undefined
  if (requiredRole) {
    const storedRole = getStoredRole()
    if (!storedRole || storedRole !== requiredRole) {
      return '/dashboard'
    }
  }
})

export default router
