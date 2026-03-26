FROM pytorch/pytorch:2.11.0-cuda13.0-cudnn9-runtime

# ffmpeg
RUN apt-get update && apt-get install -y --no-install-recommends \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*

# Рабочая директория
WORKDIR /app

# Путь, куда библиотеки Hugging Face и CTranslate2 будут качать веса.
ENV HF_HOME=/models
ENV MODELSCOPE_CACHE=/models

# Зависимости
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY ./app /app/app

# ASGI-сервер
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]