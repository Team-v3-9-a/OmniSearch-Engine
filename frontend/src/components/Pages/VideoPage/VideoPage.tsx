import styles from "./VideoPage.module.css";
import {useLocation, useParams, useSearchParams} from "react-router-dom";
import {useQuery} from "@tanstack/react-query";
import {getVideoStream} from "@/api";
import {useRef} from "react";

interface routerState {
  title: string;
  duration: number;
  created_date: string;
  snippet: string;
}

export const VideoPage = () => {

  const { id } = useParams<{ id: string }>()
  const [searchParams] = useSearchParams()
  const startTime = searchParams.get("t")
  
  const videoRef = useRef<HTMLVideoElement>(null);

  const location = useLocation()
  const { title, duration, created_date, snippet } = (location.state as routerState) || {}

  const { data: videoDetails, isLoading, isError} = useQuery({
    queryKey: ['streamUrl', id, startTime],
    queryFn: () => getVideoStream(id!),
    enabled: !!id
  })

  const handleLoadedMetadata = () => {
    if( videoRef.current && startTime) {
      videoRef.current.currentTime = Number(startTime)
      videoRef.current.play().catch(console.warn)
    }
  }
  
  if (!id) return <p>Ошибка: ID видео не передан в URL</p>;

  console.log('Дебаг плеера:', {
    isIdPresent: !!id,
    isLoading,
    url: videoDetails?.streamUrl
  });

  if (isLoading) return <p>Загрузка</p>
  if (isError || !videoDetails) return <p>Ошибка</p>


  return(
      <section className={styles.mainContainer}>
        <div className={styles.playerWrapper}>
          <video
              ref={videoRef}
              src={videoDetails?.streamUrl}
              controls
              muted
              crossOrigin="anonymous"
              className={styles.videoPlayer}
              onLoadedMetadata={handleLoadedMetadata}
          />
        </div>

        <div className={styles.metadataContainer}>
          <h1 className={styles.title}>{title || 'Без названия'}</h1>

          {snippet && (
              <div className={styles.snippetBox}>
                Совпадение по тексту: «{snippet}»
              </div>
          )}

          <p className={styles.createdAt}>
            Создано: {created_date || 'Неизвестно'}
          </p>
          <p className={styles.duration}>
            Длительность: {duration || 'Неизвестно'}
          </p>
        </div>
      </section>
  )
}
