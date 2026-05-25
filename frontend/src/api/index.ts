import {apiClient} from "@/api/client.ts";

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

export interface SearchResultItem {
    video_id: string,
    title: string,
    thumbnail_url: string,
    score: number,
    segments: {
        text_snippet: string,
        start_time: number,
        end_time: number
    }[]
}


export const searchVideos = async (query: string): Promise<SearchResultItem[]> => {
    const response = await apiClient.get('/api/v1/videos/search', {
        params: { query }
    })
    return response.data
}
