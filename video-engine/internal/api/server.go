package api

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"regexp"
	"time"

	"omnisearch/video-engine/internal/ml"
	"omnisearch/video-engine/internal/pipeline"
	"omnisearch/video-engine/internal/s3"
)

var safeVideoIDRegex = regexp.MustCompile(`^[a-zA-Z0-9_\-]+$`)

func isValidVideoID(id string) bool {
	return safeVideoIDRegex.MatchString(id)
}

type Server struct {
	s3Client *s3.Client
	mlClient *ml.Client
	mux      *http.ServeMux
	httpSrv  *http.Server
}

func NewServer() (*Server, error) {
	s3Client, err := s3.NewClient()
	if err != nil {
		return nil, err
	}

	mux := http.NewServeMux()
	s := &Server{
		s3Client: s3Client,
		mlClient: ml.NewClient(),
		mux:      mux,
	}

	mux.HandleFunc("GET /health", s.handleHealth)
	mux.HandleFunc("POST /process", s.handleProcess)

	return s, nil
}

func (s *Server) Start(ctx context.Context, port string) error {
	log.Printf("Запуск сервера на порту %s", port)
	s.httpSrv = &http.Server{
		Addr:    port,
		Handler: s.mux,
	}

	go func() {
		<-ctx.Done()
		log.Println("Получен сигнал завершения работы, останавливаем HTTP сервер...")

		shutdownCtx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
		defer cancel()

		if err := s.httpSrv.Shutdown(shutdownCtx); err != nil {
			log.Printf("Ошибка при graceful shutdown: %v", err)
		}
	}()

	if err := s.httpSrv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
		return err
	}
	return nil
}

func (s *Server) handleHealth(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusOK)
	w.Write([]byte("ok"))
}

type ProcessRequest struct {
	VideoID string `json:"video_id"`
	S3Path  string `json:"s3_path"`
}

func (s *Server) handleProcess(w http.ResponseWriter, r *http.Request) {
	var req ProcessRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	if req.VideoID == "" || req.S3Path == "" {
		http.Error(w, "video_id and s3_path are required", http.StatusBadRequest)
		return
	}

	if !isValidVideoID(req.VideoID) {
		http.Error(w, "invalid video_id format", http.StatusBadRequest)
		return
	}

	// Отвечаем 202 Accepted и запускаем асинхронную обработку
	w.WriteHeader(http.StatusAccepted)
	w.Write([]byte(`{"message": "Processing started"}`))

	go s.processVideo(req.VideoID, req.S3Path)
}

func (s *Server) processVideo(videoID, s3Path string) {
	log.Printf("Начало обработки видео %s", videoID)

	baseDir := os.Getenv("SHARED_MEDIA_PATH")
	if baseDir == "" {
		baseDir = "/app/shared_media"
	}

	workDir := filepath.Join(baseDir, videoID)
	os.MkdirAll(workDir, 0755)
	
	// В случае успеха удаляем локальные файлы, можно и при ошибке
	defer os.RemoveAll(workDir)

	localVideoPath := filepath.Join(workDir, "source.mp4")
	outAudioPath := filepath.Join(workDir, "audio.wav")
	outFramesDir := filepath.Join(workDir, "frames")

	os.MkdirAll(outFramesDir, 0755)

	ctx := context.Background()

	// 1. Скачиваем видео из S3
	err := s.s3Client.DownloadVideo(ctx, s3Path, localVideoPath)
	if err != nil {
		log.Printf("Ошибка скачивания видео %s: %v", videoID, err)
		s.sendCallback(videoID, "ERROR", 0, "")
		return
	}

	// 2. Локальный процессинг (audio + frames)
	duration, err := pipeline.Process(localVideoPath, outAudioPath, outFramesDir)
	if err != nil {
		log.Printf("Ошибка обработки видео %s: %v", videoID, err)
		s.sendCallback(videoID, "ERROR", 0, "")
		return
	}

	// 3. Загружаем результаты обратно в S3
	thumbnailPath, err := s.s3Client.UploadMedia(ctx, videoID, outAudioPath, outFramesDir)
	if err != nil {
		log.Printf("Ошибка загрузки результатов видео %s: %v", videoID, err)
		s.sendCallback(videoID, "ERROR", 0, "")
		return
	}

	// 4. Отправляем callback в Backend о начале ML-процессинга
	log.Printf("Успешное завершение нарезки видео %s. Отправка callback...", videoID)
	s.sendCallback(videoID, "PROCESSING_ML", duration, thumbnailPath)

	// 5. Запускаем ML Engine (транскрибация и векторизация)
	audioObjectKey := fmt.Sprintf("media/%s/audio.wav", videoID)
	err = s.mlClient.TriggerProcess(ctx, videoID, audioObjectKey)
	if err != nil {
		log.Printf("Критическая ошибка: не удалось запустить ML Engine для видео %s: %v", videoID, err)
		s.sendCallback(videoID, "ERROR", 0, "")
		return
	}
}

func (s *Server) sendCallback(videoID, status string, duration float64, thumbnailPath string) {
	// Для вызова бекенда мы берем URL бекенда. Т.к. video-engine запускается в docker, backend это http://backend:8080
	// Для тестов можно через env.
	apiURL := os.Getenv("BACKEND_API_URL")
	if apiURL == "" {
		apiURL = "http://backend:8080"
	}
	
	url := fmt.Sprintf("%s/api/v1/internal/videos/%s", apiURL, videoID)

	payload := map[string]interface{}{
		"status": status,
	}
	if duration > 0 {
		payload["durationSeconds"] = int(duration)
	}
	if thumbnailPath != "" {
		payload["thumbnailPath"] = thumbnailPath
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
	apiSecret := os.Getenv("INTERNAL_API_SECRET")
	if apiSecret != "" {
		req.Header.Set("X-Internal-Secret", apiSecret)
	}

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		log.Printf("Ошибка отправки callback: %v", err)
		return
	}
	defer resp.Body.Close()

	if resp.StatusCode >= 200 && resp.StatusCode < 300 {
		log.Printf("[Callback] Статус %s для видео %s успешно обновлен!", status, videoID)
	} else {
		log.Printf("[Callback] Неожиданный статус от сервера: %d", resp.StatusCode)
	}
}
