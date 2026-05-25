FROM pytorch/pytorch:2.11.0-cuda12.8-cudnn9-runtime

# Зеркала Яндекса
RUN sed -i 's|http://archive.ubuntu.com/ubuntu/|http://mirror.yandex.ru/ubuntu/|g' /etc/apt/sources.list.d/ubuntu.sources && \
    sed -i 's|http://security.ubuntu.com/ubuntu/|http://mirror.yandex.ru/ubuntu/|g' /etc/apt/sources.list.d/ubuntu.sources

# ffmpeg + wget (нужен для healthcheck в docker-compose)
RUN apt-get update && apt-get install -y --no-install-recommends \
    ffmpeg \
    wget \
    && rm -rf /var/lib/apt/lists/*

# Рабочая директория
WORKDIR /app

# Путь, куда библиотеки Hugging Face и CTranslate2 будут качать веса.
ENV HF_HOME=/models
ENV MODELSCOPE_CACHE=/models

# Зависимости
COPY requirements.txt .
RUN pip install --default-timeout=100 --no-cache-dir -r requirements.txt --break-system-packages

COPY ./app /app/app

# ASGI-сервер
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]