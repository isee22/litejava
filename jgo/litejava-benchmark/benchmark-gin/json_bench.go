package main

import (
	"encoding/json"
	"fmt"
	"time"
)

const iterations = 100000

func main() {
	// 简单数据
	simpleData := map[string]interface{}{
		"message":   "Hello, World!",
		"framework": "Gin",
		"timestamp": time.Now().UnixMilli(),
	}

	// 复杂数据
	complexData := createComplexData()

	fmt.Println("=== Go encoding/json Benchmark ===\n")

	// Warmup
	fmt.Println("Warming up...")
	for i := 0; i < 10000; i++ {
		json.Marshal(simpleData)
	}

	// Simple serialize
	fmt.Println("\n--- Simple Data (3 fields) ---")
	start := time.Now()
	for i := 0; i < iterations; i++ {
		json.Marshal(simpleData)
	}
	elapsed := time.Since(start)
	fmt.Printf("  Go json.Marshal: %.2f ms, %.0f ops/sec\n", 
		float64(elapsed.Milliseconds()), float64(iterations)/elapsed.Seconds())

	// Complex serialize
	fmt.Println("\n--- Complex Data (nested, 50 items) ---")
	start = time.Now()
	for i := 0; i < iterations; i++ {
		json.Marshal(complexData)
	}
	elapsed = time.Since(start)
	fmt.Printf("  Go json.Marshal: %.2f ms, %.0f ops/sec\n",
		float64(elapsed.Milliseconds()), float64(iterations)/elapsed.Seconds())

	// Deserialize
	simpleJson, _ := json.Marshal(simpleData)
	complexJson, _ := json.Marshal(complexData)

	fmt.Println("\n--- Deserialization (Simple) ---")
	start = time.Now()
	for i := 0; i < iterations; i++ {
		var m map[string]interface{}
		json.Unmarshal(simpleJson, &m)
	}
	elapsed = time.Since(start)
	fmt.Printf("  Go json.Unmarshal: %.2f ms, %.0f ops/sec\n",
		float64(elapsed.Milliseconds()), float64(iterations)/elapsed.Seconds())

	fmt.Println("\n--- Deserialization (Complex) ---")
	start = time.Now()
	for i := 0; i < iterations; i++ {
		var m map[string]interface{}
		json.Unmarshal(complexJson, &m)
	}
	elapsed = time.Since(start)
	fmt.Printf("  Go json.Unmarshal: %.2f ms, %.0f ops/sec\n",
		float64(elapsed.Milliseconds()), float64(iterations)/elapsed.Seconds())
}

func createComplexData() map[string]interface{} {
	users := make([]map[string]interface{}, 50)
	for i := 0; i < 50; i++ {
		users[i] = map[string]interface{}{
			"id":     i + 1,
			"name":   fmt.Sprintf("User%d", i+1),
			"email":  fmt.Sprintf("user%d@example.com", i+1),
			"age":    20 + (i % 30),
			"active": i%2 == 0,
		}
	}
	return map[string]interface{}{
		"page": 1,
		"size": 50,
		"data": users,
	}
}
