import styles from "./HomePage.module.css";
import Search from "@/components/Search/Seacrh.tsx";
import Button from "@/components/Button/Button.tsx";
import DragAndDropZone from "@/components/DragAndDropZone/DragAndDropZone.tsx";
import { useUploadStore } from "@/store/useUploadStore.ts";
import { Link } from "react-router-dom";

const HomePage = () => {
  const { tasks } = useUploadStore();

  // Find all active uploading tasks
  const uploadingTasks = Object.values(tasks).filter(
    (task) => task.status === 'UPLOADING'
  );

  return (
    <section className={styles.mainPage}>
      {uploadingTasks.length > 0 && (
        <div className={styles.notification}>
          <h5 className={styles.notificationTitle}>Загрузка видео начата</h5>
          <p className={styles.notificationMessage}>
            Файл <strong>«{uploadingTasks[0].filename}»</strong> начал загружаться. Вы можете следить за детальным процессом во вкладке <Link to="/my-videos" className={styles.notificationLink}>«Мои видео»</Link>.
          </p>
        </div>
      )}

      <div className={styles.searchContainer}>
        <h1>
          Найдите <span className={styles.blueText}>тот самый момент</span>
        </h1>
        <Search />
        <h2>
          Или загрузите <span className={styles.blueText}>СВОи</span>
        </h2>
        <div className={styles.uploadContainer}>
          <Button />
          <DragAndDropZone />
        </div>
      </div>
    </section>
  )
}

export default HomePage;