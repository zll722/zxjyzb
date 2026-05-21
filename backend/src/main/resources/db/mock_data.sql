-- ==========================================
-- 在线教育直播平台测试数据种子脚本
-- 执行前将清空现有数据库中的所有表，并重置自增计数器
-- 默认所有用户的登录密码统一为: 123456
-- ==========================================

USE edu_live;

-- 禁用外键检查以防止清空表时报错
SET FOREIGN_KEY_CHECKS = 0;

-- 1. 清空所有 26 张表的数据
TRUNCATE TABLE user;
TRUNCATE TABLE teacher_profile;
TRUNCATE TABLE category;
TRUNCATE TABLE course;
TRUNCATE TABLE chapter;
TRUNCATE TABLE course_favorite;
TRUNCATE TABLE course_order;
TRUNCATE TABLE learning_record;
TRUNCATE TABLE live_room;
TRUNCATE TABLE live_chat;
TRUNCATE TABLE live_barrage;
TRUNCATE TABLE live_poll;
TRUNCATE TABLE live_poll_vote;
TRUNCATE TABLE live_mic_request;
TRUNCATE TABLE live_replay;
TRUNCATE TABLE homework;
TRUNCATE TABLE homework_submission;
TRUNCATE TABLE exam;
TRUNCATE TABLE exam_question;
TRUNCATE TABLE exam_submission;
TRUNCATE TABLE exam_answer;
TRUNCATE TABLE announcement;
TRUNCATE TABLE site_banner;
TRUNCATE TABLE site_message;
TRUNCATE TABLE barrage_keyword;
TRUNCATE TABLE banner;

-- 启用外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- ==========================================
-- 2. 插入用户数据 (密码统一为 123456)
-- 密码哈希: $2a$10$8m7thlvzi/y/DsNPR2o9Ee6OpJJ3nvRz4KiJHbozsyrOIsS89kSm6
-- ==========================================
INSERT INTO user (id, username, password, email, role, status, avatar, created_at, updated_at) VALUES
(1, 'admin', '$2a$10$8m7thlvzi/y/DsNPR2o9Ee6OpJJ3nvRz4KiJHbozsyrOIsS89kSm6', 'admin@edu-live.com', 'ADMIN', 1, '/uploads/avatar/default_admin.jpg', '2026-05-01 08:00:00', '2026-05-01 08:00:00'),
(2, 'teacher1', '$2a$10$8m7thlvzi/y/DsNPR2o9Ee6OpJJ3nvRz4KiJHbozsyrOIsS89kSm6', 'teacher1@edu-live.com', 'TEACHER', 1, '/uploads/avatar/default_teacher1.jpg', '2026-05-01 09:00:00', '2026-05-01 09:00:00'),
(3, 'teacher2', '$2a$10$8m7thlvzi/y/DsNPR2o9Ee6OpJJ3nvRz4KiJHbozsyrOIsS89kSm6', 'teacher2@edu-live.com', 'TEACHER', 1, '/uploads/avatar/default_teacher2.jpg', '2026-05-01 09:10:00', '2026-05-01 09:10:00'),
(4, 'student1', '$2a$10$8m7thlvzi/y/DsNPR2o9Ee6OpJJ3nvRz4KiJHbozsyrOIsS89kSm6', 'student1@edu-live.com', 'STUDENT', 1, '/uploads/avatar/default_student1.jpg', '2026-05-01 10:00:00', '2026-05-01 10:00:00'),
(5, 'student2', '$2a$10$8m7thlvzi/y/DsNPR2o9Ee6OpJJ3nvRz4KiJHbozsyrOIsS89kSm6', 'student2@edu-live.com', 'STUDENT', 1, '/uploads/avatar/default_student2.jpg', '2026-05-01 10:05:00', '2026-05-01 10:05:00'),
(6, 'student3', '$2a$10$8m7thlvzi/y/DsNPR2o9Ee6OpJJ3nvRz4KiJHbozsyrOIsS89kSm6', 'student3@edu-live.com', 'STUDENT', 1, '/uploads/avatar/default_student3.jpg', '2026-05-01 10:10:00', '2026-05-01 10:10:00');

