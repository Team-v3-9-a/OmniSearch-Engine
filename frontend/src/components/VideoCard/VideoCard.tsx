import styles from './VideoCard.module.css'
import { Link } from "react-router-dom";
import type { SearchResultItem } from "@/types/api";

type Props = SearchResultItem;

const formatTime = (time: number) => {
  const hours = Math.floor(time / 3600);
  const minutes = Math.floor((time % 3600) / 60);
  const seconds = Math.floor(time % 60);

  return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
}

export const VideoCard = (props: Props) => {
  return (
    <div className={styles.card}>
      <Link
        className={styles.link}
        to={`/video/${props.video_id}?t=${props.segments[0]?.start_time || 0}`}
        state={{
          title: props.title,
          duration: props.duration,
          created_date: props.upload_date,
          snippet: props.segments[0]?.text_snippet || '',
        }}
      >
        <img src={props.thumbnail_url} alt={props.title} className={styles.thumbnail} />
      </Link>

      <div className={styles.info}>
        <p className={styles.title}>
          {props.title}
        </p>

        Найденные сегменты:

        <div className={styles.timeContainer}>
          {props.segments.map((segment: { start_time: number; end_time: number; text_snippet: string }, index) => (
            <Link
              key={`${segment.start_time}-${index}`}
              className={styles.segmentLink}
              to={`/video/${props.video_id}?t=${segment.start_time}`}
              state={{
                title: props.title,
                duration: props.duration,
                created_date: props.upload_date,
                snippet: segment.text_snippet,
              }}
            >
              <p className={styles.segmentText}>
                <span className={styles.time}>
                  {formatTime(segment.start_time)} - {formatTime(segment.end_time)}
                </span>
                {' - '}
                <span className={styles.snippet}>
                  {segment.text_snippet}
                </span>
              </p>
            </Link>
          ))}
        </div>
      </div>
    </div>
  );
};
