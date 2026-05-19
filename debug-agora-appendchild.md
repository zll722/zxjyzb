# debug-agora-appendchild

Status: [OPEN]

## Symptom
教师开播后出现 `e10.appendChild is not a function`。

## Hypotheses
1. 传给 Agora `track.play(...)` 的 `videoContainer` 不是原生 HTMLElement，而是 Vue 组件实例或数组。
2. Vue `v-for` 中单个 `ref="teacherVideoRef"` 绑定到多个节点，导致 ref 值形态不稳定。
3. 开播后列表刷新导致本地预览容器被卸载，Agora SDK 在旧引用上调用 `appendChild`。
4. 浏览器页面仍使用旧前端服务缓存或旧端口代码。
5. 错误来自学生连麦远端视频容器，而不是教师本地预览容器。

## Evidence
- 浏览器页面复现到页面消息 `e10.appendChild is not a function`。
- 页面处于教师直播管理页，房间 `MCP测试直播` 状态为 `LIVE`。
- 控制台没有新的 Agora join 失败，错误消息由页面 catch 后展示。
- DOM 快照显示开播失败后 `activeRoomId` 被释放，本地预览容器不存在，只剩主页面和 textarea。
- `TeacherLivePage.vue` 中 `ref="teacherVideoRef"` 位于 `v-for` 内，并在开播成功后立即 `await load()` 刷新房间列表。

## Analysis
- 假设 1 确认：`teacherVideoRef` 在 `v-for` 中会变成非单一 HTMLElement 的引用形态，传给 Agora `videoTrack.play(...)` 后触发 `appendChild is not a function`。
- 假设 2 确认：同一个模板 ref 放在多房间列表里，Vue 会按多 ref 处理，类型标成单个 HTMLElement 不可靠。
- 假设 3 确认：开播成功后立刻 `load()` 会刷新列表，可能卸载 Agora 正在播放的预览节点。
- 假设 4 否定：强制刷新 5174 后仍能复现，说明不是旧缓存。
- 假设 5 否定：当前无学生连麦用户，错误发生在教师本地预览阶段。

## Fix
- 将单个 `teacherVideoRef` 改为按直播间 ID 存储的 `teacherVideoRefs`。
- 模板使用函数 ref：`teacherVideoRefs[room.id] = el as HTMLElement | null`，确保传给 Agora 的是当前房间的真实 DOM。
- 开播成功后不再立即 `load()` 刷新列表，避免卸载 Agora 本地预览容器。
- 切换摄像头/屏幕共享时也从 `teacherVideoRefs[activeRoomId]` 取当前容器。

## Post-Fix Evidence
- `TeacherLivePage.vue` 诊断无错误。
- `npm run check` 通过。
- 重启后端并强制刷新前端后，重新登录教师，点击“开始”。
- 页面显示 `直播已开始，音视频已发布` 和 `本地摄像头预览已发布到 Agora 频道`。
- 控制台只剩一个 Agora 备用 WebSocket 关闭警告，未再出现 `appendChild is not a function`。

## Conclusion
根因是 Vue `v-for` 中复用单个模板 ref，导致传入 Agora SDK 的播放容器不是稳定的 HTMLElement；改为按房间 ID 管理 DOM ref，并避免开播后立即刷新列表后，问题已解决。
