package video

import (
	"fmt"
	"path/filepath"

	"gocv.io/x/gocv"
)

// SampleFrames извлекает кадры с заданной частотой (targetFPS).
func SampleFrames(inputPath, outputDir string, targetFPS int) (float64, error) {
	video, err := gocv.VideoCaptureFile(inputPath)
	if err != nil {
		return 0, fmt.Errorf("не удалось открыть видео %s: %v", inputPath, err)
	}
	defer video.Close() // очистка C++ памяти

	originalFPS := video.Get(gocv.VideoCaptureFPS)
	if originalFPS <= 0 {
		return 0, fmt.Errorf("не удалось определить FPS видео")
	}

	totalFrames := video.Get(gocv.VideoCaptureFrameCount)
	duration := totalFrames / originalFPS

	frameSkip := int(originalFPS) / targetFPS
	if frameSkip == 0 {
		frameSkip = 1
	}

	img := gocv.NewMat()
	defer img.Close()

	frameCount, savedCount := 0, 0

	for {
		if ok := video.Read(&img); !ok || img.Empty() {
			break
		}

		if frameCount%frameSkip == 0 {
			fileName := fmt.Sprintf("frame_%04d.jpg", savedCount)
			outPath := filepath.Join(outputDir, fileName)

			if success := gocv.IMWrite(outPath, img); !success {
				return 0, fmt.Errorf("не удалось сохранить кадр: %s", outPath)
			}
			savedCount++
		}
		frameCount++
	}

	return duration, nil
}
