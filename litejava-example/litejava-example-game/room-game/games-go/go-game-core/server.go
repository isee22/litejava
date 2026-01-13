package core

import (
	"crypto/md5"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"strconv"
	"sync"
	"time"

	"github.com/gorilla/websocket"
)

// GameServer 游戏服务器基类
type GameServer struct {
	Config     *Config
	Rooms      map[string]*Room
	Sessions   *SessionManager
	Settlement *SettlementService
	upgrader   websocket.Upgrader
	mu         sync.RWMutex

	// 游戏处理器 (子类实现)
	GameHandler GameHandler
}

// GameHandler 游戏处理器接口
type GameHandler interface {
	// GetGameName 游戏名称
	GetGameName() string
	// GetMaxPlayers 最大玩家数
	GetMaxPlayers() int
	// OnGameCmd 处理游戏命令
	OnGameCmd(s *GameServer, userID int64, room *Room, cmd int, data json.RawMessage)
	// StartGame 开始游戏
	StartGame(s *GameServer, room *Room)
	// GetGameState 获取游戏状态 (断线重连)
	GetGameState(room *Room, seatIndex int) any
}

// Message WebSocket 消息
type Message struct {
	Cmd  int             `json:"cmd"`
	Code int             `json:"code,omitempty"`
	Data json.RawMessage `json:"data,omitempty"`
}

// NewGameServer 创建游戏服务器
func NewGameServer(cfg *Config, handler GameHandler) *GameServer {
	return &GameServer{
		Config:      cfg,
		Rooms:       make(map[string]*Room),
		Sessions:    NewSessionManager(),
		Settlement:  NewSettlementService(cfg.Account.URL),
		upgrader:    websocket.Upgrader{CheckOrigin: func(r *http.Request) bool { return true }},
		GameHandler: handler,
	}
}

// Start 启动服务器
func (s *GameServer) Start() {
	// HTTP 路由
	http.HandleFunc("/create_room", s.handleCreateRoom)
	http.HandleFunc("/enter_room", s.handleEnterRoom)
	http.HandleFunc("/is_room_runing", s.handleIsRoomRunning)
	http.HandleFunc("/health", s.handleHealth)
	http.HandleFunc("/status", s.handleStatus)
	http.HandleFunc("/game", s.handleWebSocket)

	// 注册到 HallServer
	go s.registerLoop()

	serverID := s.GetServerID()
	log.Printf("=== %s [%s] ===", s.GameHandler.GetGameName(), serverID)
	log.Printf("WebSocket: ws://%s:%d/game", s.Config.Server.ClientIP, s.Config.Server.WsPort)
	log.Printf("HTTP: http://%s:%d", s.Config.Server.ClientIP, s.Config.Server.HttpPort)

	log.Fatal(http.ListenAndServe(fmt.Sprintf(":%d", s.Config.Server.HttpPort), nil))
}


// GetServerID 获取服务器ID
func (s *GameServer) GetServerID() string {
	return fmt.Sprintf("%s:%d", s.Config.Server.ClientIP, s.Config.Server.WsPort)
}

// GetLoad 获取负载
func (s *GameServer) GetLoad() int {
	s.mu.RLock()
	defer s.mu.RUnlock()
	count := 0
	for _, room := range s.Rooms {
		count += room.PlayerCount()
	}
	return len(s.Rooms)*10 + count
}

// GetRoom 获取用户所在房间
func (s *GameServer) GetRoom(userID int64) *Room {
	roomID := s.Sessions.GetUserRoom(userID)
	if roomID == "" {
		return nil
	}
	return s.Rooms[roomID]
}

// Md5Sign MD5签名
func (s *GameServer) Md5Sign(parts ...string) string {
	h := md5.New()
	for _, p := range parts {
		h.Write([]byte(p))
	}
	h.Write([]byte(s.Config.Hall.PriKey))
	return hex.EncodeToString(h.Sum(nil))
}

// ==================== HTTP 处理 ====================

