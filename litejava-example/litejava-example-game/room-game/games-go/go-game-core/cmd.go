package core

// 协议命令号 (与 Java Cmd.java 一致)
const (
	CmdLogin     = 1
	CmdPing      = 4
	CmdKick      = 5
	CmdRoomExit  = 106
	CmdChatMsg   = 151
	CmdUserJoin  = 500
	CmdUserExit  = 501
	CmdUserState = 502
	CmdUserReady = 503
	CmdReady     = 504
	CmdGameStart = 510
	CmdGameOver  = 511
	CmdGameState = 512
	CmdDeal      = 520
	CmdDraw      = 521
	CmdTurn      = 522
)

// 错误码
const (
	ErrOK           = 0
	ErrUnknown      = 1
	ErrNotLogin     = 2
	ErrNotInRoom    = 3
	ErrInvalidToken = 7
	ErrRoomNotFound = 20
	ErrRoomFull     = 21
	ErrNotYourTurn  = 30
	ErrInvalidAction = 31
)