-- ==========================================
-- 3. 插入教师详情数据
-- ==========================================
INSERT INTO teacher_profile (id, user_id, bio, teaching_years, expertise, created_at, updated_at) VALUES
(1, 2, '资深前端开发专家，曾就职于大型互联网企业，拥有10年以上的前端开发与架构经验，擅长组件化开发和性能优化。', 10, 'Vue3, React, Webpack/Vite, 前端工程化', '2026-05-01 09:00:00', '2026-05-01 09:00:00'),
(2, 3, '高级后端架构师，Spring Boot / Spring Cloud 系列开源技术核心贡献者，对微服务设计、高并发与高可用系统架构有深入研究。', 8, 'Java, Spring Boot, Spring Cloud, Redis, MySQL, 分布式系统', '2026-05-01 09:10:00', '2026-05-01 09:10:00');

-- ==========================================
-- 4. 插入课程分类数据
-- ==========================================
INSERT INTO category (id, name, sort, created_at, updated_at) VALUES
(1, '前端开发', 1, '2026-05-01 08:30:00', '2026-05-01 08:30:00'),
(2, '后端开发', 2, '2026-05-01 08:31:00', '2026-05-01 08:31:00'),
(3, '算法与AI', 3, '2026-05-01 08:32:00', '2026-05-01 08:32:00'),
(4, '测试运维', 4, '2026-05-01 08:33:00', '2026-05-01 08:33:00'),
(5, '外语学习', 5, '2026-05-01 08:34:00', '2026-05-01 08:34:00');

-- ==========================================
-- 5. 插入课程数据
-- ==========================================
INSERT INTO course (id, title, cover, intro, category_id, teacher_id, price, status, favorites_count, created_at, updated_at) VALUES
(1, 'Vue3 + TypeScript 核心技术实战', '/uploads/avatar/vue3_course.jpg', '本课程从Vue3基础语法开始，深入剖析Composition API、状态管理Pinia、Vue Router，以及TypeScript在Vue3中的完美结合。包含完整的企业级项目实战，带你快速成长为前端中高级开发人员。', 1, 2, 99.00, 'PUBLISHED', 2, '2026-05-05 10:00:00', '2026-05-05 10:00:00'),
(2, 'Spring Boot 3.x 企业级项目实战开发', '/uploads/avatar/springboot_course.jpg', '系统讲解Spring Boot 3.x 核心原理与企业级主流组件整合。内容涵盖MyBatis-Plus、Spring Security安全框架、Redis缓存机制、消息队列RabbitMQ以及Docker容器化部署，助力学员打通Java后端开发全链路。', 2, 3, 129.00, 'PUBLISHED', 1, '2026-05-06 10:00:00', '2026-05-06 10:00:00'),
(3, '零基础人工智能与大语言模型入门', '/uploads/avatar/ai_course.jpg', '本课程专为零基础学员设计，从 Python 基础语法入手，带你理解机器学习和深度学习的核心概念。重点关注 ChatGPT、GPT-4 等大语言模型的 prompt 调优技术与 Agent 智能体构建，理论与实战并重。', 3, 2, 199.00, 'UNDER_REVIEW', 0, '2026-05-10 11:00:00', '2026-05-10 11:00:00'),
(4, '高效 Docker 与 Kubernetes 容器化部署实战', '/uploads/avatar/devops_course.jpg', '全面掌握现代运维容器化技术。深入剖析 Docker 镜像构建、容器网络与数据卷，并全面讲解 Kubernetes 集群搭建、Pod 管理、Service 与 Ingress 流量控制，以及 CI/CD 持续集成部署流程。', 4, 3, 0.00, 'DRAFT', 0, '2026-05-15 14:00:00', '2026-05-15 14:00:00');

