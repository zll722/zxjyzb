<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { MonitorUp, Mic, MicOff, Play, Plus, Square, Video, Vote } from 'lucide-vue-next'
import type { IAgoraRTCRemoteUser, UID } from 'agora-rtc-sdk-ng'
import { liveApi, type LiveMicRequest, type LivePollDetail, type LiveRoom } from '@/lib/api'
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
const message = ref('')
const activeRoomId = ref<number | null>(null)
const micEnabled = ref(true)
const videoSource = ref<'camera' | 'screen'>('camera')
const cameraMessage = ref('未检测摄像头')
const teacherVideoRefs = reactive<Record<number, HTMLElement | null>>({})
let teacherSession: TeacherAgoraSession | null = null

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

function connectRoom(roomId: number) {
  if (sockets.has(roomId)) return
  const socket = createLiveSocket(roomId, (payload: LiveSocketMessage) => {
    if ((payload.type === 'poll' || payload.type === 'poll_result') && payload.poll) upsertPoll(roomId, payload.poll as LivePollDetail)
    if ((payload.type === 'mic_request' || payload.type === 'mic_response' || payload.type === 'mic_end') && payload.request) upsertMicRequest(roomId, payload.request as LiveMicRequest)
  })
  sockets.set(roomId, socket)
}

function cohostKey(uid: UID) {
  return String(uid)
}

function formatCameraMessage(session: TeacherAgoraSession) {
  return session.videoSource === 'screen'
    ? `当前视频源：屏幕共享 · ${session.videoResolution} · ${session.videoTrackState}`
    : `摄像头：${session.cameraLabel} · ${session.videoResolution} · ${session.videoTrackState}`
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
  await liveApi.create({ title: form.title, intro: form.intro })
  form.title = ''
  form.intro = ''
  message.value = '直播间已创建'
  await load()
}

async function releaseTeacherSession() {
  await leaveTeacherSession(teacherSession)
  teacherSession = null
  activeRoomId.value = null
  videoSource.value = 'camera'
  cameraMessage.value = '未检测摄像头'
  cohostUsers.value = []
  Object.keys(cohostVideos).forEach((key) => delete cohostVideos[key])
  Object.keys(teacherVideoRefs).forEach((key) => delete teacherVideoRefs[Number(key)])
}

async function startRoom(room: LiveRoom) {
  try {
    await releaseTeacherSession()
    const latest = await liveApi.start(room.id)
    const liveRoom = await liveApi.detail(latest.id)
    activeRoomId.value = liveRoom.id
    micEnabled.value = true
    videoSource.value = 'camera'
    await nextTick()
    const videoContainer = teacherVideoRefs[liveRoom.id]
    if (!videoContainer) throw new Error('本地预览容器未准备好')
    teacherSession = await createTeacherSession(liveRoom, videoContainer, subscribeCohost, removeCohost)
    cameraMessage.value = formatCameraMessage(teacherSession)
    message.value = '直播已开始，音视频已发布'
  } catch (error) {
    await releaseTeacherSession()
    message.value = error instanceof Error ? error.message : '开播失败'
    await load()
  }
}

async function endRoom(id: number) {
  if (activeRoomId.value === id) await releaseTeacherSession()
  await liveApi.end(id)
  message.value = '直播已结束'
  await load()
}

async function toggleMic() {
  if (!teacherSession) return
  micEnabled.value = !micEnabled.value
  await teacherSession.audioTrack.setEnabled(micEnabled.value)
}

async function switchVideo(source: 'camera' | 'screen') {
  if (!teacherSession || !activeRoomId.value) return
  const videoContainer = teacherVideoRefs[activeRoomId.value]
  if (!videoContainer) return
  try {
    await switchTeacherVideoSource(teacherSession, source, videoContainer)
    videoSource.value = source
    cameraMessage.value = formatCameraMessage(teacherSession)
    message.value = source === 'screen' ? '屏幕共享已发布' : '已恢复摄像头推流'
  } catch (error) {
    message.value = error instanceof Error ? error.message : '切换视频源失败'
  }
}

async function createPoll(id: number) {
  const item = pollForms[id]
  await liveApi.createPoll(id, { question: item.question, options: item.optionsText.split('\n').filter(Boolean) })
  item.question = ''
  message.value = '投票已推送'
  polls[id] = await liveApi.polls(id)
}

