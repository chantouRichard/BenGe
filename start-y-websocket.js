// start-y-websocket.js
const http = require('http')
const WebSocket = require('ws')
const { setupWSConnection } = require('y-websocket/bin/utils')

const server = http.createServer()
const wss = new WebSocket.Server({ server })

wss.on('connection', (conn, req) => {
  setupWSConnection(conn, req)
})

const port = 1234
server.listen(port, () => {
  console.log(`✅ y-websocket is running at ws://localhost:${port}`)
})
