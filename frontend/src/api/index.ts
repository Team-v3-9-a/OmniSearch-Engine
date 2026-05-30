import {apiClient} from "@/api/client.ts";
import type { MyVideoItem, SearchResultItem, VideoDetails } from "@/types/api.ts";

export const uploadVideo = async (
    videoFile: FormData,
    onProgress?: (progress: number) => void
) => {
    const response = await apiClient.post(`/api/v1/videos/upload`, videoFile, {
        onUploadProgress: (progressEvent) => {
            if (progressEvent.total) {
                const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total)
                onProgress?.(percentCompleted)
            }
        }
    })
    return response.data
}

export const getVideoStatus = async (videoId: string)=> {
    const response = await apiClient.get(`api/v1/videos/${videoId}`)
    return response.data;
}

export const searchVideos = async (query: string): Promise<SearchResultItem[]> => {
    const response = await apiClient.get('/api/v1/videos/search', {
        params: { query }
    })
    return response.data
}

export const getVideoStream = async (videoId: string): Promise<VideoDetails> => {
    const response = await apiClient.get(`/api/v1/videos/${videoId}/stream`)
    return response.data
}

export const getMyVideos = async (): Promise<MyVideoItem[]> => {
    const response = await apiClient.get('/api/v1/videos/my')
    return response.data;
}