-- ==========================================
-- 6. 插入课时章节数据
-- ==========================================
INSERT INTO chapter (id, course_id, title, video_path, duration, sort, created_at, updated_at) VALUES
(1, 1, 'Vue 3 核心概念与新特性概述', '/uploads/video/vue3-intro.mp4', 600, 1, '2026-05-05 10:30:00', '2026-05-05 10:30:00'),
(2, 1, 'Composition API 深度解析与实战', '/uploads/video/vue3-composition.mp4', 1200, 2, '2026-05-05 10:45:00', '2026-05-05 10:45:00'),
(3, 1, 'TypeScript 基础与在 Vue3 中的强类型集成', '/uploads/video/vue3-ts.mp4', 900, 3, '2026-05-05 11:00:00', '2026-05-05 11:00:00'),
(4, 2, 'Spring Boot 3.x 新特性与快速起步', '/uploads/video/springboot-intro.mp4', 750, 1, '2026-05-06 10:30:00', '2026-05-06 10:30:00'),
(5, 2, 'MyBatis-Plus 自动代码生成与快速开发', '/uploads/video/springboot-mp.mp4', 1100, 2, '2026-05-06 10:45:00', '2026-05-06 10:45:00'),
(6, 2, 'Spring Security + JWT 权限系统设计', '/uploads/video/springboot-security.mp4', 1500, 3, '2026-05-06 11:00:00', '2026-05-06 11:00:00');

-- ==========================================
-- 7. 插入收藏数据
-- ==========================================
INSERT INTO course_favorite (id, user_id, course_id, created_at) VALUES
(1, 4, 1, '2026-05-21 09:00:00'),
(2, 5, 1, '2026-05-21 09:15:00'),
(3, 4, 2, '2026-05-21 09:30:00');

-- ==========================================
-- 8. 插入课程订单数据
-- ==========================================
INSERT INTO course_order (id, order_no, user_id, course_id, amount, status, paid_at, created_at, updated_at) VALUES
(1, 'ORDER202605210001', 4, 1, 99.00, 'PAID', '2026-05-21 10:00:00', '2026-05-21 09:55:00', '2026-05-21 10:00:00'),
(2, 'ORDER202605210002', 5, 1, 99.00, 'PAID', '2026-05-21 10:30:00', '2026-05-21 10:25:00', '2026-05-21 10:30:00'),
(3, 'ORDER202605210003', 4, 2, 129.00, 'PAID', '2026-05-21 11:00:00', '2026-05-21 10:50:00', '2026-05-21 11:00:00'),
(4, 'ORDER202605210004', 5, 2, 129.00, 'PENDING', NULL, '2026-05-21 12:00:00', '2026-05-21 12:00:00');

-- ==========================================
-- 9. 插入学习进度数据
-- ==========================================
INSERT INTO learning_record (id, user_id, course_id, chapter_id, progress, last_watch_time, created_at, updated_at) VALUES
(1, 4, 1, 1, 100, '2026-05-21 10:15:00', '2026-05-21 10:00:00', '2026-05-21 10:15:00'),
(2, 4, 1, 2, 65, '2026-05-21 10:45:00', '2026-05-21 10:30:00', '2026-05-21 10:45:00'),
(3, 5, 1, 1, 80, '2026-05-21 11:20:00', '2026-05-21 11:00:00', '2026-05-21 11:20:00');

-- ==========================================
-- 10. 插入直播间数据
-- ==========================================
INSERT INTO live_room (id, title, cover, intro, teacher_id, status, scheduled_time, start_time, end_time, online_count, created_at, updated_at) VALUES
(1, 'Vue3组件封装最佳实践直播', '/uploads/avatar/vue3_live.jpg', '本期直播我们将手把手带大家封装几个常用的高质量业务组件，并讲解其中的封装逻辑与性能优化要点。欢迎大家准时参与互动提问！', 2, 'LIVE', '2026-05-21 16:00:00', '2026-05-21 15:30:00', NULL, 45, '2026-05-21 15:00:00', '2026-05-21 15:30:00'),
(2, 'Spring Cloud 分布式微服务架构设计与落地策略', '/uploads/avatar/springboot_live.jpg', '介绍主流微服务组件选型，如 Nacos 注册配置中心、Sentinel 限流熔断与 Gateway 网关设计，分享生产环境下的高可用落地技巧。', 3, 'SCHEDULED', '2026-05-22 20:00:00', NULL, NULL, 0, '2026-05-21 15:10:00', '2026-05-21 15:10:00'),
(3, '大模型 Agent 智能体开发实战分享', '/uploads/avatar/ai_live.jpg', '回顾如何使用 LangChain 和 AutoGPT 构建具有规划、记忆和工具调用能力的自定义 Agent。本场直播已顺利结束，回放视频已上传。', 2, 'ENDED', '2026-05-20 14:00:00', '2026-05-20 14:00:00', '2026-05-20 15:30:00', 0, '2026-05-20 12:00:00', '2026-05-20 15:30:00');

