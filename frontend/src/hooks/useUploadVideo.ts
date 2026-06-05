import { uploadVideo } from "@/api";
import { useUploadStore } from "@/store/useUploadStore.ts";


const generateUUID = () => {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
};

export const useUploadVideo = () => {
  const { addTask, updateProgress, updateStatus } = useUploadStore()

  const uploadFile = async (file: File) => {
    const localId = generateUUID()

    const formData = new FormData();

    formData.append('video', file)
    formData.append('title', file.name)

    addTask(localId, file.name)

    try {
      const data = await uploadVideo(formData, (progress) => {
        updateProgress(localId, progress)
      })
      updateStatus(localId, 'UPLOADED', data.id)
    } catch (e) {
      updateStatus(localId, 'ERROR')
    }
  }

  return [uploadFile]
}