package core

import (
	"log"
	"os"

	"gopkg.in/yaml.v3"
)

type Config struct {
	Server  ServerConfig  `yaml:"server"`
	Game    GameConfig    `yaml:"game"`
	Hall    HallConfig    `yaml:"hall"`
	Account AccountConfig `yaml:"account"`
}

type ServerConfig struct {
	ID       string `yaml:"id"`
	WsPort   int    `yaml:"wsPort"`
	HttpPort int    `yaml:"httpPort"`
	ClientIP string `yaml:"clientip"`
}

type GameConfig struct {
	Type       string `yaml:"type"`
	MaxPlayers int    `yaml:"maxPlayers"`
}

type HallConfig struct {
	URL    string `yaml:"url"`
	PriKey string `yaml:"priKey"`
}

type AccountConfig struct {
	URL string `yaml:"url"`
}

func LoadConfig(path string) *Config {
	data, err := os.ReadFile(path)
	if err != nil {
		log.Fatal("读取配置失败:", err)
	}

	var cfg Config
	if err := yaml.Unmarshal(data, &cfg); err != nil {
		log.Fatal("解析配置失败:", err)
	}
	
	// 默认值
	if cfg.Account.URL == "" {
		cfg.Account.URL = "http://localhost:8101"
	}
	
	return &cfg
}