-- ==========================================
-- 11. 插入直播间聊天及弹幕数据
-- ==========================================
INSERT INTO live_chat (id, room_id, user_id, message, created_at) VALUES
(1, 1, 4, '老师好，请问这个组件的Props类型如何限制得更严格？', '2026-05-21 15:35:00'),
(2, 1, 2, '非常好的问题，一会儿我们写到代码这里的时候，我会详细讲一下 TypeScript 泛型在 Props 中的高级用法。', '2026-05-21 15:36:10'),
(3, 1, 5, '打卡学习，前排围观！期待今天封装的通用 Table 组件！', '2026-05-21 15:37:00');

INSERT INTO live_barrage (id, room_id, user_id, content, created_at) VALUES
(1, 1, 4, '太清晰了！点赞！', '2026-05-21 15:35:30'),
(2, 1, 5, '66666，这个思路棒！', '2026-05-21 15:35:50');

-- ==========================================
-- 12. 插入直播间投票及投票结果数据
-- ==========================================
INSERT INTO live_poll (id, room_id, question, options, created_at) VALUES
(1, 1, '大家平常在前端开发中，使用哪种状态管理库最多？', '["Pinia", "Vuex", "Component State (本地状态)", "其它"]', '2026-05-21 15:40:00');

INSERT INTO live_poll_vote (id, poll_id, user_id, option_index, created_at) VALUES
(1, 1, 4, 0, '2026-05-21 15:40:30'),
(2, 1, 5, 0, '2026-05-21 15:40:45');

-- ==========================================
-- 13. 插入直播间连麦申请数据
-- ==========================================
INSERT INTO live_mic_request (id, room_id, user_id, status, created_at, updated_at) VALUES
(1, 1, 4, 'APPROVED', '2026-05-21 15:38:00', '2026-05-21 15:38:30'),
(2, 1, 5, 'PENDING', '2026-05-21 15:42:00', '2026-05-21 15:42:00');

-- ==========================================
-- 14. 插入直播录播回放数据
-- ==========================================
INSERT INTO live_replay (id, room_id, teacher_id, title, intro, video_path, file_size, duration, status, created_at, updated_at) VALUES
(1, 3, 2, '大模型 Agent 智能体开发实战分享（录播回放）', '这是 2026-05-20 直播大模型 Agent 开发的录制版，错过直播的同学可以观看此视频进行学习。本期主要演示如何快速使用 LangChain 构建具有复杂工具调用的 Agent 节点。', '/uploads/replay/agent-replay.mp4', 524288000, 5400, 'PUBLISHED', '2026-05-20 16:00:00', '2026-05-20 16:00:00');

-- ==========================================
-- 15. 插入作业数据
-- ==========================================
INSERT INTO homework (id, course_id, teacher_id, title, description, deadline, created_at, updated_at) VALUES
(1, 1, 2, 'Vue3 组件封装练习作业', '请根据本周学习的 Composition API 和组件封装原则，封装一个具有倒计时功能的 Button 组件。需要包含：1. 倒计时长 Props (time)，2. 倒计时结束的 Emit 事件 (finish)，3. 按钮禁用状态管理。提交时请贴出关键代码，并附带设计思路说明。', '2026-06-01 23:59:59', '2026-05-21 11:00:00', '2026-05-21 11:00:00'),
(2, 2, 3, 'Spring Boot 整合 Redis 缓存实战作业', '实现一个基于 Redis 的热点数据缓存功能。要求：1. 设置合理的缓存过期时间，2. 解决缓存击穿/穿透的简单防御，3. 使用 RedisTemplate 或 Spring Cache 注解。请提交您的 Controller 和 Service 实现代码。', '2026-06-05 23:59:59', '2026-05-21 11:30:00', '2026-05-21 11:30:00');

