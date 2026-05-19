<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { MessageCircle, PhoneOff, Radio, Send, Users, Vote } from 'lucide-vue-next'
import type { IAgoraRTCRemoteUser } from 'agora-rtc-sdk-ng'
import { formatLiveStatus, getToken, liveApi, type LiveMicRequest, type LivePollDetail, type LiveRoom } from '@/lib/api'
import { createLiveSocket, type LiveSocketMessage } from '@/lib/liveSocket'
import { useAuth } from '@/composables/useAuth'
import { createStudentSession, leaveStudentSession, startStudentCohost, stopStudentCohost, type StudentAgoraSession } from '@/lib/agora'

const route = useRoute()
const auth = useAuth()
const room = ref<LiveRoom | null>(null)
const chatMessages = ref<{ username: string; content: string; time: string }[]>([])
const polls = ref<LivePollDetail[]>([])
const selectedOptions = ref<Record<number, number>>({})
const micRequest = ref<LiveMicRequest | null>(null)
const micMessage = ref('')
const chat = ref('')
const barrage = ref('')
const onlineCount = ref(0)
const remoteVideoRef = ref<HTMLElement | null>(null)
const localCohostVideoRef = ref<HTMLElement | null>(null)
const agoraMessage = ref('等待教师开播')
const cohosting = ref(false)
const chatListRef = ref<HTMLElement | null>(null)

// 弹幕飘动
interface BarrageItem { id: number; content: string; top: number; color: string }
const floatingBarrages = ref<BarrageItem[]>([])
let barrageSeq = 0
const BARRAGE_COLORS = ['#67e8f9', '#a5f3fc', '#99f6e4', '#fde68a', '#c4b5fd', '#fbcfe8', '#ffffff']

function spawnBarrage(content: string) {
  const id = ++barrageSeq
  const top = 5 + Math.random() * 65
  const color = BARRAGE_COLORS[Math.floor(Math.random() * BARRAGE_COLORS.length)]
  floatingBarrages.value.push({ id, content, top, color })
  setTimeout(() => {
    floatingBarrages.value = floatingBarrages.value.filter((b) => b.id !== id)
  }, 8000)
}

let socket: WebSocket | null = null
let studentSession: StudentAgoraSession | null = null

const currentPoll = computed(() => polls.value[0] || null)

function upsertPoll(poll: LivePollDetail) {
  const index = polls.value.findIndex((item) => item.id === poll.id)
  if (index >= 0) polls.value[index] = poll
  else polls.value.unshift(poll)
}

