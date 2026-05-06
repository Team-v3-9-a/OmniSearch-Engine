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
