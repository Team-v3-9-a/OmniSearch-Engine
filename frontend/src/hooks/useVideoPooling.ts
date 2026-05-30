import {useUploadStore} from "@/store/useUploadStore.ts";
import {useEffect} from "react";
import {getVideoStatus} from "@/api";

export const useVideoPooling = () => {
  const { tasks, updateStatus } = useUploadStore()

  useEffect(() => {
    const activeTask = Object.values(tasks).filter(
        (task) =>
          !!task.backendId && ['UPLOADED', 'PROCESSING_MEDIA', 'PROCESSING_ML'].includes(task.status)
    )

    if (activeTask.length === 0) return

    const intervalId = setInterval(() => {
      activeTask.forEach(async task => {
        if (!task.backendId) return

        try {
          const data = await getVideoStatus(task.backendId)

          if ( data.status !== task.status ) {
            updateStatus(task.localId, data.status, task.backendId)
          }
        } catch (e) {
          console.log(`Ошибка пуллинга статуса для видео ${task.backendId}:`, e)
        }
      })
    }, 3000)

    return () => clearInterval(intervalId)
  }, [tasks, updateStatus]);
}