import axios from 'axios'

const HOST = import.meta.env.VITE_SERVER_HOST

export const uploadVideo = async (
    videoFile: FormData,
    onProgress?: (progress: number) => void
) => {
    const response = await axios.post(`${HOST}/api/v1/videos/upload`, videoFile, {
        headers: {
            'Content-Type': 'multipart/form-data'
        },
        onUploadProgress: (progressEvent) => {
            if (progressEvent.total) {
                const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total)
                onProgress?.(percentCompleted)
            }
        }
    })

    return response.data
}
