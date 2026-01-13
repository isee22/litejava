package main

import (
	"game-core-go"
)

func main() {
	cfg := core.LoadConfig("config.yml")
	handler := &Mahjong2Handler{}
	server := core.NewGameServer(cfg, handler)
	server.Start()
}
