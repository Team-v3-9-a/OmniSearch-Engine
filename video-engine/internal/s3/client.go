package s3

import (
	"context"
	"fmt"
	"log"
	"os"
	"path/filepath"
	"strings"

	"github.com/minio/minio-go/v7"
	"github.com/minio/minio-go/v7/pkg/credentials"
)

type Client struct {
	minioClient *minio.Client
	bucket      string
}

func NewClient() (*Client, error) {
	endpoint := os.Getenv("S3_ENDPOINT")
	if endpoint == "" {
		endpoint = "localhost:9000"
	}
	// Убираем http:// или https:// из endpoint
	endpoint = strings.TrimPrefix(endpoint, "http://")
	endpoint = strings.TrimPrefix(endpoint, "https://")

	accessKey := os.Getenv("S3_ACCESS_KEY")
	secretKey := os.Getenv("S3_SECRET_KEY")
	useSSL := false // Обычно локально MinIO работает без SSL

	minioClient, err := minio.New(endpoint, &minio.Options{
		Creds:  credentials.NewStaticV4(accessKey, secretKey, ""),
		Secure: useSSL,
	})
	if err != nil {
		return nil, fmt.Errorf("ошибка инициализации S3 клиента: %v", err)
	}

	return &Client{
		minioClient: minioClient,
		bucket:      "videos",
	}, nil
}

// DownloadVideo скачивает видео из S3 в локальную директорию
func (c *Client) DownloadVideo(ctx context.Context, s3Path, localPath string) error {
	log.Printf("[S3] Скачивание %s в %s...", s3Path, localPath)
	err := c.minioClient.FGetObject(ctx, c.bucket, s3Path, localPath, minio.GetObjectOptions{})
	if err != nil {
		return fmt.Errorf("не удалось скачать файл %s: %v", s3Path, err)
	}
	return nil
}

func (c *Client) UploadMedia(ctx context.Context, videoID, localAudioPath, localFramesDir string) (string, error) {
	log.Printf("[S3] Загрузка аудио %s...", localAudioPath)
	audioS3Path := fmt.Sprintf("media/%s/audio.wav", videoID)
	_, err := c.minioClient.FPutObject(ctx, c.bucket, audioS3Path, localAudioPath, minio.PutObjectOptions{ContentType: "audio/wav"})
	if err != nil {
		return "", fmt.Errorf("ошибка загрузки аудио: %v", err)
	}

	log.Printf("[S3] Загрузка кадров из %s...", localFramesDir)
	files, err := os.ReadDir(localFramesDir)
	if err != nil {
		return "", fmt.Errorf("не удалось прочитать директорию кадров: %v", err)
	}

	var thumbnailPath string
	thumbnailUploaded := false

	for _, file := range files {
		if file.IsDir() {
			continue
		}
		localFilePath := filepath.Join(localFramesDir, file.Name())
		s3FilePath := fmt.Sprintf("media/%s/frames/%s", videoID, file.Name())
		
		_, err := c.minioClient.FPutObject(ctx, c.bucket, s3FilePath, localFilePath, minio.PutObjectOptions{ContentType: "image/jpeg"})
		if err != nil {
			log.Printf("[S3] Предупреждение: ошибка загрузки кадра %s: %v", file.Name(), err)
		}

		// Загружаем первый кадр как thumbnail
		if !thumbnailUploaded && strings.HasSuffix(file.Name(), ".jpg") {
			thumbS3Path := fmt.Sprintf("media/%s/thumbnail.jpg", videoID)
			_, err := c.minioClient.FPutObject(ctx, c.bucket, thumbS3Path, localFilePath, minio.PutObjectOptions{ContentType: "image/jpeg"})
			if err != nil {
				log.Printf("[S3] Предупреждение: ошибка загрузки thumbnail %s: %v", file.Name(), err)
			} else {
				thumbnailPath = thumbS3Path
				thumbnailUploaded = true
			}
		}
	}

	return thumbnailPath, nil
}
