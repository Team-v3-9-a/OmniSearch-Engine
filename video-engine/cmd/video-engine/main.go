package main

import (
	"log"
	"os"

	"github.com/joho/godotenv"

	"omnisearch/video-engine/internal/api"
)

func main() {
	err := godotenv.Load()
	if err != nil {
		log.Println("Предупреждение: ошибка загрузки .env файла, продолжаем с системными переменными")
	}

	server, err := api.NewServer()
	if err != nil {
		log.Fatalf("Ошибка инициализации сервера: %v", err)
	}

	port := os.Getenv("PORT")
	if port == "" {
		port = "8081"
	}

	if err := server.Start(":" + port); err != nil {
		log.Fatalf("Ошибка при работе сервера: %v", err)
	}
}

