package main

import (
	"math/rand"
	"time"
)

// Mahjong2Game 二人麻将游戏逻辑
type Mahjong2Game struct {
	Tiles       []int   // 牌堆
	Hands       [][]int // 各玩家手牌
	Discards    [][]int // 各玩家弃牌
	CurrentTurn int     // 当前轮到谁
	LastDraw    int     // 最后摸的牌
	LastDiscard int     // 最后打出的牌
}

// NewMahjong2Game 创建新游戏
func NewMahjong2Game() *Mahjong2Game {
	return &Mahjong2Game{
		Hands:    make([][]int, 2),
		Discards: make([][]int, 2),
	}
}

// Init 初始化牌堆并洗牌
func (g *Mahjong2Game) Init() {
	// 简化: 只用万子 1-9，每种4张 = 36张
	g.Tiles = make([]int, 0, 36)
	for i := 1; i <= 9; i++ {
		for j := 0; j < 4; j++ {
			g.Tiles = append(g.Tiles, i)
		}
	}
	g.shuffle()
}

func (g *Mahjong2Game) shuffle() {
	r := rand.New(rand.NewSource(time.Now().UnixNano()))
	for i := len(g.Tiles) - 1; i > 0; i-- {
		j := r.Intn(i + 1)
		g.Tiles[i], g.Tiles[j] = g.Tiles[j], g.Tiles[i]
	}
}

// Deal 发牌 (每人13张)
func (g *Mahjong2Game) Deal() {
	for i := 0; i < 2; i++ {
		g.Hands[i] = make([]int, 13)
		copy(g.Hands[i], g.Tiles[:13])
		g.Tiles = g.Tiles[13:]
		g.Discards[i] = []int{}
	}
}

// Draw 摸牌
func (g *Mahjong2Game) Draw(seatIndex int) int {
	if len(g.Tiles) == 0 {
		return -1
	}
	tile := g.Tiles[0]
	g.Tiles = g.Tiles[1:]
	g.Hands[seatIndex] = append(g.Hands[seatIndex], tile)
	g.LastDraw = tile
	return tile
}

// Discard 出牌
func (g *Mahjong2Game) Discard(seatIndex int, tile int) bool {
	hand := g.Hands[seatIndex]
	for i, t := range hand {
		if t == tile {
			g.Hands[seatIndex] = append(hand[:i], hand[i+1:]...)
			g.Discards[seatIndex] = append(g.Discards[seatIndex], tile)
			g.LastDiscard = tile
			return true
		}
	}
	return false
}

// HasTile 检查手牌中是否有指定牌
func (g *Mahjong2Game) HasTile(seatIndex int, tile int) bool {
	for _, t := range g.Hands[seatIndex] {
		if t == tile {
			return true
		}
	}
	return false
}

// TilesRemaining 剩余牌数
func (g *Mahjong2Game) TilesRemaining() int {
	return len(g.Tiles)
}

// GetHand 获取玩家手牌
func (g *Mahjong2Game) GetHand(seatIndex int) []int {
	return g.Hands[seatIndex]
}

// NextTurn 下一个玩家
func (g *Mahjong2Game) NextTurn() int {
	g.CurrentTurn = (g.CurrentTurn + 1) % 2
	return g.CurrentTurn
}
