FROM golang:1.21-bullseye

WORKDIR /app

RUN apt-get update && apt-get install -y \
    gcc \
    g++ \
    pkg-config \
    ffmpeg \
    libopencv-dev \
    && rm -rf /var/lib/apt/lists/*


RUN mkdir -p /app/shared_media && chmod 777 /app/shared_media

COPY . .

# Создаём маркер-файл для healthcheck и держим контейнер живым
CMD ["sh", "-c", "touch /app/shared_media/.keep && tail -f /dev/null"]