package main

import (
	"fmt"
	"net/http"
	"os"
	"path/filepath"
	"strconv"
	"strings"

	"github.com/gin-gonic/gin"
	"gorm.io/driver/mysql"
	"gorm.io/gorm"
)

var db *gorm.DB

type User struct {
	ID    int    `json:"id" gorm:"primaryKey"`
	Name  string `json:"name"`
	Email string `json:"email"`
	Age   int    `json:"age"`
}

type Post struct {
	ID      int    `json:"id" gorm:"primaryKey"`
	UserID  int    `json:"userId" gorm:"column:user_id"`
	Title   string `json:"title"`
	Content string `json:"content"`
	Views   int    `json:"views"`
	Author  string `json:"author" gorm:"-"`
}

func main() {
	port := "8184"
	if len(os.Args) > 1 {
		port = os.Args[1]
	}

	// 获取可执行文件所在目录
	execPath, _ := os.Executable()
	execDir := filepath.Dir(execPath)
	
	// 如果是 go run，使用当前目录
	if strings.Contains(execPath, "go-build") {
		execDir, _ = os.Getwd()
	}

	initDB()

	gin.SetMode(gin.ReleaseMode)
	r := gin.New()
	
	// 加载模板 - 使用绝对路径
	r.LoadHTMLGlob(filepath.Join(execDir, "templates/*.html"))

	// 静态文件 - 使用绝对路径
	r.Static("/static", filepath.Join(execDir, "static"))

	// 纯文本
	r.GET("/text", func(c *gin.Context) {
		c.String(http.StatusOK, "Hello, World!")
	})

	// JSON
	r.GET("/json", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{
			"message":   "Hello, World!",
			"framework": "Gin",
			"timestamp": 0,
		})
	})

	// 动态页面
	r.GET("/dynamic", func(c *gin.Context) {
		var users []User
		db.Order("id").Limit(10).Find(&users)
		c.HTML(http.StatusOK, "users.html", gin.H{
			"framework": "Gin",
			"users":     users,
		})
	})

	// 用户列表
	r.GET("/users", func(c *gin.Context) {
		page, _ := strconv.Atoi(c.DefaultQuery("page", "1"))
		size, _ := strconv.Atoi(c.DefaultQuery("size", "50"))
		if size > 100 {
			size = 100
		}
		offset := (page - 1) * size

		var users []User
		db.Order("id").Limit(size).Offset(offset).Find(&users)
		c.JSON(http.StatusOK, gin.H{
			"data": users,
			"page": page,
			"size": size,
		})
	})

	// 文章列表 (JOIN)
	r.GET("/posts", func(c *gin.Context) {
		page, _ := strconv.Atoi(c.DefaultQuery("page", "1"))
		size, _ := strconv.Atoi(c.DefaultQuery("size", "50"))
		if size > 100 {
			size = 100
		}
		offset := (page - 1) * size

		var posts []Post
		db.Table("posts p").
			Select("p.*, u.name as author").
			Joins("LEFT JOIN users u ON p.user_id = u.id").
			Order("p.id").
			Limit(size).
			Offset(offset).
			Scan(&posts)

		c.JSON(http.StatusOK, gin.H{
			"data": posts,
			"page": page,
			"size": size,
		})
	})

	fmt.Printf("Gin started on port %s\n", port)
	r.Run(":" + port)
}

func initDB() {
	dsn := "root:123456@tcp(127.0.0.1:3306)/benchmark?charset=utf8mb4&parseTime=True&loc=Local"
	var err error
	db, err = gorm.Open(mysql.Open(dsn), &gorm.Config{})
	if err != nil {
		panic("failed to connect database: " + err.Error())
	}

	sqlDB, _ := db.DB()
	sqlDB.SetMaxOpenConns(50)
	sqlDB.SetMaxIdleConns(10)

	fmt.Println("Gin: Database connected (GORM)")
}
