import axios from "axios";

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_SERVER_HOST,
  timeout: -1,
  headers: {
    "Accept": "application/json",
  },
})

apiClient.interceptors.request.use(
    (response) => response,
    (error) => {
      console.error('API Error:', error.response?.status, error.response?.data);
      return Promise.reject(error);
    }
)