func (s *GameServer) handleCreateRoom(w http.ResponseWriter, r *http.Request) {
	userID, _ := strconv.ParseInt(r.URL.Query().Get("userid"), 10, 64)
	roomID := r.URL.Query().Get("roomid")
	conf := r.URL.Query().Get("conf")
	sign := r.URL.Query().Get("sign")

	// 验证签名
	expected1 := s.Md5Sign(fmt.Sprintf("%d", userID), roomID, conf)
	expected2 := s.Md5Sign(fmt.Sprintf("%d", userID), conf)
	if sign != expected1 && sign != expected2 {
		s.jsonResp(w, map[string]any{"errcode": -1, "errmsg": "invalid sign"})
		return
	}

	s.mu.Lock()
	if roomID == "" {
		roomID = fmt.Sprintf("%d", time.Now().UnixNano()%1000000)
	}
	room := NewRoom(roomID, userID, s.GameHandler.GetMaxPlayers())
	s.Rooms[roomID] = room
	s.mu.Unlock()

	log.Printf("创建房间: %s by %d", roomID, userID)
	s.jsonResp(w, map[string]any{"errcode": 0, "roomid": roomID})
}

func (s *GameServer) handleEnterRoom(w http.ResponseWriter, r *http.Request) {
	userID, _ := strconv.ParseInt(r.URL.Query().Get("userid"), 10, 64)
	name := r.URL.Query().Get("name")
	roomID := r.URL.Query().Get("roomid")
	sign := r.URL.Query().Get("sign")

	// 验证签名
	expected := s.Md5Sign(fmt.Sprintf("%d", userID), name, roomID)
	if sign != expected {
		s.jsonResp(w, map[string]any{"errcode": -1, "errmsg": "invalid sign"})
		return
	}

	s.mu.Lock()
	defer s.mu.Unlock()

	room, ok := s.Rooms[roomID]
	if !ok {
		s.jsonResp(w, map[string]any{"errcode": ErrRoomNotFound, "errmsg": "room not found"})
		return
	}

	// 找空位或已有座位
	seat := room.FindSeat(userID)
	if seat == nil {
		seat = room.FindEmptySeat()
		if seat == nil {
			s.jsonResp(w, map[string]any{"errcode": ErrRoomFull, "errmsg": "room full"})
			return
		}
		seat.UserID = userID
		seat.Name = name
	}

	s.Sessions.SetUserRoom(userID, roomID)

	// 生成 token
	token := fmt.Sprintf("%d_%s_%d", userID, roomID, time.Now().UnixNano())
	s.Sessions.AddToken(token, &TokenInfo{
		UserID:  userID,
		RoomID:  roomID,
		Expires: time.Now().Add(5 * time.Minute),
	})

	log.Printf("玩家进入房间: %d -> %s", userID, roomID)
	s.jsonResp(w, map[string]any{"errcode": 0, "token": token})
}

func (s *GameServer) handleIsRoomRunning(w http.ResponseWriter, r *http.Request) {
	roomID := r.URL.Query().Get("roomid")
	s.mu.RLock()
	_, ok := s.Rooms[roomID]
	s.mu.RUnlock()
	s.jsonResp(w, map[string]any{"errcode": 0, "runing": ok})
}

func (s *GameServer) handleHealth(w http.ResponseWriter, r *http.Request) {
	s.jsonResp(w, map[string]any{
		"errcode":  0,
		"status":   "UP",
		"serverId": s.GetServerID(),
	})
}

func (s *GameServer) handleStatus(w http.ResponseWriter, r *http.Request) {
	s.mu.RLock()
	roomCount := len(s.Rooms)
	playerCount := 0
	for _, room := range s.Rooms {
		playerCount += room.PlayerCount()
	}
	s.mu.RUnlock()

	s.jsonResp(w, map[string]any{
		"errcode":  0,
		"serverId": s.GetServerID(),
		"rooms":    roomCount,
		"players":  playerCount,
	})
}

func (s *GameServer) jsonResp(w http.ResponseWriter, data any) {
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(data)
}


// ==================== WebSocket 处理 ====================

func (s *GameServer) handleWebSocket(w http.ResponseWriter, r *http.Request) {
	conn, err := s.upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Printf("WebSocket upgrade failed: %v", err)
		return
	}
	defer conn.Close()

	for {
		_, msgBytes, err := conn.ReadMessage()
		if err != nil {
			s.onDisconnect(conn)
			return
		}

		var msg Message
		if err := json.Unmarshal(msgBytes, &msg); err != nil {
			continue
		}
		s.handleMessage(conn, &msg)
	}
}

