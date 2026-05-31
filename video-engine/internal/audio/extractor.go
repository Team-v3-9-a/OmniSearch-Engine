package audio

import (
	"bytes"
	"context"
	"fmt"
	"os/exec"
)

// Extract извлекает аудиодорожку из видео и сохраняет в WAV (16kHz, Mono).
func Extract(ctx context.Context, inputPath, outputPath string) error {
	cmd := exec.CommandContext(ctx, "ffmpeg",
		"-y", "-i", inputPath,
		"-vn", "-acodec", "pcm_s16le",
		"-ar", "16000", "-ac", "1",
		outputPath,
	)

	var stderr bytes.Buffer
	cmd.Stderr = &stderr

	if err := cmd.Run(); err != nil {
		return fmt.Errorf("ошибка FFmpeg: %v\nЛоги: %s", err, stderr.String())
	}
	return nil
}
