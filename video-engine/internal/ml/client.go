package ml

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"os"
	"strings"
	"time"
)

type Client struct {
	baseURL    string
	apiSecret  string
	httpClient *http.Client
}

func NewClient() *Client {
	baseURL := os.Getenv("ML_ENGINE_API_URL")
	if baseURL == "" {
		baseURL = "http://ml-engine:8000"
	}
	baseURL = strings.TrimRight(baseURL, "/")

	return &Client{
		baseURL:   baseURL,
		apiSecret: os.Getenv("INTERNAL_API_SECRET"),
		httpClient: &http.Client{
			Timeout: 10 * time.Second,
		},
	}
}

type ProcessRequest struct {
	VideoID   string `json:"video_id"`
	AudioPath string `json:"audio_path"`
}

// TriggerProcess отправляет запрос на транскрибацию и векторизацию аудиофайла с механизмом retry.
func (c *Client) TriggerProcess(ctx context.Context, videoID, audioPath string) error {
	url := fmt.Sprintf("%s/api/v1/process", c.baseURL)
	payload := ProcessRequest{
		VideoID:   videoID,
		AudioPath: audioPath,
	}

	data, err := json.Marshal(payload)
	if err != nil {
		return fmt.Errorf("ошибка сериализации JSON для ML Engine: %v", err)
	}

	maxRetries := 3
	baseDelay := 2 * time.Second

	for attempt := 1; attempt <= maxRetries; attempt++ {
		log.Printf("[ML Client] Попытка %d/%d вызова ML Engine (%s) для видео %s...", attempt, maxRetries, url, videoID)

		req, err := http.NewRequestWithContext(ctx, http.MethodPost, url, bytes.NewBuffer(data))
		if err != nil {
			return fmt.Errorf("ошибка создания HTTP запроса к ML Engine: %v", err)
		}

		req.Header.Set("Content-Type", "application/json")
		if c.apiSecret != "" {
			req.Header.Set("X-Internal-Secret", c.apiSecret)
		}

		resp, err := c.httpClient.Do(req)
		if err == nil {
			defer resp.Body.Close()
			if resp.StatusCode == http.StatusAccepted {
				log.Printf("[ML Client] Успешный запуск ML-процессинга для видео %s (202 Accepted)", videoID)
				return nil
			}
			log.Printf("[ML Client] Предупреждение: ML Engine ответил статусом %d (ожидалось 202)", resp.StatusCode)
			// Если статус 4xx (кроме 429), обычно нет смысла ретраить
			if resp.StatusCode >= 400 && resp.StatusCode < 500 && resp.StatusCode != http.StatusTooManyRequests {
				return fmt.Errorf("некорректный запрос к ML Engine: код %d", resp.StatusCode)
			}
		} else {
			log.Printf("[ML Client] Ошибка сети при вызове ML Engine: %v", err)
		}

		if attempt == maxRetries {
			return fmt.Errorf("ML Engine недоступен после %d попыток", maxRetries)
		}

		log.Printf("[ML Client] Ожидание %v перед повторной попыткой...", baseDelay)
		select {
		case <-ctx.Done():
			return ctx.Err()
		case <-time.After(baseDelay):
		}

		baseDelay *= 2 // Exponential backoff
	}

	return nil
}
