package main

import (
	"flag"
	"fmt"
	"log"
	"os"
	"path/filepath"

	// Импорт оркестратора
	"omnisearch/video-engine/internal/pipeline"
)

func main() {
	inputPtr := flag.String("input", "", "Абсолютный путь к исходному видеофайлу")
	outAudioPtr := flag.String("out-audio", "", "Абсолютный путь для сохранения аудио")
	outFramesPtr := flag.String("out-frames", "", "Абсолютный путь к директории кадров")

	flag.Parse()

	if *inputPtr == "" || *outAudioPtr == "" || *outFramesPtr == "" {
		flag.Usage()
		log.Fatalf("Ошибка: не переданы обязательные аргументы.")
	}

	inputPath := *inputPtr
	outAudioPath := *outAudioPtr
	outFramesDir := *outFramesPtr

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
	fmt.Println("=============================================")

	// параллельный процессинг
	err := pipeline.Process(inputPath, outAudioPath, outFramesDir)
	if err != nil {
		log.Fatalf("Критическая ошибка пайплайна: %v", err)
	}

	fmt.Println("=== Обработка успешно завершена! ===")
}
