import { uploadVideo } from "@/api";
import { toast } from "react-toastify";
import {useUploadStore} from "@/store/useUploadStore.ts";


export const useUploadVideo = () => {
  const {addTask, updateProgress, updateStatus} = useUploadStore()

  const uploadFile = async (file: File) => {
    const localId = crypto.randomUUID()

    const formData = new FormData();

    formData.append('video', file)
    formData.append('title', file.name)

    addTask(localId, file.name)

    try {
      const data = await uploadVideo(formData, (progress) => {
        updateProgress(localId, progress)
      })
      updateStatus(localId, 'SUCCESS', data.id)
      toast.success(`Видео ${file.name} загружено`)
    } catch (e) {
      updateStatus(localId, 'ERROR')
      toast.error(`Ошибка`)
    }
  }

  return [uploadFile]
}