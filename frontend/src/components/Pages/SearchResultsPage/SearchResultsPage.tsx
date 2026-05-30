import styles from './SearchResultsPage.module.css'
import { useSearchParams } from "react-router-dom";
import { mockSearchResults } from './mockResults.ts'
import { VideoCard } from "@/components/VideoCard/VideoCard.tsx";
import type { SearchResultItem } from "@/types/api.ts";
import { VideoSkeleton } from "@/components/VideoCard/VideoSkeleton/VideoSkeleton.tsx";
import { useQuery } from '@tanstack/react-query';
import { searchVideos } from '@/api/index.ts';

const SearchResultsPage = () => {
  const [searchParams] = useSearchParams()

  const query = searchParams.get("query") || '';

  const { data, isLoading, isFetched, error } = useQuery({
    queryKey: ['results', query],
    queryFn: () => searchVideos(query),
    // initialData: mockSearchResults,
    enabled: !!query
  })


  const showLoading = isLoading || isFetched;

  const isEmpty = !showLoading && data && data.length === 0;

  return (
    <section className={styles.mainContainer}>
      <h3 className={styles.title}>
        {query ? `Результаты поиска по запросу: "${query}"` : 'Введите запрос для поиска'}
      </h3>
      <div className={styles.resultList}>
        {
          showLoading && !error && (
            <>
              <VideoSkeleton />
              <VideoSkeleton />
              <VideoSkeleton />
              <VideoSkeleton />
              <VideoSkeleton />
              <VideoSkeleton />
              <VideoSkeleton />
            </>
          )
        }
        {
          isEmpty && !isLoading && !error && query && (
            <div className={styles.emptyState}>
              <p>Ничего не найдено. Попробуйте изменить запрос.</p>
            </div>
          )
        }

        {
          !showLoading && !error && data && data.length > 0 && data.map((videoItem: SearchResultItem, index) => (
            <VideoCard
              key={`${videoItem.video_id}-${index}`}
              {...videoItem}
            />
          ))
        }

        {
          error && (
            <p className={styles.errorState}>Ошибка загрузки видео</p>
          )
        }
      </div>
    </section>
  )
}

export default SearchResultsPage;
