package audio

import (
	"context"
	"testing"
)

func TestExtract_CancelledContext(t *testing.T) {
	ctx, cancel := context.WithCancel(context.Background())
	cancel() // Отменяем контекст сразу

	err := Extract(ctx, "nonexistent.mp4", "output.wav")
	if err == nil {
		t.Error("ожидалась ошибка при отмененном контексте, но получили nil")
	}
}
