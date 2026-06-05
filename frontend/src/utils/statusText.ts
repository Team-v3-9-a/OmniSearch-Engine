import type { UploadStatus } from "@/types/api";

export const getStatusText = (status: UploadStatus) => {
  switch (status) {
    case 'UPLOADING': return 'Отправка';
    case 'UPLOADED': return 'Загружено';
    case 'PROCESSING_MEDIA': return 'Обработка видео';
    case 'PROCESSING_ML': return 'ML анализ';
    case 'READY': return 'Готово';
    case 'ERROR': return 'Ошибка';
    default: return '';
  }
};