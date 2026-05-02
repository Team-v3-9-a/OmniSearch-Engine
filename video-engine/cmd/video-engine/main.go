package main

import (
	"bytes"
	"encoding/json"
	"flag"
	"fmt"
	"log"
	"net/http"
	"os"
	"path/filepath"

	// Импорт оркестратора
	"omnisearch/video-engine/internal/pipeline"
)

func main() {
	inputPtr := flag.String("input", "", "Абсолютный путь к исходному видеофайлу")
	outAudioPtr := flag.String("out-audio", "", "Абсолютный путь для сохранения аудио")
	outFramesPtr := flag.String("out-frames", "", "Абсолютный путь к директории кадров")
	apiURLPtr := flag.String("api-url", "", "URL API бэкенда")
	videoIDPtr := flag.String("video-id", "", "ID видео")

	flag.Parse()

	if *inputPtr == "" || *outAudioPtr == "" || *outFramesPtr == "" {
		flag.Usage()
		log.Fatalf("Ошибка: не переданы обязательные аргументы.")
	}

	inputPath := *inputPtr
	outAudioPath := *outAudioPtr
	outFramesDir := *outFramesPtr
	apiURL := *apiURLPtr
	videoID := *videoIDPtr

	// Проверка входного файла
	if info, err := os.Stat(inputPath); err != nil || info.IsDir() {
		log.Fatalf("Ошибка: Входной файл не найден: %s", inputPath)
	}

	// создание директорий
	if err := os.MkdirAll(outFramesDir, 0755); err != nil {
		log.Fatalf("Ошибка создания директории кадров: %v", err)
	}
	if err := os.MkdirAll(filepath.Dir(outAudioPath), 0755); err != nil {
		log.Fatalf("Ошибка создания директории аудио: %v", err)
	}

	fmt.Println("=== Инициализация OmniSearch Video Engine ===")
	fmt.Printf("[ВХОД] Видео: %s\n", inputPath)
	fmt.Printf("[ВЫХОД] Аудио: %s\n", outAudioPath)
	fmt.Printf("[ВЫХОД] Кадры: %s\n", outFramesDir)
	if apiURL != "" && videoID != "" {
		fmt.Printf("[CALLBACK] API: %s, VideoID: %s\n", apiURL, videoID)
	}
	fmt.Println("=============================================")

	// параллельный процессинг
	duration, err := pipeline.Process(inputPath, outAudioPath, outFramesDir)
	if err != nil {
		if apiURL != "" && videoID != "" {
			sendCallback(apiURL, videoID, "ERROR", 0)
		}
		log.Fatalf("Критическая ошибка пайплайна: %v", err)
	}

	fmt.Println("=== Обработка успешно завершена! ===")

	if apiURL != "" && videoID != "" {
		sendCallback(apiURL, videoID, "PROCESSING_ML", duration)
	}
}

func sendCallback(apiURL, videoID, status string, duration float64) {
	url := fmt.Sprintf("%s/api/v1/internal/videos/%s", apiURL, videoID)

	payload := map[string]interface{}{
		"status": status,
	}
	if duration > 0 {
		payload["durationSeconds"] = int(duration)
	}

	data, err := json.Marshal(payload)
	if err != nil {
		log.Printf("Ошибка формирования JSON для callback: %v", err)
		return
	}

	req, err := http.NewRequest(http.MethodPatch, url, bytes.NewBuffer(data))
	if err != nil {
		log.Printf("Ошибка создания HTTP запроса: %v", err)
		return
	}
	req.Header.Set("Content-Type", "application/json")

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		log.Printf("Ошибка отправки callback: %v", err)
		return
	}
	defer resp.Body.Close()

	if resp.StatusCode >= 200 && resp.StatusCode < 300 {
		fmt.Println("[Callback] Статус успешно обновлен!")
	} else {
		log.Printf("[Callback] Неожиданный статус от сервера: %d", resp.StatusCode)
	}
}
