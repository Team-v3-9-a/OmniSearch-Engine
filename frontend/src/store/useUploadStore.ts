import type { UploadStatus, UploadTask } from '@/types/api';
import { create } from 'zustand'

export interface UploadStore {
  tasks: Record<string, UploadTask>,
  addTask: (localId: string, filename: string) => void,
  updateProgress: (localId: string, progress: number) => void,
  updateStatus: (localId: string, status: UploadStatus, backendId?: string) => void,
  removeTask: (localId: string) => void
}

export const useUploadStore = create<UploadStore>((set) => ({
  tasks: {},
  addTask: (localId, filename) => set((state) => ({
    tasks: {
      ...state.tasks,
      [localId]: {localId, filename, progress: 0, status: 'UPLOADING'}
    }
  })),
  updateProgress: (localId, progress) => set((state) => ({
    tasks: {
      ...state.tasks,
      [localId]: {...state.tasks[localId], progress}
    }
  })),
  updateStatus: (localId, status, backendId) => {
    set((state) => ({
      tasks: {
        ...state.tasks,
        [localId]: {
          ...state.tasks[localId],
          status,
          backendId
        }
      }
    }))

    // if (status == 'READY' || status == 'ERROR') {
    //   const timeoutMs = status == 'READY' ? 10000 : 20000
    //   setTimeout(() => {
    //     get().removeTask(localId)
    //   }, timeoutMs)
    // }
  },
  removeTask: (localId) => set((state) => {
    const newTasks = {...state.tasks}
    delete newTasks[localId]
    return {tasks: newTasks}
  })
}));