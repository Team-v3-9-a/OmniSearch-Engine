.PHONY: build run clean test-env

# Сборка бинарника
build:
	go build -o bin/video-engine ./cmd/video-engine/main.go

# Очистка сгенерированных файлов
clean:
	rm -rf bin/ output_frames/ output_audio/

# Быстрая проверка, что OpenCV и CGO слинковались корректно
test-env:
	go run cmd/test-env/main.go