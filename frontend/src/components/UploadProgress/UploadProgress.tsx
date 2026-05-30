import styles from './UploadProgress.module.css'
import {useUploadStore} from "@/store/useUploadStore.ts";
import {useEffect, useState} from "react";
import collapseIcon from "@/assets/Icons/Chevron_Down.svg";
import {useVideoPooling} from "@/hooks/useVideoPooling.ts";
import { getStatusText } from '@/utils/statusText';

const UploadProgress = () => {
  const {tasks} = useUploadStore()
  const taskList = Object.values(tasks)

  useVideoPooling()

  const [isExpanded, setIsExpanded] = useState(false)

  useEffect(() => {
    if (taskList.length > 0) {
      setIsExpanded(true)
    }
  }, [taskList.length])

  return (
      <div className={`${styles.uploadProgress} ${!isExpanded && styles.uploadProgressCollapsed}`}>
        <div className={styles.downloadContainer}>
          <h4 className={styles.downloadTitle}>Загрузки</h4>
          <button
              className={styles.collapseBtn}
              onClick={() => setIsExpanded(!isExpanded)}
          >
            <img className={styles.collapseIcon} src={collapseIcon} alt='Кнопка сворачивания'/>
          </button>
        </div>
        <div className={`${styles.separator} ${!isExpanded && styles.hidden}`}></div>
        <div className={`${styles.progressList} ${!isExpanded && styles.hidden}`}>
          {taskList.length == 0 ? <h4 className={styles.taskTitle}>Нет потоковых файлов</h4> : ''}
          {taskList.map((task) => (
              <div key={task.localId} className={styles.taskItem}>
                <div className={styles.task}>
                    <span className={styles.taskTitle}>
                      {task.filename}
                    </span>
                  <div className={styles.statusContainer}>
                      <span className={`${styles.status} ${styles[task.status]}`}>
                        {getStatusText(task.status)}
                      </span>
                    {(task.status === 'READY' || task.status === 'ERROR') && (
                        <button
                            onClick={() => useUploadStore.getState().removeTask(task.localId)}
                            className={styles.closeBtn}
                        >
                          ✕
                        </button>
                    )}
                  </div>
                </div>
                <div className={styles.progressBarWrapper}>
                  <div
                      className={`${styles.progressBar} ${task.status == 'UPLOADED' ? styles.hidden : ''}`}
                      style={{width: `${task.status === 'UPLOADED' ? 100 : task.progress}%`}}
                  />
                  {task.status === 'UPLOADING' && (
                      <span className={styles.progress}>{Math.round(task.progress)}%</span>
                  )}
                </div>
              </div>
          )) }
        </div>
      </div>
  )
}

export default UploadProgress