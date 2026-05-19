export type LiveSocketMessage = Record<string, unknown>

export function createLiveSocket(roomId: number, onMessage: (message: LiveSocketMessage) => void) {
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  const socket = new WebSocket(`${protocol}://${window.location.host}/api/ws/live/${roomId}`)

  socket.onmessage = (event) => {
    try {
      onMessage(JSON.parse(event.data))
    } catch {
      onMessage({ type: 'raw', content: event.data })
    }
  }

  return socket
}
