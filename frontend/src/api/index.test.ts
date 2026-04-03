import { describe, it, expect, vi, beforeEach } from 'vitest'
import axios from 'axios'
import { uploadVideo } from './index'

vi.mock('axios')

describe('uploadVideo', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('should successfully upload a video and return data', async () => {
        const mockData = { id: 'video-123', url: 'http://example.com/video.mp4' }
        vi.mocked(axios.post).mockResolvedValueOnce({ data: mockData })

        const formData = new FormData()
        formData.append('file', new Blob(['test'], { type: 'video/mp4' }))

        const result = await uploadVideo(formData)

        expect(axios.post).toHaveBeenCalledTimes(1)
        expect(axios.post).toHaveBeenCalledWith(
            '/api/v1/videos/upload',
            formData,
            expect.objectContaining({
                headers: {
                    'Content-Type': 'multipart/form-data'
                },
                validateStatus: expect.any(Function),
                onUploadProgress: expect.any(Function)
            })
        )
        expect(result).toEqual(mockData)
    })

    it('should report upload progress correctly', async () => {
        vi.mocked(axios.post).mockImplementationOnce((url, data, config) => {
            if (config?.onUploadProgress) {
                config.onUploadProgress({ loaded: 50, total: 100 } as any)
                config.onUploadProgress({ loaded: 100, total: 100 } as any)
            }
            return Promise.resolve({ data: { success: true } })
        })

        const onProgress = vi.fn()
        const formData = new FormData()

        await uploadVideo(formData, onProgress)

        expect(onProgress).toHaveBeenCalledTimes(2)
        expect(onProgress).toHaveBeenNthCalledWith(1, 50)
        expect(onProgress).toHaveBeenNthCalledWith(2, 100)
    })

    it('should handle missing total in progress event safely', async () => {
        vi.mocked(axios.post).mockImplementationOnce((url, data, config) => {
            if (config?.onUploadProgress) {
                // Should not call onProgress if total is undefined/0
                config.onUploadProgress({ loaded: 50 } as any)
            }
            return Promise.resolve({ data: { success: true } })
        })

        const onProgress = vi.fn()
        const formData = new FormData()

        await uploadVideo(formData, onProgress)

        expect(onProgress).not.toHaveBeenCalled()
    })

    it('should validate status less than 500', async () => {
        vi.mocked(axios.post).mockResolvedValueOnce({ data: {} })
        const formData = new FormData()

        await uploadVideo(formData)

        const callArgs = vi.mocked(axios.post).mock.calls[0]
        const config = callArgs[2]
        const validateStatus = config!.validateStatus!

        expect(validateStatus(200)).toBe(true)
        expect(validateStatus(400)).toBe(true)
        expect(validateStatus(499)).toBe(true)
        expect(validateStatus(500)).toBe(false)
        expect(validateStatus(503)).toBe(false)
    })

    it('should throw an error if the request fails', async () => {
        const error = new Error('Network Error')
        vi.mocked(axios.post).mockRejectedValueOnce(error)

        const formData = new FormData()

        await expect(uploadVideo(formData)).rejects.toThrow('Network Error')
    })
})
