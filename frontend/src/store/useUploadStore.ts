import { create } from 'zustand'

export type UploadStatus = 'IDLE' | 'UPLOADING' | 'SUCCESS' | 'ERROR'

export interface UploadTask {
  localId: string;
  filename: string;
  progress: number;
  status: UploadStatus;
  backendId?: string;
}

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
  updateStatus: (localId, status, backendId) => set((state) => ({
    tasks: {
      ...state.tasks,
      [localId]: {
        ...state.tasks[localId],
        status,
        backendId
      }
    }
  })),
  removeTask: (localId) => set((state) => {
    const newTasks = {...state.tasks}
    delete newTasks[localId]
    return {tasks: newTasks}
  })
}));
