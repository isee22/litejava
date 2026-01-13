package main

import (
	"encoding/json"
	"game-core-go"
	"log"
	"sync"
)

// 麻将特有命令
const (
	CmdDiscard = 1100 // 出牌
	CmdPeng    = 1101 // 碰
	CmdGang    = 1102 // 杠
	CmdHu      = 1103 // 胡牌
)

// 错误码
const (
	ErrNotYourTurn  = 30
	ErrInvalidTile  = 31
)

// Mahjong2Handler 二人麻将处理器
type Mahjong2Handler struct {
	games map[string]*Mahjong2Game // roomID -> game
	mu    sync.RWMutex
}

func (h *Mahjong2Handler) GetGameName() string {
	return "二人麻将 (Go)"
}

func (h *Mahjong2Handler) GetMaxPlayers() int {
	return 2
}

func (h *Mahjong2Handler) OnGameCmd(s *core.GameServer, userID int64, room *core.Room, cmd int, data json.RawMessage) {
	switch cmd {
	case CmdDiscard:
		h.onDiscard(s, userID, room, data)
	}
}

func (h *Mahjong2Handler) StartGame(s *core.GameServer, room *core.Room) {
	game := NewMahjong2Game()
	game.Init()
	game.Deal()

	h.mu.Lock()
	if h.games == nil {
		h.games = make(map[string]*Mahjong2Game)
	}
	h.games[room.ID] = game
	h.mu.Unlock()

	// 广播游戏开始
	s.Broadcast(room, core.CmdGameStart, nil)

	// 发牌给各玩家
	room.RLock()
	for i, seat := range room.Seats {
		if seat.UserID > 0 && i < 2 {
			s.SendTo(seat.UserID, core.CmdDeal, map[string]any{
				"tiles": game.GetHand(i),
			})
		}
	}
	room.RUnlock()

	// 庄家摸牌
	tile := game.Draw(0)
	if tile >= 0 {
		room.RLock()
		s.SendTo(room.Seats[0].UserID, core.CmdDraw, map[string]any{"tile": tile})
		room.RUnlock()
	}

	// 通知轮到庄家
	s.Broadcast(room, core.CmdTurn, map[string]any{"seatIndex": 0})
	log.Printf("游戏开始: 房间 %s", room.ID)
}

func (h *Mahjong2Handler) GetGameState(room *core.Room, seatIndex int) any {
	h.mu.RLock()
	game := h.games[room.ID]
	h.mu.RUnlock()

	if game == nil {
		return nil
	}

	return map[string]any{
		"hand":        game.GetHand(seatIndex),
		"currentTurn": game.CurrentTurn,
		"remaining":   game.TilesRemaining(),
	}
}


func (h *Mahjong2Handler) onDiscard(s *core.GameServer, userID int64, room *core.Room, data json.RawMessage) {
	var req struct {
		Tile int `json:"tile"`
	}
	json.Unmarshal(data, &req)

	h.mu.Lock()
	game := h.games[room.ID]
	if game == nil {
		h.mu.Unlock()
		return
	}

	seatIndex := room.GetSeatIndex(userID)
	if seatIndex < 0 || seatIndex != game.CurrentTurn {
		h.mu.Unlock()
		s.Sessions.RLock()
		conn := s.Sessions.GetConn(userID)
		s.Sessions.RUnlock()
		if conn != nil {
			s.Send(conn, CmdDiscard, ErrNotYourTurn, nil)
		}
		return
	}

	// 出牌
	if !game.Discard(seatIndex, req.Tile) {
		h.mu.Unlock()
		s.Sessions.RLock()
		conn := s.Sessions.GetConn(userID)
		s.Sessions.RUnlock()
		if conn != nil {
			s.Send(conn, CmdDiscard, ErrInvalidTile, nil)
		}
		return
	}
	h.mu.Unlock()

	// 广播出牌
	s.Broadcast(room, CmdDiscard, map[string]any{
		"seatIndex": seatIndex,
		"tile":      req.Tile,
	})

	// 下一个玩家摸牌
	h.nextTurn(s, room)
}

func (h *Mahjong2Handler) nextTurn(s *core.GameServer, room *core.Room) {
	h.mu.Lock()
	game := h.games[room.ID]
	if game == nil {
		h.mu.Unlock()
		return
	}

	nextSeat := game.NextTurn()

	if game.TilesRemaining() > 0 {
		tile := game.Draw(nextSeat)
		h.mu.Unlock()

		room.RLock()
		s.SendTo(room.Seats[nextSeat].UserID, core.CmdDraw, map[string]any{"tile": tile})
		room.RUnlock()
		s.Broadcast(room, core.CmdTurn, map[string]any{"seatIndex": nextSeat})
	} else {
		h.mu.Unlock()
		// 流局
		h.endGame(s, room, -1, "荒庄")
	}
}

func (h *Mahjong2Handler) endGame(s *core.GameServer, room *core.Room, winner int, reason string) {
	s.Broadcast(room, core.CmdGameOver, map[string]any{
		"winner": winner,
		"reason": reason,
	})

	// 清理游戏状态
	h.mu.Lock()
	delete(h.games, room.ID)
	h.mu.Unlock()

	// 重置房间
	room.Lock()
	room.ResetReady()
	room.Unlock()

	log.Printf("游戏结束: 房间 %s, 赢家=%d, 原因=%s", room.ID, winner, reason)
}
