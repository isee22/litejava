module game-mahjong2-go

go 1.21

require game-core-go v0.0.0

require (
	github.com/gorilla/websocket v1.5.3 // indirect
	gopkg.in/yaml.v3 v3.0.1 // indirect
)

replace game-core-go => ../game-core-go