func (s *GameServer) handleMessage(conn *websocket.Conn, msg *Message) {
	switch msg.Cmd {
	case CmdLogin:
		s.onLogin(conn, msg.Data)
	case CmdPing:
		s.Send(conn, CmdPing, 0, nil)
	case CmdReady:
		s.onReady(conn)
	case CmdRoomExit:
		s.onExit(conn)
	default:
		// 游戏命令交给 handler 处理
		s.mu.RLock()
		userID := s.Sessions.GetUserID(conn)
		room := s.GetRoom(userID)
		s.mu.RUnlock()
		if room != nil && s.GameHandler != nil {
			s.GameHandler.OnGameCmd(s, userID, room, msg.Cmd, msg.Data)
		}
	}
}

func (s *GameServer) onLogin(conn *websocket.Conn, data json.RawMessage) {
	var req struct {
		Token string `json:"token"`
	}
	json.Unmarshal(data, &req)

	s.mu.Lock()
	tokenInfo := s.Sessions.GetToken(req.Token)
	if tokenInfo == nil || time.Now().After(tokenInfo.Expires) {
		s.mu.Unlock()
		s.Send(conn, CmdLogin, ErrInvalidToken, nil)
		return
	}
	s.Sessions.RemoveToken(req.Token)

	userID := tokenInfo.UserID
	roomID := tokenInfo.RoomID
	room, ok := s.Rooms[roomID]
	if !ok {
		s.mu.Unlock()
		s.Send(conn, CmdLogin, ErrRoomNotFound, nil)
		return
	}

	// 绑定会话
	s.Sessions.Lock()
	oldConn := s.Sessions.BindSession(userID, conn)
	if oldConn != nil {
		oldConn.Close()
	}
	s.Sessions.Unlock()

	// 标记在线
	seatIndex := -1
	room.Lock()
	if seat := room.FindSeat(userID); seat != nil {
		seat.Online = true
		seatIndex = seat.SeatIndex
	}
	room.Unlock()
	s.mu.Unlock()

	// 获取游戏状态 (断线重连)
	var gameState any
	if s.GameHandler != nil {
		gameState = s.GameHandler.GetGameState(room, seatIndex)
	}

	// 响应
	s.Send(conn, CmdLogin, 0, map[string]any{
		"roomId": roomID,
		"seats":  room.Seats,
		"game":   gameState,
	})
	log.Printf("玩家登录: %d -> 房间 %s", userID, roomID)
}

func (s *GameServer) onReady(conn *websocket.Conn) {
	s.mu.Lock()
	userID := s.Sessions.GetUserID(conn)
	room := s.GetRoom(userID)
	if room == nil {
		s.mu.Unlock()
		return
	}

	room.Lock()
	seat := room.FindSeat(userID)
	if seat == nil {
		room.Unlock()
		s.mu.Unlock()
		return
	}
	seat.Ready = true
	seatIndex := seat.SeatIndex
	allReady := room.IsAllReady(s.GameHandler.GetMaxPlayers())
	room.Unlock()
	s.mu.Unlock()

	// 广播准备
	s.Broadcast(room, CmdUserReady, map[string]any{
		"userId":    userID,
		"seatIndex": seatIndex,
	})

	// 检查是否全部准备
	if allReady && s.GameHandler != nil {
		s.GameHandler.StartGame(s, room)
	}
}

func (s *GameServer) onExit(conn *websocket.Conn) {
	s.mu.Lock()
	userID := s.Sessions.GetUserID(conn)
	room := s.GetRoom(userID)
	if room == nil {
		s.mu.Unlock()
		return
	}

	// 游戏中不能退出
	room.RLock()
	// 这里需要检查游戏状态，由子类实现
	room.RUnlock()

	// 退出房间
	room.Lock()
	for _, seat := range room.Seats {
		if seat.UserID == userID {
			seat.UserID = 0
			seat.Name = ""
			seat.Ready = false
			seat.Online = false
			break
		}
	}
	room.Unlock()

	s.Sessions.RemoveUserRoom(userID)
	s.Sessions.Lock()
	s.Sessions.UnbindSession(conn)
	s.Sessions.Unlock()
	s.mu.Unlock()

	// 通知其他人
	s.Broadcast(room, CmdUserExit, map[string]any{"userId": userID})
	s.Send(conn, CmdRoomExit, 0, nil)
	conn.Close()
}

