package core

import (
	"sync"
	"time"
)

type Seat struct {
	SeatIndex int    `json:"seatIndex"`
	UserID    int64  `json:"userId"`
	Name      string `json:"name"`
	Ready     bool   `json:"ready"`
	Online    bool   `json:"online"`
}

type Room struct {
	ID         string
	OwnerID    int64
	Seats      []*Seat
	CreateTime time.Time
	mu         sync.RWMutex
}

func NewRoom(id string, ownerID int64, maxPlayers int) *Room {
	room := &Room{
		ID:         id,
		OwnerID:    ownerID,
		Seats:      make([]*Seat, maxPlayers),
		CreateTime: time.Now(),
	}
	for i := range room.Seats {
		room.Seats[i] = &Seat{SeatIndex: i}
	}
	return room
}

func (r *Room) Lock()    { r.mu.Lock() }
func (r *Room) Unlock()  { r.mu.Unlock() }
func (r *Room) RLock()   { r.mu.RLock() }
func (r *Room) RUnlock() { r.mu.RUnlock() }

func (r *Room) FindSeat(userID int64) *Seat {
	for _, seat := range r.Seats {
		if seat.UserID == userID {
			return seat
		}
	}
	return nil
}

func (r *Room) FindEmptySeat() *Seat {
	for _, seat := range r.Seats {
		if seat.UserID == 0 {
			return seat
		}
	}
	return nil
}

func (r *Room) GetSeatIndex(userID int64) int {
	for i, seat := range r.Seats {
		if seat.UserID == userID {
			return i
		}
	}
	return -1
}

func (r *Room) PlayerCount() int {
	count := 0
	for _, seat := range r.Seats {
		if seat.UserID > 0 {
			count++
		}
	}
	return count
}

func (r *Room) IsAllReady(maxPlayers int) bool {
	count := 0
	for _, seat := range r.Seats {
		if seat.UserID > 0 {
			if !seat.Ready {
				return false
			}
			count++
		}
	}
	return count == maxPlayers
}

func (r *Room) ResetReady() {
	for _, seat := range r.Seats {
		seat.Ready = false
	}
}

func (r *Room) GetOnlineUsers() []int64 {
	var users []int64
	for _, seat := range r.Seats {
		if seat.UserID > 0 && seat.Online {
			users = append(users, seat.UserID)
		}
	}
	return users
}
