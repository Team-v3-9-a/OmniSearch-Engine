package main

import (
	"context"
	"log"
	"os"
	"os/signal"
	"syscall"

	"github.com/joho/godotenv"

	"omnisearch/video-engine/internal/api"
)

func main() {
	err := godotenv.Load()
	if err != nil {
		log.Println("Предупреждение: ошибка загрузки .env файла, продолжаем с системными переменными")
	}

	// Создаем контекст, который отменяется при получении сигналов SIGINT или SIGTERM
	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
	defer stop()

	server, err := api.NewServer()
	if err != nil {
		log.Fatalf("Ошибка инициализации сервера: %v", err)
	}

	port := os.Getenv("PORT")
	if port == "" {
		port = "8081"
	}

	if err := server.Start(ctx, ":" + port); err != nil {
		log.Fatalf("Ошибка при работе сервера: %v", err)
	}

	log.Println("Сервер успешно завершил работу")
}