-- ==========================================
-- 16. 插入作业提交与批改数据
-- ==========================================
INSERT INTO homework_submission (id, homework_id, student_id, content, attachment_path, score, comment, submitted_at, reviewed_at) VALUES
(1, 1, 4, '老师，这是我编写的倒计时组件：\n```vue\n<script setup>\nimport { ref, onUnmounted } from \'vue\';\nconst props = defineProps({ time: { type: Number, default: 60 } });\nconst emit = defineEmits([\'finish\']);\nconst count = ref(props.time);\nconst timer = setInterval(() => {\n  if (count.value > 0) count.value--;\n  else { clearInterval(timer); emit(\'finish\'); }\n}, 1000);\nonUnmounted(() => clearInterval(timer));\n</script>\n```\n思路：利用 ref 记录倒计时数值，组件卸载时清除定时器以防止内存泄漏。', NULL, 95.00, '代码写得很规范，特别是注意到了组件卸载时清除定时器（onUnmounted），这个细节非常好，继续加油！', '2026-05-21 13:00:00', '2026-05-21 14:00:00'),
(2, 1, 5, '我的思路是采用 reactive 包裹状态，并在 watchEffect 中监听倒计时变化，当其降为 0 时重置状态并发送事件。但是发现如果多次渲染可能会冲突，因此采用了 setup 自有作用域隔离。', NULL, NULL, NULL, '2026-05-21 13:30:00', NULL);

-- ==========================================
-- 17. 插入考试数据
-- ==========================================
INSERT INTO exam (id, course_id, teacher_id, title, description, start_time, end_time, duration_minutes, total_score, status, created_at, updated_at) VALUES
(1, 1, 2, 'Vue3 + TS 基础水平测试', '本测试旨在检验学员对 Vue3 新特性以及 TypeScript 基本类型集成的掌握情况。考试时间共60分钟，满分100分。请在规定时间内独立完成。', '2026-05-21 00:00:00', '2026-06-30 23:59:59', 60, 100.00, 'PUBLISHED', '2026-05-21 12:00:00', '2026-05-21 12:00:00');

-- ==========================================
-- 18. 插入考试试题数据 (选项及参考答案)
-- ==========================================
INSERT INTO exam_question (id, exam_id, type, content, options, answer, score, sort, created_at, updated_at) VALUES
(1, 1, 'SINGLE', '在 Vue 3 中，使用 Composition API 时，以下哪个 API 可以用来定义一个响应式的对象，且不需要使用 .value 来修改其属性？', '["ref", "reactive", "computed", "readonly"]', 'reactive', 20.00, 1, '2026-05-21 12:05:00', '2026-05-21 12:05:00'),
(2, 1, 'MULTIPLE', '关于 Vue 3 中的 ref 和 reactive，以下说法正确的有？', '["ref 用于定义任意类型（包含基本和引用类型）的响应式数据，reactive 只能定义引用类型的响应式数据", "ref 的值在 JavaScript 逻辑中必须通过 .value 访问（在 template 模板中会自动解包）", "reactive 定义的响应式对象如果被直接结构解构，解构出来的属性会失去响应性", "reactive 可以接收基本数据类型（如 string, number）作为参数并使其具备响应性"]', 'ref 用于定义任意类型（包含基本和引用类型）的响应式数据，reactive 只能定义引用类型的响应式数据,ref 的值在 JavaScript 逻辑中必须通过 .value 访问（在 template 模板中会自动解包）,reactive 定义的响应式对象如果被直接结构解构，解构出来的属性会失去响应性', 30.00, 2, '2026-05-21 12:07:00', '2026-05-21 12:07:00'),
(3, 1, 'JUDGE', '在 Vue 3 中，v-if 和 v-for 作用在同一个元素上时，v-if 的优先级比 v-for 高。', '["正确", "错误"]', '正确', 10.00, 3, '2026-05-21 12:10:00', '2026-05-21 12:10:00'),
(4, 1, 'SHORT', '请简述 Vue 3 的 Composition API 相比于 Vue 2 的 Options API 具有哪些优势？', NULL, '1. 更好的逻辑复用：无需使用 mixin 就能非常优雅地抽离及组合代码逻辑。\n2. 更好的代码组织：能将相同功能的相关代码（数据、方法、计算属性等）聚合在一起，阅读与维护性更强。\n3. 更加优秀的 TypeScript 类型支持：基于纯函数编写，完美兼容强类型提示。', 40.00, 4, '2026-05-21 12:12:00', '2026-05-21 12:12:00');

