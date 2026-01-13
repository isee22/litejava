package core

import (
	"encoding/json"
	"sync"
	"time"

	"github.com/gorilla/websocket"
)

type TokenInfo struct {
	UserID  int64
	RoomID  string
	Expires time.Time
}

type SessionManager struct {
	sessions map[int64]*websocket.Conn
	connUser map[*websocket.Conn]int64
	userRoom map[int64]string
	tokens   map[string]*TokenInfo
	mu       sync.RWMutex
}

func NewSessionManager() *SessionManager {
	return &SessionManager{
		sessions: make(map[int64]*websocket.Conn),
		connUser: make(map[*websocket.Conn]int64),
		userRoom: make(map[int64]string),
		tokens:   make(map[string]*TokenInfo),
	}
}

func (m *SessionManager) Lock()    { m.mu.Lock() }
func (m *SessionManager) Unlock()  { m.mu.Unlock() }
func (m *SessionManager) RLock()   { m.mu.RLock() }
func (m *SessionManager) RUnlock() { m.mu.RUnlock() }

// Token 管理
func (m *SessionManager) AddToken(token string, info *TokenInfo) {
	m.tokens[token] = info
}

func (m *SessionManager) GetToken(token string) *TokenInfo {
	return m.tokens[token]
}

func (m *SessionManager) RemoveToken(token string) {
	delete(m.tokens, token)
}

// 会话管理
func (m *SessionManager) BindSession(userID int64, conn *websocket.Conn) *websocket.Conn {
	oldConn := m.sessions[userID]
	if oldConn != nil {
		delete(m.connUser, oldConn)
	}
	m.sessions[userID] = conn
	m.connUser[conn] = userID
	return oldConn
}

func (m *SessionManager) UnbindSession(conn *websocket.Conn) int64 {
	userID, ok := m.connUser[conn]
	if ok {
		delete(m.sessions, userID)
		delete(m.connUser, conn)
	}
	return userID
}

func (m *SessionManager) GetConn(userID int64) *websocket.Conn {
	return m.sessions[userID]
}

func (m *SessionManager) GetUserID(conn *websocket.Conn) int64 {
	return m.connUser[conn]
}

// 房间映射
func (m *SessionManager) SetUserRoom(userID int64, roomID string) {
	m.userRoom[userID] = roomID
}

func (m *SessionManager) GetUserRoom(userID int64) string {
	return m.userRoom[userID]
}

func (m *SessionManager) RemoveUserRoom(userID int64) {
	delete(m.userRoom, userID)
}

// 消息发送
func (m *SessionManager) Send(conn *websocket.Conn, cmd, code int, data any) {
	msg := map[string]any{"cmd": cmd, "code": code}
	if data != nil {
		msg["data"] = data
	}
	bytes, _ := json.Marshal(msg)
	conn.WriteMessage(websocket.TextMessage, bytes)
}

func (m *SessionManager) SendTo(userID int64, cmd int, data any) {
	m.RLock()
	conn := m.sessions[userID]
	m.RUnlock()
	if conn != nil {
		m.Send(conn, cmd, 0, data)
	}
}

func (m *SessionManager) Broadcast(userIDs []int64, cmd int, data any) {
	msg := map[string]any{"cmd": cmd, "code": 0}
	if data != nil {
		msg["data"] = data
	}
	bytes, _ := json.Marshal(msg)

	m.RLock()
	for _, userID := range userIDs {
		if conn := m.sessions[userID]; conn != nil {
			conn.WriteMessage(websocket.TextMessage, bytes)
		}
	}
	m.RUnlock()
}
