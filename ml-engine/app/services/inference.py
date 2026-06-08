from faster_whisper import WhisperModel
from sentence_transformers import SentenceTransformer
import torch
import re
import os
from PIL import Image

class MLService:
    def __init__(self):
        self.device = "cuda" if torch.cuda.is_available() else "cpu"
        print(f"Loading Whisper large-v3 (INT8) on {self.device}...")
        self.whisper = WhisperModel(
            "large-v3", 
            device=self.device, 
            compute_type="int8",
            download_root="/models/whisper"
        )

        cache_folder = "/models/sentence_transformers"
        print(f"Loading E5-base on {self.device}...")
        model_kwargs = {"torch_dtype": torch.float16} if self.device == "cuda" else {}
        self.embedder = SentenceTransformer(
            "intfloat/multilingual-e5-base", 
            device=self.device,
            model_kwargs=model_kwargs,
            cache_folder=cache_folder
        )

        print(f"Loading CLIP-ViT-B-32 on {self.device}...")
        self.clip_vision = SentenceTransformer(
            'clip-ViT-B-32',
            device=self.device,
            cache_folder=cache_folder)
        
        print(f"Loading CLIP-ViT-B-32-multilingual on {self.device}...")
        self.clip_text = SentenceTransformer(
            'sentence-transformers/clip-ViT-B-32-multilingual-v1',
            device=self.device,
            cache_folder=cache_folder)

    
    # Обработка аудио в текстовые сегменты с помощью Whisper
    def process_audio_to_chunks(self, audio_path: str):
        segments, _ = self.whisper.transcribe(
            audio_path, 
            beam_size=5, 
            vad_filter=True, 
            vad_parameters=dict(min_silence_duration_ms=500)
        )
        
        chunks = []
        for segment in segments:
            text = segment.text.strip()
            if not text:
                continue
            chunks.append({
                "text": text,
                "start_time": segment.start,
                "end_time": segment.end
            })
        return chunks

    # Получение эмбеддинга текста с помощью SentenceTransformer
    def get_embedding(self, text: str, is_query: bool = False):
        prefix = "query: " if is_query else "passage: "
        formatted_text = prefix + text
        return self.embedder.encode(formatted_text, normalize_embeddings=True).tolist()
        
    def get_image_embedding(self, image_path: str):
        """Получение эмбеддинга одного изображения через CLIP Vision."""
        img = Image.open(image_path)
        return self.clip_vision.encode(img, normalize_embeddings=True).tolist()
    
    def get_image_embeddings_batch(self, image_paths: list[str]) -> list[list[float]]:
        """Получение эмбеддингов для пакета изображений через CLIP Vision."""
        images = [Image.open(p) for p in image_paths]
        embeddings = self.clip_vision.encode(images, normalize_embeddings=True, batch_size=32)
        return [emb.tolist() for emb in embeddings]
    
    def get_vision_text_embedding(self, text: str):
        """Получение текстового эмбеддинга через CLIP Text (мультиязычный)."""
        return self.clip_text.encode(text, normalize_embeddings=True).tolist()

    @staticmethod
    def parse_frame_timecodes(filename: str) -> dict | None:
        """
        Извлекает таймкоды из имени файла кадра.
        Формат: frame_{index}_{startTime}_{endTime}.jpg
        """
        basename = os.path.basename(filename)
        match = re.match(r"frame_(\d+)_([\d.]+)_([\d.]+)\.jpg", basename)
        if not match:
            return None
        return {
            "index": int(match.group(1)),
            "start_time": float(match.group(2)),
            "end_time": float(match.group(3)),
        }