-- ==========================================
-- 19. 插入考试提交与阅卷数据
-- ==========================================
-- Student 1 (ID 4) 的提交：待阅卷 (因为有简答题 SHORT)
INSERT INTO exam_submission (id, exam_id, student_id, objective_score, subjective_score, total_score, status, comment, submitted_at, reviewed_at) VALUES
(1, 1, 4, 60.00, 0.00, 60.00, 'PENDING_REVIEW', NULL, '2026-05-21 13:00:00', NULL);

-- Student 2 (ID 5) 的提交：已阅卷 (简答题由教师批改了 35 分，共 85 分)
INSERT INTO exam_submission (id, exam_id, student_id, objective_score, subjective_score, total_score, status, comment, submitted_at, reviewed_at) VALUES
(2, 1, 5, 50.00, 35.00, 85.00, 'REVIEWED', '回答得非常深刻，对Composition API的逻辑抽离总结得很到位，继续努力！', '2026-05-21 13:10:00', '2026-05-21 14:10:00');

-- ==========================================
-- 20. 插入考试各题答题记录数据
-- ==========================================
-- Student 1 的答题记录：
INSERT INTO exam_answer (id, submission_id, question_id, answer, correct, score, comment) VALUES
(1, 1, 1, 'reactive', 1, 20.00, NULL), -- 答对 SINGLE
(2, 1, 2, 'ref 用于定义任意类型（包含基本和引用类型）的响应式数据，reactive 只能定义引用类型的响应式数据,ref 的值在 JavaScript 逻辑中必须通过 .value 访问（在 template 模板中会自动解包）,reactive 定义的响应式对象如果被直接结构解构，解构出来的属性会失去响应性', 1, 30.00, NULL), -- 答对 MULTIPLE
(3, 1, 3, '正确', 1, 10.00, NULL), -- 答对 JUDGE
(4, 1, 4, 'Composition API 最大的好处是可以实现更加清晰的逻辑复用，告别了以前 Vue2 里 mixin 的命名冲突和黑盒变量问题，并且能提供非常自然的 TS 智能类型提示。', NULL, 0.00, NULL); -- 待批改 SHORT

-- Student 2 的答题记录：
INSERT INTO exam_answer (id, submission_id, question_id, answer, correct, score, comment) VALUES
(5, 2, 1, 'ref', 0, 0.00, NULL), -- 答错 SINGLE (应为 reactive)
(6, 2, 2, 'ref 用于定义任意类型（包含基本和引用类型）的响应式数据，reactive 只能定义引用类型的响应式数据,ref 的值在 JavaScript 逻辑中必须通过 .value 访问（在 template 模板中会自动解包）,reactive 定义的响应式对象如果被直接结构解构，解构出来的属性会失去响应性', 1, 30.00, NULL), -- 答对 MULTIPLE
(7, 2, 3, '正确', 1, 10.00, NULL), -- 答对 JUDGE
(8, 2, 4, '1. 方便了代码的解耦与组件化复用，逻辑非常凝聚。\n2. 在大型项目和复杂业务下，避免了像 Options API 选项式开发里在 methods、data 块之间反复横跳的问题。\n3. 原生对 TypeScript 的友好支持。', NULL, 35.00, '回答要点十分清晰，概括全面。'); -- 已批改 SHORT (给 35 分)

