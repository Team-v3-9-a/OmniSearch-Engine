from faster_whisper import WhisperModel
from sentence_transformers import SentenceTransformer
import torch

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