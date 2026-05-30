import { useQuery } from '@tanstack/react-query';
import styles from './MyVideosPage.module.css'
import { getMyVideos } from '@/api';
import type { MyVideoItem } from '@/types/api.ts';
import { parseDate } from '@/utils/parseDate';
import { VideoFileIcon } from '@/assets/Icons/VideoFile';
import { StatusLabel } from '@/components/StatusLabel/StatusLabel';
import { useUploadStore } from '@/store/useUploadStore.ts';

export const MyVideosPage = () => {
  const { tasks } = useUploadStore();

  const { data = [], isLoading, error } = useQuery<MyVideoItem[]>({
    queryKey: ['my-videos'],
    queryFn: getMyVideos
  })

  const combinedVideos = [...data];

  Object.values(tasks).forEach((task) => {
    const existingVideo = task.backendId
      ? combinedVideos.find((v) => v.id === task.backendId)
      : null;

    if (existingVideo) {
      existingVideo.status = task.status;
      (existingVideo as any).progress = task.progress;
    } else {
      combinedVideos.unshift({
        id: task.localId,
        title: task.filename,
        status: task.status,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        progress: task.progress,
      } as any);
    }
  });

  const inProgressVideos = combinedVideos.filter((video) => !['READY', 'ERROR'].includes(video.status));
  const completedVideos = combinedVideos.filter((video) => ['READY', 'ERROR'].includes(video.status));

  const renderVideoCard = (video: MyVideoItem & { progress?: number }) => {
    const showProgress = ['UPLOADING', 'UPLOADED', 'PROCESSING_MEDIA', 'PROCESSING_ML'].includes(video.status);
    const progress = video.progress ?? (video.status === 'UPLOADING' ? 0 : null);

    return (
      <li key={video.id} className={styles.videoItem}>
        <div className={styles.iconWrapper}>
          <VideoFileIcon className={styles.videoIcon} />
          <h4 className={styles.videoTitle}>{video.title}</h4>
        </div>

        <div className={styles.statusWrapper}>
          <p className={styles.videoStatus}>Статус:</p>
          <StatusLabel status={video.status} />
        </div>

        {showProgress && (
          <div className={styles.progressBarWrapper}>
            <div className={styles.progressBarInfo}>
              <span className={styles.progressBarLabel}>
                {video.status === 'UPLOADING' ? 'Загрузка файла...' : 'Обработка видео...'}
              </span>
              {video.status === 'UPLOADING' && progress !== null && (
                <span className={styles.progressPercent}>{Math.round(progress)}%</span>
              )}
            </div>
            <div className={styles.progressTrack}>
              {video.status === 'UPLOADING' ? (
                <div
                  className={styles.progressBar}
                  style={{ width: `${progress}%` }}
                />
              ) : (
                <div className={styles.progressBarIndeterminate} />
              )}
            </div>
          </div>
        )}

        <p className={styles.createdAt}>Дата создания: {parseDate(video.createdAt)}</p>
        <p className={styles.updatedAt}>Дата обновления: {parseDate(video.updatedAt)}</p>
      </li>
    );
  };

  return (
    <main className={styles.mainContainer}>
      <h3 className={styles.title}>Мои видео</h3>
      <p className={styles.description}>Здесь будут отображаться ваши загруженные видео.</p>

      {isLoading && (
        <p className={styles.loadingText}>Загрузка...</p>
      )}

      {error && (
        <p className={styles.errorState}>Ошибка загрузки видео</p>
      )}

      {inProgressVideos.length > 0 && (
        <div className={styles.section}>
          <h4 className={styles.sectionTitle}>В процессе ({inProgressVideos.length})</h4>
          <ul className={styles.videoList}>
            {inProgressVideos.map(renderVideoCard)}
          </ul>
        </div>
      )}

      {completedVideos.length > 0 && (
        <div className={styles.section}>
          <h4 className={styles.sectionTitle}>Завершенные ({completedVideos.length})</h4>
          <ul className={styles.videoList}>
            {completedVideos.map(renderVideoCard)}
          </ul>
        </div>
      )}

      {combinedVideos.length === 0 && !isLoading && !error && (
        <p className={styles.emptyText}>У вас пока нет загруженных видео.</p>
      )}
    </main>
  );
}