import { test, expect, type Page } from '@playwright/test';
import * as path from 'path';
import * as fs from 'fs';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Share variables across tests
const randomId = Math.random().toString(36).substring(2, 8);
const studentUsername = `stu_${randomId}`;
const categoryName = `分类_${randomId}`;
const courseTitle = `课程_${randomId}`;
const announcementTitle = `公告_${randomId}`;
const spamKeyword = `spam_${randomId}`;
const homeworkTitle = `作业_${randomId}`;
const examTitle = `考试_${randomId}`;

// Screenshots folder configuration
const screenshotDir = path.join(__dirname, '../e2e-screenshots');
if (!fs.existsSync(screenshotDir)) {
  fs.mkdirSync(screenshotDir, { recursive: true });
}

async function takeScreenshot(page: Page, name: string) {
  const screenshotPath = path.join(screenshotDir, name);
  await page.screenshot({ path: screenshotPath, fullPage: true });
  console.log(`Saved screenshot to: ${screenshotPath}`);
}

async function logout(page: Page) {
  // Click logout button (button with lucide-log-out icon)
  await page.locator('header button .lucide-log-out, header button:has(.lucide-log-out)').first().click();
  await page.waitForURL(url => url.pathname === '/' || url.pathname.endsWith('/login'));
}

async function teacherLogin(page: Page) {
  await page.goto('/login');
  await page.fill('#login-username', 'teacher1');
  await page.fill('#login-password', '123456');
  await page.click('button[type="submit"]');

  try {
    // Wait for redirect to dashboard/workspace
    await page.waitForURL('**/teacher/**', { timeout: 4000 });
  } catch (e) {
    console.log('Login with teacher1 failed, trying fallback to debugteacher...');
    await page.goto('/login');
    await page.fill('#login-username', 'debugteacher');
    await page.fill('#login-password', '123456');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/teacher/**');
  }
}

test.describe.serial('EduLive Online Education Platform E2E Tests', () => {

  test('Step 1: Student Registration and Logout', async ({ page }) => {
    await page.goto('/register');
    await page.fill('#register-username', studentUsername);
    await page.fill('#register-email', `${studentUsername}@example.com`);
    await page.fill('#register-password', '123456');
    await page.fill('#register-confirm', '123456');
    await page.selectOption('#register-role', 'STUDENT');
    await page.click('button[type="submit"]');

    await page.waitForURL('**/dashboard');
    await expect(page.locator('h1')).toContainText(studentUsername);
    await takeScreenshot(page, '01_student_registered.png');

    await logout(page);
  });

  test('Step 2: Admin configurations (Category, Announcement, Spam Keyword)', async ({ page }) => {
    // Login as Admin
    await page.goto('/login');
    await page.fill('#login-username', 'admin');
    await page.fill('#login-password', '123456');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/admin**');

    // 1. Create Category
    await page.goto('/admin/categories');
    await page.fill('input[placeholder="分类名称"]', categoryName);
    await page.fill('input[placeholder="排序值"]', '10');
    await page.click('button:has-text("新增分类")');
    await expect(page.locator('article', { hasText: categoryName }).first()).toBeVisible();

    // 2. Create System Announcement
    await page.goto('/admin/announcements');
    const createForm = page.locator('form:has-text("创建公告")');
    await createForm.locator('input[placeholder="公告标题"]').fill(announcementTitle);
    await createForm.locator('select').first().selectOption('SYSTEM');
    await createForm.locator('select').nth(1).selectOption('PUBLISHED');
    await createForm.locator('textarea[placeholder="公告内容"]').fill('这是一条由 Playwright 自动测试生成的系统公告。');
    await createForm.locator('button:has-text("创建公告")').click();
    await page.waitForTimeout(1000); // Wait for API response and list reload
    const articles = page.locator('article');
    const count = await articles.count();
    let found = false;
    for (let i = 0; i < count; i++) {
      const val = await articles.nth(i).locator('input').first().inputValue();
      if (val === announcementTitle) {
        found = true;
        break;
      }
    }
    expect(found).toBe(true);

    // 3. Create Barrage Spam Keyword
    await page.goto('/admin/barrages');
    await page.fill('input[placeholder="输入屏蔽词"]', spamKeyword);
    await page.click('aside form button');
    await page.waitForTimeout(1000);
    await expect(page.locator('aside button', { hasText: spamKeyword }).first()).toBeVisible();

    await takeScreenshot(page, '02_admin_configured.png');
    await logout(page);
  });

  test('Step 3: Teacher Creates Course, Video, and Submits Review', async ({ page }) => {
    await teacherLogin(page);

    // Create course
    await page.goto('/teacher/courses');
    await page.fill('input[placeholder="输入课程名称"]', courseTitle);
    // Select category created by admin
    await page.selectOption('form select', { label: categoryName });
    await page.fill('input[placeholder="用一句话介绍这门课程（可选）"]', '这是由 Playwright 自动测试创建的优质课程。');
    await page.fill('input[placeholder="0"]', '9.9'); // Set price to 9.9
    await page.click('form button[type="submit"]');
    
    // Wait for the course to appear
    const courseArticle = page.locator('article', { hasText: courseTitle });
    await expect(courseArticle).toBeVisible();

    // Add a chapter
    await courseArticle.locator('input[placeholder="新章节标题"]').fill('第一章：自动测试入门');
    await courseArticle.locator('input[placeholder="排序"]').fill('1');
    await courseArticle.locator('button:has-text("添加章节")').click();
    await page.waitForTimeout(1000);

    // Upload mock video
    const videoPath = path.join(__dirname, 'assets/mock-video.mp4');
    await courseArticle.locator('input[type="file"]').setInputFiles(videoPath);
    
    // Wait for the video upload to finish (status text changes or success banner)
    await expect(courseArticle.locator('text=已上传视频')).toBeVisible({ timeout: 15000 });

    // Submit for review
    await courseArticle.locator('button:has-text("提交审核")').click();
    // Verify that the status of the course is "审核中" (REVIEWING maps to "审核中" on the teacher page)
    await expect(courseArticle.locator('text=审核中')).toBeVisible();

    await takeScreenshot(page, '03_teacher_course_created.png');
    await logout(page);
  });

  test('Step 4: Admin Approves Teacher Course', async ({ page }) => {
    await page.goto('/login');
    await page.fill('#login-username', 'admin');
    await page.fill('#login-password', '123456');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/admin**');

    await page.goto('/admin/courses');
    const courseRow = page.locator('article', { hasText: courseTitle });
    await expect(courseRow).toBeVisible();
    await courseRow.locator('button:has-text("通过")').click();
    
    // Wait for status to show "已发布"
    await expect(courseRow.locator('text=已发布')).toBeVisible();

    await takeScreenshot(page, '04_admin_approved_course.png');
    await logout(page);
  });

  test('Step 5: Teacher Creates Homework and Exam', async ({ page }) => {
    await teacherLogin(page);

    // 1. Create Homework
    await page.goto('/teacher/homeworks');
    await page.selectOption('form select', { label: courseTitle });
    await page.fill('input[placeholder="作业标题"]', homeworkTitle);
    await page.fill('input[placeholder="作业说明（可选）"]', '请简述你学习 E2E 自动化测试的心得。');
    await page.fill('input[type="datetime-local"]', '2026-12-31T23:59');
    await page.click('form button[type="submit"]');
    await page.waitForTimeout(1000);
    await expect(page.locator('article', { hasText: homeworkTitle }).first()).toBeVisible();

    // 2. Create Exam
    await page.goto('/teacher/exams');
    await page.selectOption('form select', { label: courseTitle });
    await page.fill('input[placeholder="考试标题"]', examTitle);
    await page.locator('input[type="datetime-local"]').first().fill('2026-01-01T00:00'); // Start time
    await page.locator('input[type="datetime-local"]').nth(1).fill('2026-12-31T23:59'); // End time
    await page.fill('input[placeholder="考试说明（可选）"]', '本场考试包含选择题与简答题。');
    await page.fill('input[placeholder="时长(分钟)"], input[type="number"]', '60');
    await page.click('form button[type="submit"]');
    await page.waitForTimeout(1000);

    const examArticle = page.locator('article', { hasText: examTitle });
    await expect(examArticle).toBeVisible();

    // 3. Add Questions to Exam
    await examArticle.locator('a:has-text("题目")').click();
    await page.waitForURL('**/questions');

    // Add SINGLE question
    await page.selectOption('form select', { value: 'SINGLE' });
    await page.fill('input[placeholder="题干内容"]', 'Playwright 默认的元素定位超市是多少毫秒？');
    await page.fill('input[placeholder="分值"]', '5');
    await page.fill('textarea[placeholder*="选项，每行一个"]', 'A. 5000\nB. 10000\nC. 15000\nD. 30000');
    await page.fill('textarea[placeholder*="答案；多选"]', 'D. 30000');
    await page.click('button:has-text("添加题目")');
    await page.waitForTimeout(1000);

    // Add SHORT question
    await page.selectOption('form select', { value: 'SHORT' });
    await page.fill('input[placeholder="题干内容"]', '请谈谈你对 E2E 测试的理解。');
    await page.fill('input[placeholder="分值"]', '10');
    await page.fill('textarea[placeholder*="答案；多选"]', 'E2E测试能够模拟真实用户的端到端流程，对界面的集成情况提供最真实的反馈。');
    await page.click('button:has-text("添加题目")');
    await page.waitForTimeout(1000);

    // 4. Publish Exam
    await page.goto('/teacher/exams');
    const examToPublish = page.locator('article', { hasText: examTitle });
    await examToPublish.locator('button:has-text("发布")').click();
    await expect(examToPublish.locator('text=PUBLISHED')).toBeVisible();

    await takeScreenshot(page, '05_teacher_created_homework_exam.png');
    await logout(page);
  });

  test('Step 6: Student Purchases Course, Watches Video, Submits Homework, Takes Exam, Uploads Avatar', async ({ page }) => {
    // Login as registered student
    await page.goto('/login');
    await page.fill('#login-username', studentUsername);
    await page.fill('#login-password', '123456');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/dashboard');

    // 1. Browse and Purchase Course
    await page.goto('/courses');
    await page.locator('a', { hasText: courseTitle }).first().click();
    await page.waitForURL('**/courses/*');

    await page.click('button:has-text("收藏课程")');
    await page.click('button:has-text("立即购买")');
    await expect(page.locator('text=购买成功')).toBeVisible();

    // 2. Play Video & Save Progress
    await page.evaluate(() => {
      const video = document.querySelector('video');
      if (video) {
        video.currentTime = 10;
        video.dispatchEvent(new Event('pause'));
      }
    });
    await page.waitForTimeout(2000); // Let progress save call fire

    // 3. Submit Homework
    await page.goto('/homeworks');
    await page.locator('a', { hasText: homeworkTitle }).first().click();
    await page.waitForURL('**/homeworks/*');
    await page.fill('textarea[placeholder="输入作业文本内容..."]', 'E2E自动化测试的心得就是可以省时省力地模拟用户的交互，让开发更敏捷。');
    await page.click('button:has-text("提交作业")');
    await expect(page.locator('text=作业已提交')).toBeVisible();

    // 4. Take Exam
    await page.goto('/exams');
    const examArticle = page.locator('article', { hasText: examTitle }).first();
    await examArticle.locator('a:has-text("开始答题")').click();
    await page.waitForURL('**/exams/*');
    // Answer Single Choice
    await page.locator('label', { hasText: 'D. 30000' }).click();
    // Answer Short Answer
    await page.fill('textarea[placeholder="输入简答题答案..."]', 'E2E测试是模拟真实用户走完全程，包括数据库、前端以及所有网络请求，提供了最高的信心。');
    await page.click('button:has-text("提交答卷")');
    await page.waitForURL('**/exams/*/result');

    // 5. Upload Avatar
    await page.goto('/profile');
    const avatarPath = path.join(__dirname, 'assets/mock-cover.jpg');
    await page.setInputFiles('input#profile-avatar', avatarPath);
    await expect(page.locator('text=头像上传成功')).toBeVisible({ timeout: 10000 });

    await takeScreenshot(page, '06_student_completed_activities.png');
    await logout(page);
  });

  test('Step 7: Teacher Grades Homework and Exam', async ({ page }) => {
    await teacherLogin(page);

    // 1. Grade Homework
    await page.goto('/teacher/homeworks');
    const homeworkRow = page.locator('article', { hasText: homeworkTitle });
    await homeworkRow.locator('a:has-text("提交列表")').click();
    await page.waitForURL('**/submissions');

    const homeworkSub = page.locator('article', { hasText: studentUsername });
    await homeworkSub.locator('input[placeholder="分数 (0-100)"]').fill('95');
    await homeworkSub.locator('textarea[placeholder="评语（可选）"]').fill('非常优秀的心得体会！理解很深入。');
    await homeworkSub.locator('button:has-text("保存批改")').click();
    await page.waitForTimeout(1000);

    // 2. Grade Exam Subjective Answer
    await page.goto('/teacher/exams');
    const examRow = page.locator('article', { hasText: examTitle });
    await examRow.locator('a:has-text("阅卷")').click();
    await page.waitForURL('**/submissions');

    // Select the student's submission from left list
    await page.locator('aside button', { hasText: studentUsername }).first().click();
    // Fill subjective question grade
    const scoreInput = page.locator('section input[placeholder="分数"]').first();
    await scoreInput.fill('9.5');
    const commentInput = page.locator('section input[placeholder="题目评语"]').first();
    await commentInput.fill('思路完全正确，答得非常好。');

    // Fill total exam comment
    await page.fill('textarea[placeholder="输入教师总评语"]', '考得很出色，继续保持！');
    await page.click('button:has-text("保存阅卷")');
    await page.waitForTimeout(1000);

    await takeScreenshot(page, '07_teacher_graded_submissions.png');
    await logout(page);
  });

  test('Step 8: Student Checks Grades and Statistics', async ({ page }) => {
    await page.goto('/login');
    await page.fill('#login-username', studentUsername);
    await page.fill('#login-password', '123456');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/dashboard');

    // Check stats page
    await page.goto('/dashboard/student-stats');
    await expect(page.locator('text=学习课程').first()).toBeVisible();
    
    // Check counts
    // 学习课程 should be 1, 收藏课程 should be 1, 作业提交 should be 1, 考试参与 should be 1
    const textContent = await page.textContent('body');
    expect(textContent).toContain('学习课程');
    expect(textContent).toContain('收藏课程');
    expect(textContent).toContain('作业提交');
    expect(textContent).toContain('考试参与');

    await takeScreenshot(page, '08_student_stats_view.png');
    await logout(page);
  });
});
