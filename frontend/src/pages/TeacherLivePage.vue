<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
  Mic, MicOff, MonitorUp, Play, Plus, Radio,
  Square, Trash2, Users, Video, Vote, MessageCircle, PhoneOff, X,
} from 'lucide-vue-next'
import type { IAgoraRTCRemoteUser, UID } from 'agora-rtc-sdk-ng'
import { getToken, liveApi, type LiveMicRequest, type LivePollDetail, type LiveRoom } from '@/lib/api'
import { useAuth } from '@/composables/useAuth'
import { createLiveSocket, type LiveSocketMessage } from '@/lib/liveSocket'
import { createTeacherSession, leaveTeacherSession, switchTeacherVideoSource, type TeacherAgoraSession } from '@/lib/agora'
const auth = useAuth()
const rooms = ref<LiveRoom[]>([])
const polls = reactive<Record<number, LivePollDetail[]>>({})
const micRequests = reactive<Record<number, LiveMicRequest[]>>({})
const cohostVideos = reactive<Record<string, HTMLElement | null>>({})
const cohostUsers = ref<IAgoraRTCRemoteUser[]>([])
const sockets = new Map<number, WebSocket>()
const form = reactive({ title: '', intro: '' })
const pollForms = reactive<Record<number, { question: string; optionsText: string }>>({})
const toast = ref('')
const toastTimer = ref<ReturnType<typeof setTimeout> | null>(null)
const activeRoomId = ref<number | null>(null)
const activeRoom = ref<LiveRoom | null>(null)
const micEnabled = ref(true)
const videoSource = ref<'camera' | 'screen'>('camera')
const cameraInfo = ref('')
const teacherVideoRef = ref<HTMLElement | null>(null)
const chatMessages = ref<{ username: string; content: string; time: string; isBarrage?: boolean }[]>([])
const chatListRef = ref<HTMLElement | null>(null)
const onlineCount = ref(0)
const pollModalOpen = ref(false)
let teacherSession: TeacherAgoraSession | null = null

function showToast(msg: string) {
  toast.value = msg
  if (toastTimer.value) clearTimeout(toastTimer.value)
  toastTimer.value = setTimeout(() => { toast.value = '' }, 3000)
}

