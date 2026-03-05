import axios from 'axios'

const API_BASE_URL = `${import.meta.env.VITE_SERVER_HOST}/api/v1`

export const uploadVideo = async (videoFile: FormData) => {
    const response = await axios.post(`${API_BASE_URL}/upload`, {videoFile}, {
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    })
    return response.data
}