async function handleMic(roomId: number, requestId: number, approved: boolean) {
  const request = await liveApi.handleMic(requestId, approved)
  upsertMicRequest(roomId, request)
}

async function endMic(roomId: number, requestId: number) {
  const request = await liveApi.endMic(requestId)
  upsertMicRequest(roomId, request)
}

onMounted(load)
onBeforeUnmount(() => {
  void releaseTeacherSession()
  sockets.forEach((socket) => socket.close())
})
</script>

<template>
  <main class="min-h-screen bg-slate-950 px-6 py-8 text-white">
    <div class="mx-auto max-w-7xl">
      <nav class="mb-8 flex items-center justify-between">
        <RouterLink to="/dashboard" class="text-sm text-cyan-200">返回工作台</RouterLink>
        <RouterLink to="/live" class="rounded-full border border-white/10 px-5 py-2 text-sm hover:bg-white/10">直播中心</RouterLink>
      </nav>
      <section class="rounded-[2rem] border border-white/10 bg-white/8 p-8 backdrop-blur">
        <p class="text-sm text-cyan-200">教师直播管理</p>
        <h1 class="mt-2 text-4xl font-black">创建直播间并管理互动</h1>
        <p v-if="message" class="mt-4 rounded-2xl bg-cyan-300/10 px-4 py-3 text-sm text-cyan-100">{{ message }}</p>
        <form class="mt-8 grid gap-4 md:grid-cols-[1fr_1fr_120px]" @submit.prevent="createRoom">
          <input v-model="form.title" required class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="直播标题" />
          <input v-model="form.intro" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="直播简介" />
          <button class="inline-flex items-center justify-center gap-2 rounded-2xl bg-cyan-300 px-5 py-3 font-bold text-slate-950 hover:bg-cyan-200">
            <Plus class="h-4 w-4" />创建
          </button>
        </form>
      </section>
      <section class="mt-8 space-y-5">
        <article v-for="room in rooms" :key="room.id" class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
          <div class="flex flex-wrap items-start justify-between gap-4">
            <div>
              <p class="text-sm text-cyan-200">{{ room.status }} · {{ room.onlineCount }} 在线</p>
              <h2 class="mt-2 text-2xl font-black">{{ room.title }}</h2>
              <p class="mt-2 text-sm text-slate-300">{{ room.intro || '暂无简介' }}</p>
              <p class="mt-2 text-xs text-slate-400">Agora 频道：{{ room.agoraChannel || `live_room_${room.id}` }}</p>
            </div>
            <div class="flex flex-wrap gap-3">
              <RouterLink :to="`/live/${room.id}`" class="rounded-full border border-white/10 px-5 py-2 text-sm hover:bg-white/10">进入</RouterLink>
              <button class="inline-flex items-center gap-2 rounded-full bg-cyan-300 px-5 py-2 text-sm font-bold text-slate-950" @click="startRoom(room)"><Play class="h-4 w-4" />开始</button>
              <button v-if="activeRoomId === room.id" class="inline-flex items-center gap-2 rounded-full border border-white/10 px-5 py-2 text-sm" @click="toggleMic">
                <component :is="micEnabled ? Mic : MicOff" class="h-4 w-4" />{{ micEnabled ? '关闭麦克风' : '开启麦克风' }}
              </button>
              <button v-if="activeRoomId === room.id" class="inline-flex items-center gap-2 rounded-full border border-white/10 px-5 py-2 text-sm" @click="switchVideo(videoSource === 'camera' ? 'screen' : 'camera')">
                <component :is="videoSource === 'camera' ? MonitorUp : Video" class="h-4 w-4" />{{ videoSource === 'camera' ? '共享屏幕' : '恢复摄像头' }}
              </button>
              <button class="inline-flex items-center gap-2 rounded-full border border-white/10 px-5 py-2 text-sm" @click="endRoom(room.id)"><Square class="h-4 w-4" />结束</button>
            </div>
          </div>
          <div v-if="activeRoomId === room.id" class="mt-6 grid gap-4 lg:grid-cols-[minmax(0,1fr)_320px]">
            <div class="overflow-hidden rounded-[1.5rem] border border-cyan-300/20 bg-black">
              <div :ref="(el) => { teacherVideoRefs[room.id] = el as HTMLElement | null }" class="h-[360px]"></div>
              <div class="flex items-center gap-2 bg-slate-900/80 px-5 py-3 text-sm text-cyan-100">
                <component :is="videoSource === 'camera' ? Video : MonitorUp" class="h-4 w-4" />{{ videoSource === 'camera' ? '本地摄像头预览已发布到 Agora 频道' : '屏幕共享已发布到 Agora 频道' }}
                <span class="text-slate-300">{{ cameraMessage }}</span>
              </div>
            </div>
            <div class="rounded-[1.5rem] border border-white/10 bg-slate-900/70 p-4">
              <h3 class="font-black text-cyan-100">学生连麦画面</h3>
              <div v-if="cohostUsers.length" class="mt-4 space-y-3">
                <div v-for="user in cohostUsers" :key="cohostKey(user.uid)" class="overflow-hidden rounded-2xl border border-white/10 bg-black">
                  <div :ref="(el) => { cohostVideos[cohostKey(user.uid)] = el as HTMLElement | null }" class="min-h-[180px]"></div>
                  <div class="bg-slate-950/90 px-4 py-2 text-xs text-slate-300">UID {{ user.uid }}</div>
                </div>
              </div>
              <p v-else class="mt-4 text-sm text-slate-400">暂无学生连麦画面</p>
            </div>
          </div>
          <div class="mt-6 grid gap-3 md:grid-cols-[1fr_1fr_120px]">
            <input v-model="pollForms[room.id].question" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="投票题目" />
            <textarea v-model="pollForms[room.id].optionsText" class="min-h-12 rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="每行一个选项" />
            <button class="inline-flex items-center justify-center gap-2 rounded-2xl bg-white px-4 py-3 font-bold text-slate-950" @click="createPoll(room.id)"><Vote class="h-4 w-4" />投票</button>
          </div>
          <div class="mt-6 grid gap-4 lg:grid-cols-2">
            <section class="rounded-3xl border border-white/10 bg-slate-900/70 p-5">
              <h3 class="font-black text-cyan-100">投票结果</h3>
              <div v-if="polls[room.id]?.length" class="mt-4 space-y-4">
                <div v-for="poll in polls[room.id]" :key="poll.id" class="rounded-2xl bg-white/5 p-4">
                  <p class="font-bold">{{ poll.question }}</p>
                  <div class="mt-3 space-y-2">
                    <div v-for="result in poll.results" :key="result.optionIndex">
                      <div class="flex justify-between text-sm text-slate-300">
                        <span>{{ result.optionText }}</span>
                        <span>{{ result.count }} / {{ poll.totalVotes }}</span>
                      </div>
                      <div class="mt-1 h-2 overflow-hidden rounded-full bg-white/10">
                        <div class="h-full rounded-full bg-cyan-300" :style="{ width: `${poll.totalVotes ? ((result.count / poll.totalVotes) * 100).toFixed(0) : 0}%` }" />
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <p v-else class="mt-4 text-sm text-slate-400">暂无投票</p>
            </section>
            <section class="rounded-3xl border border-white/10 bg-slate-900/70 p-5">
              <h3 class="font-black text-cyan-100">连麦申请</h3>
              <div v-if="micRequests[room.id]?.length" class="mt-4 space-y-3">
                <div v-for="request in micRequests[room.id]" :key="request.id" class="flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-white/5 p-4">
                  <div>
                    <p class="font-bold">{{ request.username || `用户 ${request.userId}` }}</p>
                    <p class="mt-1 text-sm text-slate-300">{{ request.status }}</p>
                  </div>
                  <div v-if="request.status === 'PENDING'" class="flex gap-2">
                    <button class="rounded-full bg-cyan-300 px-4 py-2 text-sm font-bold text-slate-950" @click="handleMic(room.id, request.id, true)">同意</button>
                    <button class="rounded-full border border-white/10 px-4 py-2 text-sm" @click="handleMic(room.id, request.id, false)">拒绝</button>
                  </div>
                  <button v-else-if="request.status === 'APPROVED'" class="rounded-full border border-amber-200/30 px-4 py-2 text-sm text-amber-100" @click="endMic(room.id, request.id)">结束连麦</button>
                </div>
              </div>
              <p v-else class="mt-4 text-sm text-slate-400">暂无连麦申请</p>
            </section>
          </div>
        </article>
      </section>
    </div>
  </main>
</template>
