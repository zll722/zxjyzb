# debug-agora-live-start

Status: [OPEN]

## Symptom
教师开始直播时报 AgoraRTCError CAN_NOT_GET_GATEWAY_SERVER / invalid vendor key / can not find appid，需要启动前后端并用浏览器复现验证。

## Hypotheses
1. 后端运行时读取的 agora.app-id 仍然不是声网国内控制台的真实 App ID。
2. 前端请求到的直播间详情里 agoraAppId/agoraToken/agoraChannel 字段为空或错误。
3. 前端构建或开发服务器缓存导致仍使用旧后端返回结果。
4. Agora Web SDK 节点配置与国内声网项目区域不匹配。
5. 登录用户、直播间状态或接口权限导致实际没有进入正确开播流程。
6. 后端 AccessToken2 生成格式与官方格式不一致，导致 SDK 在 join 时无法从 token 正确识别 App ID。

## Evidence
- 后端已启动在 `http://localhost:8080/api`，日志显示 `Tomcat started on port 8080 (http) with context path '/api'`。
- 前端已启动在 `http://127.0.0.1:5173/`。
- 使用测试教师账号 `debugteacher / 123456` 登录成功，并创建直播间 `MCP测试直播`，房间 ID 为 7。
- 浏览器网络请求 `GET /api/live/rooms/7` 返回 `agoraAppId: bb6b9586cc6c42b48f1df1a945f1e40e`、`agoraChannel: live_room_7`、`agoraToken: 007...`。
- 点击“开始”后控制台仍出现 `AgoraRTCError CAN_NOT_GET_GATEWAY_SERVER: invalid vendor key, can not find appid`。
- 控制台显示 SDK 请求 `https://webrtc2-2.ap.sd-rtn.com/api/v2/transpond/webrtc?v=2`，服务端返回 `invalid vendor key, can not find appid`。

## Analysis
- 假设 1 否定：后端确实已读取并下发当前配置中的 App ID，不再是占位文本。
- 假设 2 否定：直播间详情字段存在且格式为 32 位 App ID、频道和 token 均已返回。
- 假设 3 否定：前端通过开发服务实时请求后端，网络响应已证明使用的是新配置。
- 假设 4 部分否定：前端使用 `AREAS.CHINA` 指定中国大陆区域，请求进入 `sd-rtn.com` 国内链路。
- 假设 5 否定：登录、创建直播间、开始直播接口都成功，错误发生在 Agora SDK join 阶段。
- 假设 6 确认：原 Token 生成逻辑没有按官方 AccessToken2 格式压缩签名内容，且权限过期字段使用了绝对时间戳；修正后同一 App ID 可以成功 join。

## Fix
- 修正 `AgoraTokenUtil` 的 AccessToken2 序列化顺序、签名方式、Deflater 压缩和权限过期秒数。
- 修正前端 `setArea` 调用为类型安全的 `AREAS.CHINA`。

## Post-Fix Evidence
- 后端 `mvn -q -DskipTests package` 通过。
- 前端 `npm run check` 通过。
- 重新启动后端 `http://localhost:8080/api` 和前端 `http://127.0.0.1:5174/`。
- 浏览器重新登录测试教师后点击“开始”，控制台出现 `Joining channel success: channel: live_room_7,uid: 522465233`。
- 页面显示 `本地摄像头预览已发布到 Agora 频道`。
- 未再出现 `invalid vendor key, can not find appid`。

## Conclusion
根因不是 App ID 配置错误，而是后端自实现的 Agora AccessToken2 生成格式不符合官方格式。修正 token 生成后，当前声网国内控制台中的 App ID 和 App Certificate 可以正常加入频道并开播。