-- ==========================================
-- 21. 插入公告数据
-- ==========================================
INSERT INTO announcement (id, title, content, type, status, pinned, creator_id, publish_time, created_at, updated_at) VALUES
(1, '关于平台在线直播教学功能升级的通知', '各位同学和老师，平台今天对在线直播教学功能进行了全面升级，包括新增弹幕过滤敏感词、直播投票以及连麦互动优化等，希望能够给大家带来更好的教学体验！', 'SYSTEM', 'PUBLISHED', 1, 1, '2026-05-21 08:00:00', '2026-05-21 08:00:00', '2026-05-21 08:00:00'),
(2, '2026年夏季编程实战夏令营火热报名中', '想要在暑假快速提升自己的代码实战能力吗？快来报名参加我们的2026夏季实战夏令营吧！优秀的学员还有机会获得大厂内推名额和奖学金支持。详情请咨询各班主任老师。', 'ACTIVITY', 'PUBLISHED', 0, 1, '2026-05-21 08:10:00', '2026-05-21 08:10:00', '2026-05-21 08:10:00'),
(3, '关于违规账号处理及弹幕规范公告', '近期发现部分账号在直播间发布与课程无关的非法广告和恶意言论。对此，平台已部署了新版弹幕敏感词自动过滤机制，并会对屡教不改的违规账号采取封禁措施。', 'SYSTEM', 'PUBLISHED', 0, 1, '2026-05-21 08:15:00', '2026-05-21 08:15:00', '2026-05-21 08:15:00'),
(4, '教师端备课规范与录播格式指引草案', '为了保障教学视频的播放质量，请各位教师在上传章节视频和回放时，统一采用 MP4 格式，分辨率不低于 1080P，编码为 H.264，谢谢合作。', 'SYSTEM', 'DRAFT', 0, 1, '2026-05-21 08:20:00', '2026-05-21 08:20:00', '2026-05-21 08:20:00');

-- ==========================================
-- 22. 插入网站横幅数据 (site_banner & banner)
-- ==========================================
INSERT INTO site_banner (id, title, subtitle, image_url, link_url, sort, status, creator_id, created_at, updated_at) VALUES
(1, '前端前沿技术大揭秘', '从零起点掌握 Vue3 & TS 企业级实战', '/uploads/avatar/banner1.jpg', '/course/1', 1, 'ENABLED', 1, '2026-05-21 09:00:00', '2026-05-21 09:00:00'),
(2, 'Java 后端架构师进阶之旅', '打通 Spring Boot 3 & 微服务容器化部署全链路', '/uploads/avatar/banner2.jpg', '/course/2', 2, 'ENABLED', 1, '2026-05-21 09:05:00', '2026-05-21 09:05:00');

INSERT INTO banner (id, image_path, link, sort, status, created_at, updated_at) VALUES
(1, '/uploads/avatar/banner1.jpg', '/course/1', 1, 1, '2026-05-21 09:00:00', '2026-05-21 09:00:00'),
(2, '/uploads/avatar/banner2.jpg', '/course/2', 2, 1, '2026-05-21 09:05:00', '2026-05-21 09:05:00');

-- ==========================================
-- 23. 插入站内信消息数据
-- ==========================================
INSERT INTO site_message (id, sender_id, receiver_id, title, content, read_status, read_at, created_at, updated_at) VALUES
(1, 1, 4, '欢迎注册在线教育直播平台！', '亲爱的同学，欢迎加入我们在线教育直播平台！在这里你可以购买学习海量编程开发精品课程，在直播间与老师零距离弹幕互动、参与投票以及举手连麦。希望你学有所成！', 1, '2026-05-21 08:30:00', '2026-05-21 08:00:00', '2026-05-21 08:30:00'),
(2, 2, 4, '您的 Vue3 课后作业已被批改', '同学你好，你在《Vue3 组件封装练习作业》中提交的作业已经被老师批改完毕，获得了 95.00 分的优异成绩！老师给你写了详细的评语，快去作业详情中查看吧！', 0, NULL, '2026-05-21 14:05:00', '2026-05-21 14:05:00');

-- ==========================================
-- 24. 插入弹幕敏感词数据
-- ==========================================
INSERT INTO barrage_keyword (id, keyword, created_at) VALUES
(1, '垃圾', '2026-05-21 08:00:00'),
(2, '垃圾广告', '2026-05-21 08:00:00'),
(3, '微信群', '2026-05-21 08:00:00'),
(4, '傻子', '2026-05-21 08:00:00'),
(5, '退钱', '2026-05-21 08:00:00'),
(6, '骗子', '2026-05-21 08:00:00');
