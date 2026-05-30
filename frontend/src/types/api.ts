export interface UploadResponse{
    id: string;
    message: string;
}

export interface SearchResultItem {
    video_id: string,
    title: string,
    thumbnail_url: string,
    score: number,
    duration?: number,
    created_date?: number
    upload_date?: string,
    segments: {
        text_snippet: string,
        start_time: number,
        end_time: number
    }[]
}

export interface VideoDetails {
    streamUrl: string;
}

export interface MyVideoItem {
  id: string;
  title: string;
  status: UploadStatus;
  createdAt: string;
  updatedAt: string;
}

export type UploadStatus =
    | 'UPLOADING'
    | 'UPLOADED'
    | 'PROCESSING_MEDIA'
    | 'PROCESSING_ML'
    | 'READY'
    | 'ERROR'

export interface UploadTask {
  localId: string;
  filename: string;
  progress: number;
  status: UploadStatus;
  backendId?: string;
}