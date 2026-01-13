package core

import (
	"bytes"
	"encoding/json"
	"log"
	"net/http"
	"sync"
	"time"
)

// SettlementService 结算服务 - 异步调用 AccountServer，失败自动重试
type SettlementService struct {
	accountURL string
	retryQueue []map[string]any
	mu         sync.Mutex
}

func NewSettlementService(accountURL string) *SettlementService {
	s := &SettlementService{accountURL: accountURL}
	// 每5秒批量处理失败任务
	go func() {
		for {
			time.Sleep(5 * time.Second)
			s.processRetry()
		}
	}()
	return s
}

// Submit 提交结算 (立即异步执行)
func (s *SettlementService) Submit(roomID, gameType string, settlements []map[string]any) {
	task := map[string]any{"roomId": roomID, "gameType": gameType, "settlements": settlements}
	go s.doSettle(task)
}

func (s *SettlementService) doSettle(task map[string]any) {
	if s.httpPost(task) {
		log.Printf("[Settlement] 成功: %s", task["roomId"])
	} else {
		s.mu.Lock()
		s.retryQueue = append(s.retryQueue, task)
		s.mu.Unlock()
	}
}

func (s *SettlementService) processRetry() {
	s.mu.Lock()
	tasks := s.retryQueue
	s.retryQueue = nil
	s.mu.Unlock()
	
	for _, task := range tasks {
		go s.doSettle(task)
	}
}

func (s *SettlementService) httpPost(body map[string]any) bool {
	data, _ := json.Marshal(body)
	resp, err := (&http.Client{Timeout: 5 * time.Second}).Post(
		s.accountURL+"/game/settle", "application/json", bytes.NewReader(data))
	if err != nil {
		return false
	}
	defer resp.Body.Close()
	
	var result struct{ Code int `json:"code"` }
	json.NewDecoder(resp.Body).Decode(&result)
	return result.Code == 0
}