function formatTime(iso?: string) {
  if (!iso) return new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  return new Date(iso).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

async function scrollChatToBottom() {
  await nextTick()
  if (chatListRef.value) chatListRef.value.scrollTop = chatListRef.value.scrollHeight
}

function upsertPoll(roomId: number, poll: LivePollDetail) {
  if (!polls[roomId]) polls[roomId] = []
  const index = polls[roomId].findIndex((item) => item.id === poll.id)
  if (index >= 0) polls[roomId][index] = poll
  else polls[roomId].unshift(poll)
}

function upsertMicRequest(roomId: number, request: LiveMicRequest) {
  if (!micRequests[roomId]) micRequests[roomId] = []
  const index = micRequests[roomId].findIndex((item) => item.id === request.id)
  if (index >= 0) micRequests[roomId][index] = request
  else micRequests[roomId].unshift(request)
}

function cohostKey(uid: UID) { return String(uid) }

function formatCameraInfo(session: TeacherAgoraSession) {
  return session.videoSource === 'screen'
    ? `屏幕共享 · ${session.videoResolution}`
    : `${session.cameraLabel} · ${session.videoResolution}`
}

function connectRoom(roomId: number) {
  if (sockets.has(roomId)) return
  const socket = createLiveSocket(roomId, (payload: LiveSocketMessage) => {
    if (payload.type === 'online' && typeof payload.count === 'number') onlineCount.value = payload.count
    if ((payload.type === 'poll' || payload.type === 'poll_result') && payload.poll) upsertPoll(roomId, payload.poll as LivePollDetail)
    if ((payload.type === 'mic_request' || payload.type === 'mic_response' || payload.type === 'mic_end') && payload.request) upsertMicRequest(roomId, payload.request as LiveMicRequest)
    if (payload.type === 'chat') {
      chatMessages.value.push({
        username: (payload.username as string) || '匿名',
        content: (payload.content as string) || '',
        time: formatTime(payload.createdAt as string),
      })
      chatMessages.value = chatMessages.value.slice(-200)
      void scrollChatToBottom()
    }
    if (payload.type === 'barrage') {
      chatMessages.value.push({
        username: (payload.username as string) || '匿名',
        content: (payload.content as string) || '',
        time: formatTime(payload.createdAt as string),
        isBarrage: true,
      })
      chatMessages.value = chatMessages.value.slice(-200)
      void scrollChatToBottom()
    }
  })
  sockets.set(roomId, socket)
}

function removeCohost(user: IAgoraRTCRemoteUser) {
  cohostUsers.value = cohostUsers.value.filter((item) => item.uid !== user.uid)
  delete cohostVideos[cohostKey(user.uid)]
}

async function subscribeCohost(user: IAgoraRTCRemoteUser, mediaType: 'audio' | 'video') {
  if (!teacherSession) return
  await teacherSession.client.subscribe(user, mediaType)
  if (!cohostUsers.value.some((item) => item.uid === user.uid)) cohostUsers.value.push(user)
  if (mediaType === 'video' && user.videoTrack) {
    await nextTick()
    const container = cohostVideos[cohostKey(user.uid)]
    if (container) user.videoTrack.play(container)
  }
  if (mediaType === 'audio' && user.audioTrack) user.audioTrack.play()
}

async function loadRoomInteractions(roomId: number) {
  polls[roomId] = await liveApi.polls(roomId)
  micRequests[roomId] = await liveApi.micRequests(roomId)
  connectRoom(roomId)
}

async function load() {
  const user = auth.state.user || await auth.refreshUser()
  const teacherId = user?.id
  if (!teacherId) return
  const page = await liveApi.page({ current: 1, size: 50, teacherId })
  rooms.value = page.records.filter((room) => room.teacherId === teacherId)
  rooms.value.forEach((room) => {
    if (!pollForms[room.id]) pollForms[room.id] = { question: '', optionsText: '选项A\n选项B' }
    void loadRoomInteractions(room.id)
  })
}

async function createRoom() {
  if (!form.title.trim()) return
  await liveApi.create({ title: form.title, intro: form.intro })
  form.title = ''; form.intro = ''
  showToast('直播间已创建')
  await load()
}

async function releaseTeacherSession() {
  await leaveTeacherSession(teacherSession)
  teacherSession = null
  activeRoomId.value = null
  activeRoom.value = null
  videoSource.value = 'camera'
  cameraInfo.value = ''
  cohostUsers.value = []
  Object.keys(cohostVideos).forEach((key) => delete cohostVideos[key])
}

// Auto-end live if teacher closes/refreshes the tab
function endLiveKeepAlive(roomId: number) {
  const token = getToken()
  if (!token) return
  fetch(`/api/live/rooms/${roomId}/end`, {
    method: 'POST',
    keepalive: true,
    headers: { Authorization: `Bearer ${token}` },
  }).catch(() => undefined)
}

function handleBeforeUnload() {
  if (activeRoomId.value) endLiveKeepAlive(activeRoomId.value)
}

async function startRoom(room: LiveRoom) {
  try {
    await releaseTeacherSession()
    chatMessages.value = []
    onlineCount.value = 0
    const latest = await liveApi.start(room.id)
    const liveRoom = await liveApi.detail(latest.id)
    activeRoomId.value = liveRoom.id
    activeRoom.value = liveRoom
    onlineCount.value = liveRoom.onlineCount
    micEnabled.value = true
    videoSource.value = 'camera'
    // Clear historical polls so only current-session polls show
    polls[liveRoom.id] = []
    // Ensure poll form exists for this room
    if (!pollForms[liveRoom.id]) pollForms[liveRoom.id] = { question: '', optionsText: '选项A\n选项B' }
    await nextTick()
    if (!teacherVideoRef.value) throw new Error('本地预览容器未准备好')
    teacherSession = await createTeacherSession(liveRoom, teacherVideoRef.value, subscribeCohost, removeCohost)
    cameraInfo.value = formatCameraInfo(teacherSession)
    showToast('直播已开始，音视频已发布')
  } catch (err) {
    await releaseTeacherSession()
    showToast(err instanceof Error ? err.message : '开播失败')
    await load()
  }
}

async function endRoom(id: number) {
  try {
    if (activeRoomId.value === id) await releaseTeacherSession()
    await liveApi.end(id)
    showToast('直播已结束')
  } catch (err) {
    showToast(err instanceof Error ? err.message : '结束直播失败')
  }
  await load()
}

async function deleteRoom(id: number) {
  if (!confirm('确认删除该直播间？相关聊天记录、弹幕及投票数据将一并删除。')) return
  try {
    await liveApi.deleteRoom(id)
    showToast('直播间已删除')
    await load()
  } catch (err) {
    showToast(err instanceof Error ? err.message : '删除失败')
  }
}

async function toggleMic() {
  if (!teacherSession) return
  micEnabled.value = !micEnabled.value
  await teacherSession.audioTrack.setEnabled(micEnabled.value)
}

async function switchVideo(source: 'camera' | 'screen') {
  if (!teacherSession || !activeRoomId.value || !teacherVideoRef.value) return
  try {
    await switchTeacherVideoSource(teacherSession, source, teacherVideoRef.value)
    videoSource.value = source
    cameraInfo.value = formatCameraInfo(teacherSession)
    showToast(source === 'screen' ? '屏幕共享已发布' : '已恢复摄像头推流')
  } catch (err) {
    showToast(err instanceof Error ? err.message : '切换视频源失败')
  }
}

async function refreshPoll(pollId: number) {
  if (!activeRoomId.value) return
  try {
    const updated = await liveApi.pollResults(pollId)
    upsertPoll(activeRoomId.value, updated)
  } catch { /* ignore */ }
}

async function dismissPoll(roomId: number, pollId: number) {
  try {
    await liveApi.dismissPoll(pollId)
  } catch { /* ignore — still remove locally */ }
  if (polls[roomId]) polls[roomId] = polls[roomId].filter((p) => p.id !== pollId)
}

async function createPoll(roomId: number) {
  const item = pollForms[roomId]
  if (!item?.question.trim()) return
  try {
    const newPoll = await liveApi.createPoll(roomId, {
      question: item.question,
      options: item.optionsText.split('\n').filter(Boolean),
    })
    item.question = ''
    item.optionsText = '选项A\n选项B'
    showToast('投票已推送给学生')
    // Fetch full detail; use upsertPoll so the WS event that may have already
    // arrived first doesn't produce a duplicate entry.
    const detail = await liveApi.pollResults(newPoll.id)
    upsertPoll(roomId, detail)
  } catch (err) {
    showToast(err instanceof Error ? err.message : '发起投票失败')
  }
}

async function handleMic(roomId: number, requestId: number, approved: boolean) {
  const request = await liveApi.handleMic(requestId, approved)
  upsertMicRequest(roomId, request)
}

async function endMic(roomId: number, requestId: number) {
  const request = await liveApi.endMic(requestId)
  upsertMicRequest(roomId, request)
}

onMounted(() => {
  load()
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
  void releaseTeacherSession()
  sockets.forEach((socket) => socket.close())
})
</script>

<template>
  <div>
    <!-- Toast notification — z-[60] so it sits above the fixed live studio (z-50) -->
    <Transition name="toast">
      <div
        v-if="toast"
        class="fixed right-6 top-6 z-[60] rounded-xl bg-slate-900 px-5 py-3 text-sm font-medium text-white shadow-xl"
      >
        {{ toast }}
      </div>
    </Transition>

    <!-- ====== LIVE STUDIO (when broadcasting) — fixed overlay so it covers the nav ====== -->
    <div v-if="activeRoomId && activeRoom" class="fixed inset-0 z-50 flex flex-col bg-slate-950 text-white">
      <!-- Studio header -->
      <header class="flex items-center justify-between border-b border-white/10 bg-slate-900 px-5 py-3">
        <div class="flex items-center gap-3">
          <span class="inline-flex items-center gap-1.5 rounded-full bg-red-500/20 px-3 py-1 text-xs font-semibold text-red-400">
            <span class="h-1.5 w-1.5 animate-pulse rounded-full bg-red-400" />直播中
          </span>
          <h1 class="text-sm font-semibold text-white">{{ activeRoom.title }}</h1>
          <span class="flex items-center gap-1 text-xs text-slate-400">
            <Users class="h-3.5 w-3.5" />{{ onlineCount }} 人在线
          </span>
        </div>
        <div class="flex items-center gap-2">
          <!-- Mic toggle -->
          <button
            class="inline-flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-medium transition"
            :class="micEnabled
              ? 'bg-white/10 text-white hover:bg-white/20'
              : 'bg-red-500/20 text-red-400 hover:bg-red-500/30'"
            @click="toggleMic"
          >
            <component :is="micEnabled ? Mic : MicOff" class="h-3.5 w-3.5" />
            {{ micEnabled ? '麦克风开' : '麦克风关' }}
          </button>
          <!-- Video source toggle -->
          <button
            class="inline-flex items-center gap-1.5 rounded-lg bg-white/10 px-3 py-1.5 text-xs font-medium text-white transition hover:bg-white/20"
            @click="switchVideo(videoSource === 'camera' ? 'screen' : 'camera')"
          >
            <component :is="videoSource === 'camera' ? MonitorUp : Video" class="h-3.5 w-3.5" />
            {{ videoSource === 'camera' ? '共享屏幕' : '恢复摄像头' }}
          </button>
          <!-- Poll modal trigger -->
          <button
            class="relative inline-flex items-center gap-1.5 rounded-lg bg-white/10 px-3 py-1.5 text-xs font-medium text-white transition hover:bg-white/20"
            @click="pollModalOpen = true"
          >
            <Vote class="h-3.5 w-3.5 text-indigo-300" />发起投票
            <span
              v-if="polls[activeRoomId]?.length"
              class="absolute -right-1.5 -top-1.5 flex h-4 w-4 items-center justify-center rounded-full bg-indigo-500 text-[10px] font-bold text-white"
            >{{ polls[activeRoomId].length }}</span>
          </button>
          <!-- End live -->
          <button
            class="inline-flex items-center gap-1.5 rounded-lg bg-red-600 px-4 py-1.5 text-xs font-semibold text-white transition hover:bg-red-500"
            @click="endRoom(activeRoomId)"
          >
            <Square class="h-3.5 w-3.5" />结束直播
          </button>
        </div>
      </header>

      <!-- Studio body -->
      <div class="flex flex-1 overflow-hidden">
        <!-- Left: video + cohost + poll -->
        <div class="flex flex-1 flex-col overflow-y-auto p-4 gap-4">
          <!-- Teacher video preview -->
          <div class="teacher-video-wrap overflow-hidden rounded-xl border border-white/10 bg-black">
            <div ref="teacherVideoRef" class="aspect-video w-full" />
            <div class="flex items-center gap-2 bg-slate-900 px-4 py-2 text-xs text-slate-400">
              <component :is="videoSource === 'camera' ? Video : MonitorUp" class="h-3.5 w-3.5 text-indigo-400" />
              {{ videoSource === 'camera' ? '本地摄像头' : '屏幕共享' }}
              <span v-if="cameraInfo" class="ml-1 text-slate-500">· {{ cameraInfo }}</span>
            </div>
          </div>

          <!-- Cohost videos -->
          <div v-if="cohostUsers.length > 0" class="rounded-xl border border-white/10 bg-slate-900 p-4">
            <h3 class="mb-3 text-xs font-semibold uppercase tracking-wide text-slate-400">学生连麦画面</h3>
            <div class="grid gap-3 sm:grid-cols-2">
              <div v-for="user in cohostUsers" :key="cohostKey(user.uid)" class="overflow-hidden rounded-lg border border-white/10 bg-black">
                <div :ref="(el) => { cohostVideos[cohostKey(user.uid)] = el as HTMLElement | null }" class="h-40 w-full" />
                <div class="flex items-center justify-between bg-slate-900 px-3 py-1.5 text-xs text-slate-400">
                  <span>UID {{ user.uid }}</span>
                </div>
              </div>
            </div>
          </div>

        </div>

        <!-- Right: chat + mic requests -->
        <aside class="flex w-80 flex-col border-l border-white/10 bg-slate-900 xl:w-96">
          <!-- Mic requests -->
          <div v-if="micRequests[activeRoomId]?.some(r => r.status === 'PENDING' || r.status === 'APPROVED')" class="border-b border-white/10 p-4">
            <h3 class="mb-3 flex items-center gap-2 text-xs font-semibold uppercase tracking-wide text-slate-400">
              <PhoneOff class="h-3.5 w-3.5" />连麦申请
            </h3>
            <div class="space-y-2">
              <div
                v-for="request in micRequests[activeRoomId]?.filter(r => r.status === 'PENDING' || r.status === 'APPROVED')"
                :key="request.id"
                class="flex items-center justify-between gap-2 rounded-lg bg-slate-800 px-3 py-2 text-sm"
              >
                <span class="text-white">{{ request.username || `用户 ${request.userId}` }}</span>
                <div v-if="request.status === 'PENDING'" class="flex gap-1.5">
                  <button class="rounded-md bg-emerald-600 px-2 py-1 text-xs font-semibold text-white hover:bg-emerald-500" @click="handleMic(activeRoomId, request.id, true)">同意</button>
                  <button class="rounded-md border border-white/10 px-2 py-1 text-xs text-slate-300 hover:bg-white/10" @click="handleMic(activeRoomId, request.id, false)">拒绝</button>
                </div>
                <button v-else-if="request.status === 'APPROVED'" class="rounded-md border border-amber-400/30 px-2 py-1 text-xs text-amber-300 hover:bg-amber-400/10" @click="endMic(activeRoomId, request.id)">结束连麦</button>
              </div>
            </div>
          </div>

          <!-- Chat panel -->
          <div class="flex flex-1 flex-col overflow-hidden p-4">
            <div class="mb-3 flex items-center gap-2">
              <MessageCircle class="h-4 w-4 text-indigo-400" />
              <h3 class="text-sm font-semibold text-white">学生消息</h3>
              <span class="ml-auto text-xs text-slate-500">仅查看</span>
            </div>
            <div ref="chatListRef" class="flex-1 space-y-2 overflow-y-auto rounded-xl bg-slate-950/60 p-3 scroll-smooth">
              <div v-if="chatMessages.length === 0" class="flex h-full items-center justify-center text-xs text-slate-600">
                暂无学生消息
              </div>
              <div
                v-for="(msg, index) in chatMessages"
                :key="index"
                class="rounded-lg px-3 py-2 text-xs"
                :class="msg.isBarrage ? 'bg-indigo-500/10 border border-indigo-400/20' : 'bg-white/5'"
              >
                <div class="flex items-baseline gap-2">
                  <span class="font-semibold" :class="msg.isBarrage ? 'text-indigo-300' : 'text-slate-300'">{{ msg.username }}</span>
                  <span class="text-slate-600">{{ msg.time }}</span>
                  <span v-if="msg.isBarrage" class="ml-auto rounded bg-indigo-500/20 px-1 py-0.5 text-[10px] text-indigo-300">弹幕</span>
                </div>
                <p class="mt-1 break-all text-slate-200">{{ msg.content }}</p>
              </div>
            </div>
          </div>
        </aside>
      </div>
    </div>

    <!-- ====== ROOM MANAGEMENT (not live) ====== -->
    <div v-else class="mx-auto max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
      <div class="mb-8">
        <p class="text-sm font-medium text-neutral-400">我的直播间</p>
        <h1 class="mt-1 text-2xl font-semibold text-neutral-900">直播管理中心</h1>
      </div>

      <!-- Create form -->
      <div class="mb-8 rounded-xl border border-border bg-white p-6 shadow-sm">
        <h2 class="mb-4 flex items-center gap-2 text-base font-semibold text-neutral-900">
          <Plus class="h-4 w-4 text-primary-600" />新建直播间
        </h2>
        <form class="grid gap-3 sm:grid-cols-[1fr_1fr_auto]" @submit.prevent="createRoom">
          <input
            v-model="form.title"
            required
            placeholder="直播标题 *"
            class="h-10 rounded-md border border-border bg-white px-3 text-sm outline-none transition-all duration-200 focus:border-primary-600 focus:ring-2 focus:ring-primary-600/15"
          />
          <input
            v-model="form.intro"
            placeholder="直播简介（可选）"
            class="h-10 rounded-md border border-border bg-white px-3 text-sm outline-none transition-all duration-200 focus:border-primary-600 focus:ring-2 focus:ring-primary-600/15"
          />
          <button
            type="submit"
            class="inline-flex h-10 items-center gap-1.5 rounded-md bg-primary-600 px-5 text-sm font-semibold text-white shadow-sm transition-all duration-200 hover:bg-primary-500"
          >
            <Plus class="h-4 w-4" />创建
          </button>
        </form>
      </div>

      <!-- Room list -->
      <div v-if="rooms.length > 0" class="space-y-4">
        <article
          v-for="room in rooms"
          :key="room.id"
          class="overflow-hidden rounded-xl border border-border bg-white shadow-sm"
        >
          <!-- Room header -->
          <div class="flex flex-wrap items-center justify-between gap-3 p-5">
            <div class="flex items-center gap-3">
              <div class="grid h-10 w-10 shrink-0 place-items-center rounded-lg bg-primary-50">
                <Radio class="h-5 w-5 text-primary-600" />
              </div>
              <div>
                <div class="flex items-center gap-2">
                  <h2 class="font-semibold text-neutral-900">{{ room.title }}</h2>
                  <span
                    class="rounded-full px-2 py-0.5 text-xs font-medium"
                    :class="{
                      'bg-success-bg text-success':  room.status === 'LIVE',
                      'bg-warning-bg text-warning':  room.status === 'SCHEDULED',
                      'bg-neutral-100 text-neutral-500': room.status === 'ENDED',
                    }"
                  >
                    <span v-if="room.status === 'LIVE'" class="mr-1 inline-block live-dot" />
                    {{ room.status === 'LIVE' ? '直播中' : room.status === 'SCHEDULED' ? '待开播' : '已结束' }}
                  </span>
                </div>
                <p class="mt-0.5 text-sm text-neutral-500">{{ room.intro || '暂无简介' }}</p>
              </div>
            </div>

            <div class="flex flex-wrap gap-2">
              <button
                v-if="room.status === 'SCHEDULED'"
                class="inline-flex min-h-[40px] items-center gap-1.5 rounded-md bg-primary-600 px-4 text-sm font-semibold text-white shadow-sm transition-all duration-200 hover:bg-primary-500"
                @click="startRoom(room)"
              >
                <Play class="h-4 w-4" />开始直播
              </button>
              <template v-else-if="room.status === 'LIVE'">
                <button
                  class="inline-flex min-h-[40px] items-center gap-1.5 rounded-md bg-primary-600 px-4 text-sm font-semibold text-white shadow-sm transition-all duration-200 hover:bg-primary-500"
                  @click="startRoom(room)"
                >
                  <Play class="h-4 w-4" />进入直播间
                </button>
                <button
                  class="inline-flex min-h-[40px] items-center gap-1.5 rounded-md border border-danger/30 px-4 text-sm font-medium text-danger transition-all duration-200 hover:bg-danger-bg"
                  @click="endRoom(room.id)"
                >
                  <Square class="h-4 w-4" />结束直播
                </button>
              </template>
              <button
                v-else
                class="inline-flex min-h-[40px] items-center gap-1.5 rounded-md border border-danger/30 px-4 text-sm font-medium text-danger transition-all duration-200 hover:bg-danger-bg"
                @click="deleteRoom(room.id)"
              >
                <Trash2 class="h-4 w-4" />删除
              </button>
            </div>
          </div>

          <!-- Stats row -->
          <div class="flex gap-4 border-t border-border bg-neutral-50 px-5 py-3 text-xs text-neutral-400">
            <span class="flex items-center gap-1"><Users class="h-3.5 w-3.5" />{{ room.onlineCount }} 在线</span>
            <span>频道：{{ room.agoraChannel || `live_room_${room.id}` }}</span>
          </div>
        </article>
      </div>

      <div v-else class="rounded-xl border border-border bg-white p-10 text-center text-sm text-neutral-400">
        还没有直播间，使用上方表单创建第一个直播间
      </div>
    </div>

    <!-- ====== POLL MODAL ====== -->
    <Teleport to="body">
      <Transition name="modal">
        <div
          v-if="pollModalOpen && activeRoomId"
          class="fixed inset-0 z-[70] flex items-center justify-center p-4"
          @keydown.esc="pollModalOpen = false"
        >
          <!-- Backdrop -->
          <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="pollModalOpen = false" />

          <!-- Panel -->
          <div class="relative flex w-full max-w-md flex-col rounded-2xl border border-white/10 bg-slate-900 shadow-2xl" style="max-height: 82vh">
            <!-- Modal header -->
            <div class="flex shrink-0 items-center justify-between border-b border-white/10 px-5 py-4">
              <div class="flex items-center gap-2">
                <Vote class="h-4 w-4 text-indigo-400" />
                <h2 class="text-sm font-semibold text-white">投票管理</h2>
                <span v-if="polls[activeRoomId]?.length" class="rounded-full bg-indigo-500/20 px-2 py-0.5 text-xs font-medium text-indigo-300">
                  {{ polls[activeRoomId].length }} 条
                </span>
              </div>
              <button
                class="rounded-lg p-1.5 text-slate-400 transition hover:bg-white/10 hover:text-white"
                @click="pollModalOpen = false"
              >
                <X class="h-4 w-4" />
              </button>
            </div>

            <!-- Scrollable body -->
            <div class="flex-1 overflow-y-auto p-5 space-y-5">
              <!-- Create form -->
              <div>
                <p class="mb-3 text-xs font-semibold uppercase tracking-wide text-slate-400">发起新投票</p>
                <template v-if="pollForms[activeRoomId]">
                  <input
                    v-model="pollForms[activeRoomId].question"
                    placeholder="输入投票题目"
                    class="mb-2 w-full rounded-lg border border-white/10 bg-slate-800 px-3 py-2 text-sm text-white outline-none transition placeholder:text-slate-500 focus:border-indigo-400"
                  />
                  <textarea
                    v-model="pollForms[activeRoomId].optionsText"
                    rows="3"
                    placeholder="每行一个选项"
                    class="mb-3 w-full resize-none rounded-lg border border-white/10 bg-slate-800 px-3 py-2 text-sm text-white outline-none transition placeholder:text-slate-500 focus:border-indigo-400"
                  />
                  <button
                    class="inline-flex items-center gap-1.5 rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-indigo-500 disabled:opacity-40"
                    :disabled="!pollForms[activeRoomId].question.trim()"
                    @click="createPoll(activeRoomId)"
                  >
                    <Vote class="h-4 w-4" />推送投票
                  </button>
                </template>
              </div>

              <!-- Divider + results -->
              <template v-if="polls[activeRoomId]?.length">
                <div class="border-t border-white/10" />
                <div>
                  <p class="mb-3 text-xs font-semibold uppercase tracking-wide text-slate-400">本场投票记录</p>
                  <div class="space-y-3">
                    <div
                      v-for="poll in polls[activeRoomId]"
                      :key="poll.id"
                      class="rounded-xl border border-indigo-400/20 bg-slate-800/60 p-4"
                    >
                      <div class="mb-2 flex items-start justify-between gap-2">
                        <p class="text-sm font-semibold leading-snug text-white">{{ poll.question }}</p>
                        <div class="flex shrink-0 gap-1">
                          <button
                            class="rounded-md border border-white/10 px-2 py-1 text-[11px] text-slate-400 hover:bg-white/10"
                            title="刷新结果"
                            @click="refreshPoll(poll.id)"
                          >刷新</button>
                          <button
                            class="rounded-md border border-white/10 px-2 py-1 text-[11px] text-slate-400 hover:bg-red-500/20 hover:text-red-300"
                            title="移除"
                            @click="dismissPoll(activeRoomId, poll.id)"
                          >×</button>
                        </div>
                      </div>
                      <p class="mb-2 text-xs text-slate-500">共 {{ poll.totalVotes }} 票</p>
                      <div class="space-y-2">
                        <div v-for="result in poll.results" :key="result.optionIndex">
                          <div class="flex justify-between text-xs text-slate-300">
                            <span>{{ result.optionText }}</span>
                            <span class="text-slate-500">{{ result.count }} / {{ poll.totalVotes }}</span>
                          </div>
                          <div class="mt-1 h-1.5 overflow-hidden rounded-full bg-white/10">
                            <div
                              class="h-full rounded-full bg-indigo-500 transition-all duration-500"
                              :style="{ width: `${poll.totalVotes ? ((result.count / poll.totalVotes) * 100).toFixed(0) : 0}%` }"
                            />
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </template>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: all 0.25s ease;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* Suppress the Agora SDK's injected overlay controls (stats/fullscreen buttons).
   mirror:false is set on play(), so the container is no longer rotated, but
   we also hide the overlay entirely for a cleaner studio look. */
.teacher-video-wrap :deep(.agora_video_player > :not(video)) {
  display: none !important;
}

.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}
.modal-enter-active .relative,
.modal-leave-active .relative {
  transition: transform 0.2s ease, opacity 0.2s ease;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
.modal-enter-from .relative,
.modal-leave-to .relative {
  transform: translateY(12px) scale(0.97);
  opacity: 0;
}
</style>
