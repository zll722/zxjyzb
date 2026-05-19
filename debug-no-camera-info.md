# debug-no-camera-info

Status: [OPEN]

## Symptom
教师开播后页面显示本地摄像头已发布，但预览区域为黑屏，用户反馈无摄像头信息。

## Hypotheses
1. 当前浏览器环境没有可用摄像头输入设备，Agora 创建了空/黑色视频轨道。
2. 浏览器摄像头权限未授权或被自动化环境限制，导致预览无法显示真实画面。
3. 本地预览容器内已有 Agora video 元素，但视频轨道处于 muted/ended 或尺寸为 0。
4. 代码缺少摄像头设备检测和用户可读提示，导致无设备时仍显示“已发布”。
5. 开播逻辑只处理异常，没有处理“成功加入但无实际摄像头画面”的运行状态。

## Evidence
- 浏览器 `enumerateDevices()` 返回一个视频输入设备：`Integrated Camera (04f2:b828)`。
- 页面初始采集时没有 `video` 元素，底部只显示“本地摄像头预览已发布到 Agora 频道”，缺少设备名。
- Agora join 成功：`Joining channel success: channel: live_room_7,uid: 4103512132`。
- Agora 日志显示媒体设备权限检查结果：`check media device permissions false true false true`。
- 修复后等待 10 秒，页面文本出现 `摄像头：Integrated Camera (04f2:b828)`，DOM 中 `video` 数量为 1。

## Analysis
- 假设 1 否定：浏览器能枚举到真实摄像头设备。
- 假设 2 部分否定：当前浏览器能创建视频轨道并生成 video 元素；黑屏更可能来自设备遮挡、系统隐私开关、自动化环境或摄像头输出本身。
- 假设 3 部分确认：刚开播瞬间 video 元素可能尚未挂载，稍后出现；页面原先没有展示设备状态，用户看不到摄像头信息。
- 假设 4 确认：代码只展示“已发布”，没有展示实际摄像头名称或无法识别状态。
- 假设 5 确认：成功加入和发布时没有同步摄像头设备信息到 UI。

## Fix
- `createTeacherSession` 在创建本地音视频轨道前调用 `AgoraRTC.getCameras()`，发布后从 `videoTrack.getMediaStreamTrack().label` 读取真实摄像头名称。
- `TeacherAgoraSession` 增加 `cameraLabel`、`videoTrackState`、`videoResolution`。
- 教师直播页增加 `cameraMessage`，开播成功后显示 `摄像头：xxx · 分辨率 · live`。
- 切换屏幕共享时显示 `当前视频源：屏幕共享 · 分辨率 · live`，恢复摄像头时重新显示摄像头名称、分辨率和轨道状态。

## Post-Fix Evidence
- `frontend/src/lib/agora.ts` 诊断无错误。
- `frontend/src/pages/TeacherLivePage.vue` 诊断无错误。
- `npm run check` 通过。
- 浏览器重新登录并开播后，页面显示 `摄像头：Integrated Camera (04f2:b828)`。
- DOM 中出现 1 个 video 元素。
- 后续补充了分辨率与 MediaStreamTrack `readyState` 展示，类型检查继续通过。

## Conclusion
当前环境有摄像头，原问题是 UI 没有展示摄像头设备信息，用户只能看到黑色预览区域和“已发布”。现在教师端会在开播后展示实际摄像头名称、采集分辨率和轨道状态，用于确认当前采集设备；如果仍黑屏，下一步应检查系统隐私开关、摄像头遮挡或浏览器是否拿到黑帧。
