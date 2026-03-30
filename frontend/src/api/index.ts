import axios from 'axios'

export const uploadVideo = async (
    videoFile: FormData,
    onProgress?: (progress: number) => void
) => {
    const response = await axios.post(`/api/v1/videos/upload`, videoFile, {
        headers: {
            'Content-Type': 'multipart/form-data'
        },
        validateStatus: (status) => status < 500,
        onUploadProgress: (progressEvent) => {
            if (progressEvent.total) {
                const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total)
                onProgress?.(percentCompleted)
            }
        }
    })

    return response.data
}
