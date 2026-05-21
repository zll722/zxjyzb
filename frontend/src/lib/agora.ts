import AgoraRTC, {
  AREAS,
  type IAgoraRTCClient,
  type IAgoraRTCRemoteUser,
  type ICameraVideoTrack,
  type ILocalVideoTrack,
  type IMicrophoneAudioTrack,
  type UID,
} from 'agora-rtc-sdk-ng'
export type { IAgoraRTCClient }
import type { LiveRoom } from '@/lib/api'

// 声网国内版（shengwang.cn）：强制使用中国大陆节点
AgoraRTC.setArea({ areaCode: [AREAS.CHINA] })
if (import.meta.env.PROD) AgoraRTC.setLogLevel(3)

export interface TeacherAgoraSession {
  client: IAgoraRTCClient
  audioTrack: IMicrophoneAudioTrack
  videoTrack: ILocalVideoTrack
  uid: UID
  videoSource: 'camera' | 'screen'
  cameraLabel: string
  videoTrackState: string
  videoResolution: string
}

export interface StudentAgoraSession {
  client: IAgoraRTCClient
  uid: UID
  audioTrack?: IMicrophoneAudioTrack
  videoTrack?: ICameraVideoTrack
  cohosting: boolean
}

export function createLiveClient() {
  return AgoraRTC.createClient({ mode: 'live', codec: 'vp8' })
}

export function requireAgoraRoom(room: LiveRoom) {
  if (!room.agoraAppId) throw new Error('Agora AppId 未配置')
  if (!room.agoraChannel) throw new Error('Agora 频道未配置')
  return {
    appId: room.agoraAppId,
    channel: room.agoraChannel,
    token: room.agoraToken || null,
  }
}

export async function createTeacherSession(
  room: LiveRoom,
  videoContainer: HTMLElement,
  onUserPublished?: (user: IAgoraRTCRemoteUser, mediaType: 'audio' | 'video') => Promise<void>,
  onUserLeft?: (user: IAgoraRTCRemoteUser) => void,
) {
  const config = requireAgoraRoom(room)
  const client = createLiveClient()
  if (onUserPublished) client.on('user-published', onUserPublished)
  if (onUserLeft) {
    client.on('user-unpublished', onUserLeft)
    client.on('user-left', onUserLeft)
  }
  await client.setClientRole('host')
  const uid = await client.join(config.appId, config.channel, config.token)
  const cameras = await AgoraRTC.getCameras().catch(() => [])
  const [audioTrack, videoTrack] = await AgoraRTC.createMicrophoneAndCameraTracks()
  videoTrack.play(videoContainer, { mirror: false })
  await client.publish([audioTrack, videoTrack])
  const mediaTrack = videoTrack.getMediaStreamTrack()
  const settings = mediaTrack.getSettings()
  const cameraLabel = mediaTrack.label || cameras[0]?.label || '未识别摄像头名称'
  const videoResolution = settings.width && settings.height ? `${settings.width}×${settings.height}` : '分辨率未知'
  return { client, audioTrack, videoTrack, uid, videoSource: 'camera' as const, cameraLabel, videoTrackState: mediaTrack.readyState, videoResolution }
}

export async function switchTeacherVideoSource(session: TeacherAgoraSession, source: 'camera' | 'screen', videoContainer: HTMLElement) {
  if (session.videoSource === source) return session.videoTrack
  const oldTrack = session.videoTrack
  const nextTrack = source === 'camera' ? await AgoraRTC.createCameraVideoTrack() : await createScreenTrack()
  await session.client.unpublish(oldTrack).catch(() => undefined)
  oldTrack.stop()
  oldTrack.close()
  session.videoTrack = nextTrack
  session.videoSource = source
  const mediaTrack = nextTrack.getMediaStreamTrack()
  const settings = mediaTrack.getSettings()
  session.cameraLabel = source === 'camera' ? mediaTrack.label || '未识别摄像头名称' : '屏幕共享'
  session.videoTrackState = mediaTrack.readyState
  session.videoResolution = settings.width && settings.height ? `${settings.width}×${settings.height}` : '分辨率未知'
  nextTrack.play(videoContainer, { mirror: false })
  await session.client.publish(nextTrack)
  return nextTrack
}

export async function createStudentSession(
  room: LiveRoom,
  onUserPublished: (client: IAgoraRTCClient, user: IAgoraRTCRemoteUser, mediaType: 'audio' | 'video') => Promise<void>,
) {
  const config = requireAgoraRoom(room)
  const client = createLiveClient()
  // Pass client explicitly so the callback never depends on an external ref that may not yet be set
  client.on('user-published', (user, mediaType) => onUserPublished(client, user, mediaType))
  // level 2 = ultra-low-latency interactive mode; required to call client.subscribe()
  await client.setClientRole('audience', { level: 2 })
  const uid = await client.join(config.appId, config.channel, config.token)
  return { client, uid, cohosting: false }
}

export async function startStudentCohost(session: StudentAgoraSession, videoContainer: HTMLElement) {
  if (session.cohosting) return
  await session.client.setClientRole('host')
  const [audioTrack, videoTrack] = await AgoraRTC.createMicrophoneAndCameraTracks()
  videoTrack.play(videoContainer)
  await session.client.publish([audioTrack, videoTrack])
  session.audioTrack = audioTrack
  session.videoTrack = videoTrack
  session.cohosting = true
}

export async function stopStudentCohost(session: StudentAgoraSession | null) {
  if (!session || !session.cohosting) return
  const tracks = [session.audioTrack, session.videoTrack].filter(Boolean) as Array<IMicrophoneAudioTrack | ICameraVideoTrack>
  await session.client.unpublish(tracks).catch(() => undefined)
  tracks.forEach((track) => {
    track.stop()
    track.close()
  })
  session.audioTrack = undefined
  session.videoTrack = undefined
  session.cohosting = false
  await session.client.setClientRole('audience').catch(() => undefined)
}

export async function leaveTeacherSession(session: TeacherAgoraSession | null) {
  if (!session) return
  session.audioTrack.stop()
  session.audioTrack.close()
  session.videoTrack.stop()
  session.videoTrack.close()
  session.client.removeAllListeners()
  await session.client.unpublish().catch(() => undefined)
  await session.client.leave().catch(() => undefined)
}

export async function leaveStudentSession(session: StudentAgoraSession | null) {
  if (!session) return
  await stopStudentCohost(session)
  session.client.removeAllListeners()
  await session.client.leave().catch(() => undefined)
}

async function createScreenTrack() {
  const track = await AgoraRTC.createScreenVideoTrack({ encoderConfig: '1080p_1' }, 'disable')
  return Array.isArray(track) ? track[0] : track as ILocalVideoTrack
}