function formatTime(iso?: string) {
  if (!iso) return new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  return new Date(iso).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function typeLabel(type: string) {
  const map: Record<string, string> = { chat: '聊天', barrage: '弹幕', poll: '投票', poll_result: '投票结果', mic_response: '连麦', mic_end: '连麦结束', online: '人数更新' }
  return map[type] ?? type
}

async function load() {
  room.value = await liveApi.detail(Number(route.params.id))
  onlineCount.value = room.value.onlineCount
  polls.value = await liveApi.polls(room.value.id)
  polls.value.forEach((poll) => {
    if (poll.selectedOptionIndex !== null && poll.selectedOptionIndex !== undefined)
      selectedOptions.value[poll.id] = poll.selectedOptionIndex
  })
}

async function sendChat() {
  if (!chat.value.trim() || !room.value || !getToken()) return
  await liveApi.chat(room.value.id, chat.value.trim())
  chat.value = ''
}

async function sendBarrage() {
  if (!barrage.value.trim() || !room.value || !getToken()) return
  await liveApi.barrage(room.value.id, barrage.value.trim())
  barrage.value = ''
}

async function submitVote(poll: LivePollDetail) {
  const optionIndex = selectedOptions.value[poll.id]
  if (optionIndex === undefined || !getToken()) return
  await liveApi.vote(poll.id, optionIndex)
  const latest = await liveApi.pollResults(poll.id)
  upsertPoll(latest)
}

async function requestMic() {
  if (!room.value || !getToken()) return
  micRequest.value = await liveApi.requestMic(room.value.id)
  micMessage.value = '连麦申请已提交，等待教师处理'
}

async function subscribeTeacher(user: IAgoraRTCRemoteUser, mediaType: 'audio' | 'video') {
  if (!studentSession || !remoteVideoRef.value) return
  await studentSession.client.subscribe(user, mediaType)
  if (mediaType === 'video' && user.videoTrack) {
    user.videoTrack.play(remoteVideoRef.value)
    agoraMessage.value = '正在观看教师直播'
  }
  if (mediaType === 'audio' && user.audioTrack) user.audioTrack.play()
}

async function joinAgoraRoom() {
  if (!room.value) return
  try {
    await leaveStudentSession(studentSession)
    studentSession = await createStudentSession(room.value, subscribeTeacher)
    cohosting.value = false
    agoraMessage.value = '已加入频道，等待教师音视频'
  } catch (error) {
    studentSession = null
    cohosting.value = false
    agoraMessage.value = error instanceof Error ? error.message : '加入频道失败'
  }
}

async function beginCohost() {
  if (!studentSession) return
  try {
    await nextTick()
    if (!localCohostVideoRef.value) throw new Error('本地连麦预览容器未准备好')
    await startStudentCohost(studentSession, localCohostVideoRef.value)
    cohosting.value = true
    micMessage.value = '连麦中，已发布本地麦克风和摄像头'
  } catch (error) {
    cohosting.value = false
    micMessage.value = error instanceof Error ? error.message : '连麦发布失败'
  }
}

async function disconnectMic(notifyServer = true) {
  if (notifyServer && micRequest.value?.status === 'APPROVED')
    micRequest.value = await liveApi.endMic(micRequest.value.id)
  await stopStudentCohost(studentSession)
  cohosting.value = false
  if (micRequest.value?.status === 'ENDED') micMessage.value = '连麦已断开'
}

async function handleMicSignal(request: LiveMicRequest) {
  if (request.userId !== auth.state.user?.id) return
  micRequest.value = request
  if (request.status === 'APPROVED') {
    micMessage.value = '教师已同意连麦申请'
    await beginCohost()
  } else if (request.status === 'ENDED') {
    await disconnectMic(false)
    micMessage.value = '连麦已结束'
  } else {
    micMessage.value = '教师已拒绝连麦申请'
  }
}

async function scrollChatToBottom() {
  await nextTick()
  if (chatListRef.value) chatListRef.value.scrollTop = chatListRef.value.scrollHeight
}

onMounted(async () => {
  await auth.refreshUser()
  await load()
  if (!room.value) return
  await joinAgoraRoom()
  socket = createLiveSocket(room.value.id, (message: LiveSocketMessage) => {
    if (message.type === 'online' && typeof message.count === 'number') {
      onlineCount.value = message.count
    }
    if ((message.type === 'poll' || message.type === 'poll_result') && message.poll) {
      upsertPoll(message.poll as LivePollDetail)
    }
    if ((message.type === 'mic_response' || message.type === 'mic_end') && message.request) {
      void handleMicSignal(message.request as LiveMicRequest)
    }
    if (message.type === 'chat') {
      chatMessages.value.push({
        username: (message.username as string) || '匿名',
        content: (message.content as string) || '',
        time: formatTime(message.createdAt as string),
      })
      chatMessages.value = chatMessages.value.slice(-100)
      void scrollChatToBottom()
    }
    if (message.type === 'barrage') {
      const content = (message.content as string) || ''
      const username = (message.username as string) || ''
      spawnBarrage(username ? `${username}：${content}` : content)
      // 同时也加到聊天记录
      chatMessages.value.push({
        username: username || '匿名',
        content: `[弹幕] ${content}`,
        time: formatTime(message.createdAt as string),
      })
      chatMessages.value = chatMessages.value.slice(-100)
      void scrollChatToBottom()
    }
  })
})

onBeforeUnmount(() => {
  socket?.close()
  void disconnectMic(true).finally(() => leaveStudentSession(studentSession))
  studentSession = null
})
</script>

<template>
  <main class="min-h-screen bg-slate-950 px-6 py-8 text-white">
    <div v-if="room" class="mx-auto max-w-7xl">
      <nav class="mb-8 flex items-center justify-between">
        <RouterLink to="/live" class="text-sm text-cyan-200">返回直播中心</RouterLink>
        <RouterLink to="/dashboard" class="rounded-full border border-white/10 px-5 py-2 text-sm hover:bg-white/10">工作台</RouterLink>
      </nav>

      <section class="grid gap-6 lg:grid-cols-[1fr_380px]">
        <!-- 左侧：视频区 -->
        <div class="space-y-6">
          <div class="rounded-[2rem] border border-white/10 bg-black p-6 shadow-2xl">
            <!-- 视频容器（弹幕层叠加在此） -->
            <div class="relative min-h-[460px] overflow-hidden rounded-[1.5rem] bg-gradient-to-br from-slate-900 to-cyan-950">
              <div ref="remoteVideoRef" class="min-h-[460px]" />

              <!-- 占位覆盖层：教师未开播时显示 -->
              <div
                class="pointer-events-none absolute inset-0 grid place-items-center bg-slate-950/40 transition-opacity duration-500"
                :class="agoraMessage === '正在观看教师直播' ? 'opacity-0' : 'opacity-100'"
              >
                <div class="text-center">
                  <Radio class="mx-auto h-16 w-16 text-cyan-200" />
                  <h1 class="mt-6 text-4xl font-black">{{ room.title }}</h1>
                  <p class="mt-3 text-slate-300">{{ agoraMessage }}</p>
                </div>
              </div>

              <!-- 弹幕飘动层 -->
              <div class="pointer-events-none absolute inset-0 overflow-hidden">
                <TransitionGroup name="barrage">
                  <div
                    v-for="b in floatingBarrages"
                    :key="b.id"
                    class="barrage-item absolute whitespace-nowrap rounded-full px-3 py-1 text-sm font-bold shadow-lg"
                    :style="{
                      top: `${b.top}%`,
                      color: b.color,
                      textShadow: '0 1px 4px rgba(0,0,0,0.8)',
                      background: 'rgba(0,0,0,0.35)',
                    }"
                  >
                    {{ b.content }}
                  </div>
                </TransitionGroup>
              </div>
            </div>

            <!-- 连麦画面 -->
            <div v-if="micRequest?.status === 'APPROVED'" class="mt-5 overflow-hidden rounded-[1.5rem] border border-cyan-300/20 bg-slate-950">
              <div ref="localCohostVideoRef" class="min-h-[220px]" />
              <div class="flex items-center justify-between gap-3 bg-slate-900/90 px-5 py-3 text-sm text-cyan-100">
                <span>{{ cohosting ? '本地连麦画面已发布' : '准备发布本地连麦画面' }}</span>
                <button class="inline-flex items-center gap-2 rounded-full border border-amber-200/30 px-4 py-2 text-amber-100" @click="disconnectMic(true)">
                  <PhoneOff class="h-4 w-4" />断开连麦
                </button>
              </div>
            </div>

            <!-- 直播信息栏 -->
            <div class="mt-5 flex flex-wrap items-center justify-between gap-3">
              <div>
                <div class="flex items-center gap-3">
                  <span class="inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-bold" :class="{
                    'bg-green-400/20 text-green-300': room.status === 'LIVE',
                    'bg-amber-400/20 text-amber-300': room.status === 'SCHEDULED',
                    'bg-slate-600/60 text-slate-300': room.status === 'ENDED',
                  }">
                    <span v-if="room.status === 'LIVE'" class="h-1.5 w-1.5 animate-pulse rounded-full bg-green-400" />
                    {{ formatLiveStatus(room.status) }}
                  </span>
                  <span class="inline-flex items-center gap-1 text-sm text-slate-300">
                    <Users class="h-4 w-4" />{{ onlineCount }} 人在线
                  </span>
                </div>
                <p class="mt-2 text-sm text-slate-300">讲师：{{ room.teacherName }}</p>
                <p v-if="micMessage" class="mt-2 text-sm text-amber-100">{{ micMessage }}</p>
              </div>
              <button
                class="rounded-full bg-cyan-300 px-5 py-2 text-sm font-bold text-slate-950 hover:bg-cyan-200 disabled:cursor-not-allowed disabled:opacity-60"
                :disabled="micRequest?.status === 'PENDING' || micRequest?.status === 'APPROVED'"
                @click="requestMic"
              >
                {{ micRequest?.status === 'PENDING' ? '等待审批' : micRequest?.status === 'APPROVED' ? '连麦中' : '申请连麦' }}
              </button>
            </div>
          </div>

          <!-- 投票区 -->
          <section v-if="currentPoll" class="rounded-[2rem] border border-cyan-300/20 bg-cyan-300/10 p-6 backdrop-blur">
            <div class="flex items-center gap-3">
              <Vote class="h-5 w-5 text-cyan-200" />
              <h2 class="text-xl font-black">课堂投票</h2>
              <span class="text-sm text-slate-400">共 {{ currentPoll.totalVotes }} 票</span>
            </div>
            <p class="mt-4 text-lg font-bold">{{ currentPoll.question }}</p>
            <div class="mt-4 space-y-3">
              <label v-for="(option, index) in currentPoll.options" :key="option" class="block rounded-2xl border border-white/10 bg-slate-900/80 p-4">
                <div class="flex items-center gap-3">
                  <input
                    v-model="selectedOptions[currentPoll.id]"
                    type="radio"
                    :value="index"
                    class="h-4 w-4 accent-cyan-300"
                    :disabled="currentPoll.selectedOptionIndex !== null && currentPoll.selectedOptionIndex !== undefined"
                  />
                  <span class="flex-1">{{ option }}</span>
                  <span v-if="currentPoll.totalVotes > 0" class="text-xs text-slate-400">
                    {{ currentPoll.results.find((r) => r.optionIndex === index)?.count || 0 }} 票
                  </span>
                </div>
                <div v-if="currentPoll.totalVotes > 0" class="mt-3 h-1.5 overflow-hidden rounded-full bg-white/10">
                  <div
                    class="h-full rounded-full bg-cyan-300 transition-all duration-500"
                    :style="{ width: `${(((currentPoll.results.find((r) => r.optionIndex === index)?.count || 0) / currentPoll.totalVotes) * 100).toFixed(0)}%` }"
                  />
                </div>
              </label>
            </div>
            <button
              class="mt-5 rounded-full bg-white px-5 py-2 text-sm font-bold text-slate-950 disabled:cursor-not-allowed disabled:opacity-50"
              :disabled="selectedOptions[currentPoll.id] === undefined || (currentPoll.selectedOptionIndex !== null && currentPoll.selectedOptionIndex !== undefined)"
              @click="submitVote(currentPoll)"
            >
              {{ currentPoll.selectedOptionIndex !== null && currentPoll.selectedOptionIndex !== undefined ? '已提交' : '提交投票' }}
            </button>
          </section>
        </div>

        <!-- 右侧：聊天面板 -->
        <aside class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
          <div class="mb-5 flex items-center gap-3">
            <MessageCircle class="h-5 w-5 text-cyan-200" />
            <h2 class="text-xl font-black">互动聊天</h2>
          </div>

          <!-- 聊天消息列表 -->
          <div ref="chatListRef" class="h-[420px] space-y-2 overflow-auto rounded-2xl bg-slate-900 p-4 scroll-smooth">
            <div v-if="chatMessages.length === 0" class="flex h-full items-center justify-center text-sm text-slate-500">
              暂无消息，快来发送聊天吧
            </div>
            <div v-for="(msg, index) in chatMessages" :key="index" class="rounded-xl bg-white/5 px-3 py-2 text-sm">
              <div class="flex items-baseline gap-2">
                <span class="font-semibold text-cyan-300">{{ msg.username }}</span>
                <span class="text-xs text-slate-500">{{ msg.time }}</span>
              </div>
              <p class="mt-1 break-all text-slate-200">{{ msg.content }}</p>
            </div>
          </div>

          <!-- 输入区 -->
          <div class="mt-4 grid gap-3">
            <div class="flex gap-2">
              <input
                v-model="chat"
                class="min-w-0 flex-1 rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300"
                placeholder="发聊天..."
                @keyup.enter="sendChat"
              />
              <button class="rounded-2xl bg-cyan-300 px-4 text-slate-950 hover:bg-cyan-200" @click="sendChat">
                <Send class="h-4 w-4" />
              </button>
            </div>
            <div class="flex gap-2">
              <input
                v-model="barrage"
                class="min-w-0 flex-1 rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300"
                placeholder="发弹幕（会飘过视频）..."
                @keyup.enter="sendBarrage"
              />
              <button class="rounded-2xl border border-cyan-300/40 px-4 text-cyan-100 hover:bg-cyan-300/10" @click="sendBarrage">弹幕</button>
            </div>
          </div>
        </aside>
      </section>
    </div>

    <!-- 加载占位 -->
    <div v-else class="mx-auto max-w-7xl">
      <div class="h-8 w-32 animate-pulse rounded-full bg-white/10" />
      <div class="mt-8 grid gap-6 lg:grid-cols-[1fr_380px]">
        <div class="min-h-[500px] animate-pulse rounded-[2rem] bg-white/10" />
        <div class="animate-pulse rounded-[2rem] bg-white/10" />
      </div>
    </div>
  </main>
</template>

<style scoped>
/* 弹幕飘动动画：从右向左 */
.barrage-item {
  right: -100%;
  animation: barrageFlow 8s linear forwards;
}

@keyframes barrageFlow {
  from { right: -20%; }
  to { right: 110%; }
}

.barrage-enter-active { animation: barrageFlow 8s linear forwards; }
.barrage-leave-active { display: none; }
</style>