func (s *GameServer) onDisconnect(conn *websocket.Conn) {
	s.mu.Lock()
	s.Sessions.Lock()
	userID := s.Sessions.UnbindSession(conn)
	s.Sessions.Unlock()

	if userID > 0 {
		if room := s.GetRoom(userID); room != nil {
			room.Lock()
			if seat := room.FindSeat(userID); seat != nil {
				seat.Online = false
			}
			room.Unlock()
		}
	}
	s.mu.Unlock()

	if userID > 0 {
		log.Printf("玩家断开: %d", userID)
	}
}


// ==================== HallServer 注册 ====================

func (s *GameServer) registerLoop() {
	s.register()
	ticker := time.NewTicker(5 * time.Second)
	for range ticker.C {
		s.heartbeat()
	}
}

func (s *GameServer) register() {
	url := fmt.Sprintf("%s/register_gs?clientip=%s&clientport=%d&httpPort=%d&load=%d&gameType=%s",
		s.Config.Hall.URL,
		s.Config.Server.ClientIP,
		s.Config.Server.WsPort,
		s.Config.Server.HttpPort,
		s.GetLoad(),
		s.Config.Game.Type,
	)
	resp, err := http.Get(url)
	if err != nil {
		log.Printf("注册失败: %v", err)
		return
	}
	resp.Body.Close()
	log.Println("注册到 HallServer 成功")
}

func (s *GameServer) heartbeat() {
	url := fmt.Sprintf("%s/heartbeat?id=%s&load=%d",
		s.Config.Hall.URL,
		s.GetServerID(),
		s.GetLoad(),
	)
	resp, err := http.Get(url)
	if err != nil {
		log.Printf("心跳失败: %v", err)
		s.register()
		return
	}
	resp.Body.Close()
}

// ==================== 消息发送 ====================

// Send 发送消息
func (s *GameServer) Send(conn *websocket.Conn, cmd, code int, data any) {
	msg := map[string]any{"cmd": cmd, "code": code}
	if data != nil {
		msg["data"] = data
	}
	bytes, _ := json.Marshal(msg)
	conn.WriteMessage(websocket.TextMessage, bytes)
}

// SendTo 发送给指定用户
func (s *GameServer) SendTo(userID int64, cmd int, data any) {
	s.Sessions.RLock()
	conn := s.Sessions.GetConn(userID)
	s.Sessions.RUnlock()
	if conn != nil {
		s.Send(conn, cmd, 0, data)
	}
}

// Broadcast 广播给房间所有人
func (s *GameServer) Broadcast(room *Room, cmd int, data any) {
	msg := map[string]any{"cmd": cmd, "code": 0}
	if data != nil {
		msg["data"] = data
	}
	bytes, _ := json.Marshal(msg)

	room.RLock()
	userIDs := make([]int64, 0)
	for _, seat := range room.Seats {
		if seat.UserID > 0 {
			userIDs = append(userIDs, seat.UserID)
		}
	}
	room.RUnlock()

	s.Sessions.RLock()
	for _, userID := range userIDs {
		if conn := s.Sessions.GetConn(userID); conn != nil {
			conn.WriteMessage(websocket.TextMessage, bytes)
		}
	}
	s.Sessions.RUnlock()
}

// BroadcastExclude 广播给房间所有人 (排除指定用户)
func (s *GameServer) BroadcastExclude(room *Room, cmd int, data any, exclude int64) {
	msg := map[string]any{"cmd": cmd, "code": 0}
	if data != nil {
		msg["data"] = data
	}
	bytes, _ := json.Marshal(msg)

	room.RLock()
	userIDs := make([]int64, 0)
	for _, seat := range room.Seats {
		if seat.UserID > 0 && seat.UserID != exclude {
			userIDs = append(userIDs, seat.UserID)
		}
	}
	room.RUnlock()

	s.Sessions.RLock()
	for _, userID := range userIDs {
		if conn := s.Sessions.GetConn(userID); conn != nil {
			conn.WriteMessage(websocket.TextMessage, bytes)
		}
	}
	s.Sessions.RUnlock()
}

// Lock 加锁
func (s *GameServer) Lock() { s.mu.Lock() }

// Unlock 解锁
func (s *GameServer) Unlock() { s.mu.Unlock() }

// RLock 读锁
func (s *GameServer) RLock() { s.mu.RLock() }

// RUnlock 读解锁
func (s *GameServer) RUnlock() { s.mu.RUnlock() }
