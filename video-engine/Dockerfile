FROM gocv/opencv:4.13.0

WORKDIR /app

RUN apt-get update && apt-get install -y --fix-missing \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*

COPY . .

RUN go mod tidy
RUN go build -o bin/video-engine ./cmd/video-engine/main.go

RUN mkdir -p /app/shared_media && chmod 777 /app/shared_media

CMD ["./bin/video-engine"]