import styles from './UploadProgress.module.css'
import {useUploadStore} from "@/store/useUploadStore.ts";

const UploadProgress = () => {
  const {tasks} = useUploadStore()
  const taskList = Object.values(tasks)

  if (taskList.length === 0) return null

  return (
      <div className={styles.uploadProgress}>
        <h4 className={styles.downloadTitle}>Загрузки</h4>
        <div className={styles.separator}></div>
        <div className={styles.progressList}>
          {taskList.map((task) => (
              <div className={styles.taskItem}>
                <div className={styles.task}>
                  <span className={styles.taskTitle}>
                    {task.filename}
                  </span>
                  <div className={styles.statusContainer}>
                    <span className={`${styles.status} ${styles[task.status]}`}>
                    {task.status === 'UPLOADING' && 'Загружается'}
                      {task.status === 'SUCCESS' && 'Успешно'}
                      {task.status === 'ERROR' && 'Ошибка'}
                  </span>
                  </div>
                </div>
                <div className={styles.progressBarWrapper}>
                  <div
                      className={`${styles.progressBar} ${task.status == 'SUCCESS' ? styles.hidden : ''}`}
                      style={{width: `${task.status === 'SUCCESS' ? 100 : task.progress}%`}}
                  />
                  {
                      task.status === 'UPLOADING' &&
                      (
                          <span className={styles.progress}>{Math.round(task.progress)}%</span>
                      )
                  }
                </div>
              </div>
          ))}
        </div>
      </div>
  )
}

export default UploadProgress

