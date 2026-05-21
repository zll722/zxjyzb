<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MessageCircle, PhoneOff, Radio, Send, Users, Vote } from 'lucide-vue-next'
import type { IAgoraRTCRemoteUser } from 'agora-rtc-sdk-ng'
import { formatLiveStatus, getToken, liveApi, type LiveMicRequest, type LivePollDetail, type LiveRoom } from '@/lib/api'
import { createLiveSocket, type LiveSocketMessage } from '@/lib/liveSocket'
import { useAuth } from '@/composables/useAuth'
import { createStudentSession, leaveStudentSession, startStudentCohost, stopStudentCohost, type IAgoraRTCClient, type StudentAgoraSession } from '@/lib/agora'

const route = useRoute()
const router = useRouter()
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

interface BarrageItem { id: number; content: string; top: number; color: string }
const floatingBarrages = ref<BarrageItem[]>([])
let barrageSeq = 0

// CSS custom properties for barrage colors — indigo / violet palette
const BARRAGE_COLORS = [
  'var(--barrage-indigo)',
  'var(--barrage-violet)',
  'var(--barrage-purple)',
  'var(--barrage-sky)',
  'var(--barrage-white)',
  'var(--barrage-amber)',
  'var(--barrage-teal)',
]

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

async function load() {
  room.value = await liveApi.detail(Number(route.params.id))
  onlineCount.value = room.value.onlineCount
  // Teachers should manage their own rooms from the teacher panel
  const user = auth.state.user
  if (user?.role === 'TEACHER' && user.id === room.value.teacherId) {
    router.replace('/teacher/live')
    return
  }
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

async function subscribeTeacher(client: IAgoraRTCClient, user: IAgoraRTCRemoteUser, mediaType: 'audio' | 'video') {
  await client.subscribe(user, mediaType)
  if (mediaType === 'video' && user.videoTrack) {
    await nextTick()
    if (remoteVideoRef.value) {
      user.videoTrack.play(remoteVideoRef.value)
      agoraMessage.value = '正在观看教师直播'
    }
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
    studentSession = null; cohosting.value = false
    agoraMessage.value = error instanceof Error ? error.message : '加入频道失败'
  }
}

async function beginCohost() {
  if (!studentSession) return
  try {
    await nextTick()
    if (!localCohostVideoRef.value) throw new Error('本地连麦预览容器未准备好')
    await startStudentCohost(studentSession, localCohostVideoRef.value)
    cohosting.value = true; micMessage.value = '连麦中，已发布本地麦克风和摄像头'
  } catch (error) {
    cohosting.value = false; micMessage.value = error instanceof Error ? error.message : '连麦发布失败'
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
  if (request.status === 'APPROVED') { micMessage.value = '教师已同意连麦申请'; await beginCohost() }
  else if (request.status === 'ENDED') { await disconnectMic(false); micMessage.value = '连麦已结束' }
  else { micMessage.value = '教师已拒绝连麦申请' }
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
    if (message.type === 'online' && typeof message.count === 'number') onlineCount.value = message.count
    if (message.type === 'room_ended') {
      if (room.value) room.value = { ...room.value, status: 'ENDED' }
      agoraMessage.value = '直播已结束'
      void leaveStudentSession(studentSession)
      studentSession = null
    }
    if ((message.type === 'poll' || message.type === 'poll_result') && message.poll) upsertPoll(message.poll as LivePollDetail)
    if (message.type === 'poll_dismiss' && typeof message.pollId === 'number') polls.value = polls.value.filter((p) => p.id !== message.pollId)
    if ((message.type === 'mic_response' || message.type === 'mic_end') && message.request) void handleMicSignal(message.request as LiveMicRequest)
    if (message.type === 'chat') {
      chatMessages.value.push({ username: (message.username as string) || '匿名', content: (message.content as string) || '', time: formatTime(message.createdAt as string) })
      chatMessages.value = chatMessages.value.slice(-100)
      void scrollChatToBottom()
    }
    if (message.type === 'barrage') {
      const content = (message.content as string) || ''
      const username = (message.username as string) || ''
      spawnBarrage(username ? `${username}：${content}` : content)
      chatMessages.value.push({ username: username || '匿名', content: `[弹幕] ${content}`, time: formatTime(message.createdAt as string) })
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
  <main class="live-room min-h-screen px-4 py-6 sm:px-6 sm:py-8">
    <div v-if="room" class="mx-auto max-w-7xl">
      <nav class="mb-6 flex items-center justify-between">
        <RouterLink to="/live" class="text-sm text-indigo-300 hover:text-indigo-200">返回直播中心</RouterLink>
        <RouterLink to="/dashboard" class="rounded-lg border border-white/10 px-4 py-2 text-sm hover:bg-white/10">工作台</RouterLink>
      </nav>

      <section class="grid gap-6 xl:grid-cols-[1fr_380px]">
        <!-- Video area -->
        <div class="space-y-5">
          <div class="rounded-2xl border border-white/10 bg-black p-5 shadow-2xl">
            <!-- Video + barrage overlay -->
            <div class="relative h-[300px] overflow-hidden rounded-xl bg-gradient-to-br from-neutral-900 to-indigo-950 md:h-[440px]">
              <div ref="remoteVideoRef" class="h-full w-full" />

              <!-- Placeholder overlay -->
              <div
                class="pointer-events-none absolute inset-0 grid place-items-center transition-opacity duration-500"
                :class="[
                  agoraMessage === '正在观看教师直播' ? 'opacity-0' : 'opacity-100',
                  room.status === 'ENDED' ? 'bg-neutral-950/80' : 'bg-neutral-950/40',
                ]"
              >
                <div class="text-center px-6">
                  <Radio class="mx-auto h-14 w-14" :class="room.status === 'ENDED' ? 'text-neutral-500' : 'text-indigo-300'" />
                  <h1 class="mt-5 text-xl font-bold text-white md:text-3xl">{{ room.title }}</h1>
                  <p class="mt-3" :class="room.status === 'ENDED' ? 'text-neutral-400' : 'text-neutral-300'">{{ agoraMessage }}</p>
                  <p v-if="room.status === 'ENDED'" class="mt-4 text-sm text-neutral-500">本场直播已结束，感谢观看</p>
                </div>
              </div>

              <!-- Floating barrage layer -->
              <div class="pointer-events-none absolute inset-0 overflow-hidden">
                <TransitionGroup name="barrage">
                  <div
                    v-for="b in floatingBarrages"
                    :key="b.id"
                    class="barrage-item absolute whitespace-nowrap rounded-full px-3 py-1 text-sm font-semibold"
                    :style="{
                      top: `${b.top}%`,
                      color: b.color,
                      textShadow: '0 1px 4px rgba(0,0,0,0.9)',
                      background: 'rgba(0,0,0,0.4)',
                    }"
                  >
                    {{ b.content }}
                  </div>
                </TransitionGroup>
              </div>
            </div>

            <!-- Cohost local video -->
            <div v-if="micRequest?.status === 'APPROVED'" class="mt-4 overflow-hidden rounded-xl border border-indigo-400/20 bg-neutral-950">
              <div ref="localCohostVideoRef" class="min-h-[200px]" />
              <div class="flex items-center justify-between gap-3 bg-neutral-900/90 px-4 py-3 text-sm text-indigo-200">
                <span>{{ cohosting ? '本地连麦画面已发布' : '准备发布本地连麦画面' }}</span>
                <button class="inline-flex items-center gap-2 rounded-lg border border-amber-300/30 px-3 py-1.5 text-amber-200 hover:bg-amber-400/10" @click="disconnectMic(true)">
                  <PhoneOff class="h-4 w-4" />断开连麦
                </button>
              </div>
            </div>

            <!-- Room info bar -->
            <div class="mt-4 flex flex-wrap items-center justify-between gap-3">
              <div>
                <div class="flex items-center gap-3">
                  <span
                    class="inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-semibold"
                    :class="{
                      'bg-emerald-400/20 text-emerald-300': room.status === 'LIVE',
                      'bg-amber-400/20 text-amber-300': room.status === 'SCHEDULED',
                      'bg-neutral-600/60 text-neutral-300': room.status === 'ENDED',
                    }"
                  >
                    <span v-if="room.status === 'LIVE'" class="h-1.5 w-1.5 animate-pulse rounded-full bg-emerald-400" />
                    {{ formatLiveStatus(room.status) }}
                  </span>
                  <span class="inline-flex items-center gap-1 text-sm text-neutral-300">
                    <Users class="h-4 w-4" />{{ onlineCount }} 人在线
                  </span>
                </div>
                <p class="mt-2 text-sm text-neutral-300">讲师：{{ room.teacherName }}</p>
                <p v-if="micMessage" class="mt-1.5 text-sm text-amber-300">{{ micMessage }}</p>
              </div>
              <button
                class="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-indigo-500 disabled:cursor-not-allowed disabled:opacity-60"
                :disabled="micRequest?.status === 'PENDING' || micRequest?.status === 'APPROVED'"
                @click="requestMic"
              >
                {{ micRequest?.status === 'PENDING' ? '等待审批' : micRequest?.status === 'APPROVED' ? '连麦中' : '申请连麦' }}
              </button>
            </div>
          </div>

          <!-- Poll -->
          <section v-if="currentPoll" class="rounded-2xl border border-indigo-400/20 bg-indigo-950/50 p-5">
            <div class="flex items-center gap-3">
              <Vote class="h-5 w-5 text-indigo-300" />
              <h2 class="font-semibold text-white">课堂投票</h2>
              <span class="text-sm text-neutral-400">共 {{ currentPoll.totalVotes }} 票</span>
            </div>
            <p class="mt-4 font-semibold text-white">{{ currentPoll.question }}</p>
            <div class="mt-4 space-y-3">
              <label v-for="(option, index) in currentPoll.options" :key="option" class="block cursor-pointer rounded-xl border border-white/10 bg-neutral-900/80 p-4 transition hover:border-indigo-400/40">
                <div class="flex items-center gap-3">
                  <input
                    v-model="selectedOptions[currentPoll.id]"
                    type="radio"
                    :value="index"
                    class="h-4 w-4 accent-indigo-500"
                    :disabled="currentPoll.selectedOptionIndex !== null && currentPoll.selectedOptionIndex !== undefined"
                  />
                  <span class="flex-1 text-sm text-neutral-200">{{ option }}</span>
                  <span v-if="currentPoll.totalVotes > 0" class="text-xs text-neutral-400">
                    {{ currentPoll.results.find((r) => r.optionIndex === index)?.count || 0 }} 票
                  </span>
                </div>
                <div v-if="currentPoll.totalVotes > 0" class="mt-3 h-1.5 overflow-hidden rounded-full bg-white/10">
                  <div
                    class="h-full rounded-full bg-indigo-500 transition-all duration-500"
                    :style="{ width: `${(((currentPoll.results.find((r) => r.optionIndex === index)?.count || 0) / currentPoll.totalVotes) * 100).toFixed(0)}%` }"
                  />
                </div>
              </label>
            </div>
            <button
              class="mt-5 rounded-lg bg-indigo-600 px-5 py-2 text-sm font-semibold text-white transition hover:bg-indigo-500 disabled:cursor-not-allowed disabled:opacity-50"
              :disabled="selectedOptions[currentPoll.id] === undefined || (currentPoll.selectedOptionIndex !== null && currentPoll.selectedOptionIndex !== undefined)"
              @click="submitVote(currentPoll)"
            >
              {{ currentPoll.selectedOptionIndex !== null && currentPoll.selectedOptionIndex !== undefined ? '已提交' : '提交投票' }}
            </button>
          </section>
        </div>

        <!-- Chat panel -->
        <aside class="rounded-2xl border border-white/10 bg-neutral-900/60 p-4 backdrop-blur md:p-5">
          <div class="mb-4 flex items-center gap-3">
            <MessageCircle class="h-5 w-5 text-indigo-300" />
            <h2 class="font-semibold text-white">互动聊天</h2>
          </div>

          <div ref="chatListRef" class="h-[300px] space-y-2 overflow-auto rounded-xl bg-neutral-950/60 p-3 scroll-smooth md:h-[420px]">
            <div v-if="chatMessages.length === 0" class="flex h-full items-center justify-center text-sm text-neutral-500">
              暂无消息，快来发送聊天吧
            </div>
            <div v-for="(msg, index) in chatMessages" :key="index" class="rounded-lg bg-white/5 px-3 py-2 text-sm">
              <div class="flex items-baseline gap-2">
                <span class="font-semibold text-indigo-300">{{ msg.username }}</span>
                <span class="text-xs text-neutral-500">{{ msg.time }}</span>
              </div>
              <p class="mt-1 break-all text-neutral-200">{{ msg.content }}</p>
            </div>
          </div>

          <div class="mt-4 grid gap-3">
            <div class="flex gap-2">
              <input
                v-model="chat"
                class="live-input min-w-0 flex-1 rounded-lg border border-white/10 bg-neutral-900/80 px-3 py-2 text-sm text-white outline-none transition placeholder:text-neutral-500 focus:border-indigo-400"
                placeholder="发聊天..."
                @keyup.enter="sendChat"
              />
              <button class="rounded-lg bg-indigo-600 px-3 text-white hover:bg-indigo-500" @click="sendChat">
                <Send class="h-4 w-4" />
              </button>
            </div>
            <div class="flex gap-2">
              <input
                v-model="barrage"
                class="live-input min-w-0 flex-1 rounded-lg border border-white/10 bg-neutral-900/80 px-3 py-2 text-sm text-white outline-none transition placeholder:text-neutral-500 focus:border-indigo-400"
                placeholder="发弹幕（会飘过视频）..."
                @keyup.enter="sendBarrage"
              />
              <button class="rounded-lg border border-indigo-400/40 px-3 text-sm text-indigo-200 hover:bg-indigo-400/10" @click="sendBarrage">弹幕</button>
            </div>
          </div>
        </aside>
      </section>
    </div>

    <!-- Loading skeleton -->
    <div v-else class="mx-auto max-w-7xl">
      <div class="h-7 w-28 animate-pulse rounded-lg bg-white/10" />
      <div class="mt-6 grid gap-6 lg:grid-cols-[1fr_380px]">
        <div class="min-h-[480px] animate-pulse rounded-2xl bg-white/10" />
        <div class="animate-pulse rounded-2xl bg-white/10" />
      </div>
    </div>
  </main>
</template>

<style scoped>
/* Barrage colors as CSS custom properties */
.live-room {
  background: #020617;
  color: #fff;

  --barrage-indigo: #a5b4fc;
  --barrage-violet: #c4b5fd;
  --barrage-purple: #d8b4fe;
  --barrage-sky: #7dd3fc;
  --barrage-white: #f1f5f9;
  --barrage-amber: #fde68a;
  --barrage-teal: #99f6e4;
}

/* Barrage float animation: right to left */
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
