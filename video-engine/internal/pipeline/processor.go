package pipeline

import (
	"fmt"
	"sync"
	
	"omnisearch/video-engine/internal/audio"
	"omnisearch/video-engine/internal/video"
)

// Process запускает воркеры параллельно и дожидается их выполнения
func Process(inputPath, outAudioPath, outFramesDir string) (float64, error) {
	var wg sync.WaitGroup
	wg.Add(2)

	var audioErr, videoErr error
	var duration float64

	// 1. Горутина для аудио
	go func() {
		defer wg.Done()
		fmt.Println("[Audio Worker] Начинаю извлечение аудио...")
		audioErr = audio.Extract(inputPath, outAudioPath)
		if audioErr == nil {
			fmt.Println("[Audio Worker] Успешно завершено!")
		}
	}()

	// 2. Горутина для видео
	go func() {
		defer wg.Done()
		fmt.Println("[Video Worker] Начинаю нарезку кадров (1 FPS)...")
		duration, videoErr = video.SampleFrames(inputPath, outFramesDir, 1)
		if videoErr == nil {
			fmt.Println("[Video Worker] Успешно завершено! Длительность:", duration)
		}
	}()

	// Блокируем выполнение, пока обе горутины не вызовут wg.Done()
	wg.Wait()

	// Возвращаем ошибки, если они были
	if audioErr != nil {
		return 0, fmt.Errorf("ошибка аудио: %v", audioErr)
	}
	if videoErr != nil {
		return 0, fmt.Errorf("ошибка видео: %v", videoErr)
	}

	return duration, nil
}