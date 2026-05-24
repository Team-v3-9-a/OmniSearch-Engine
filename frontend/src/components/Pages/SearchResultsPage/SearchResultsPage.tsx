import styles from './SearchResultsPage.module.css'
import {useSearchParams} from "react-router-dom";
import { mockSearchResults} from './mockResults.ts'
import {useQuery} from "@tanstack/react-query";
import {VideoCard} from "@/components/VideoCard/VideoCard.tsx";
import {searchVideos, type SearchResultItem} from "@/api";
import {VideoSkeleton} from "@/components/VideoCard/VideoSkeleton/VideoSkeleton.tsx";

const SearchResultsPage = () => {
  const [searchParams]  = useSearchParams()

  const query = searchParams.get("query") || '';

  const { data, isLoading, isFetching } = useQuery({
    queryKey: ['results', query],
    queryFn: () => searchVideos(query),
    initialData: mockSearchResults,
    enabled: !!query
  })

  const showLoading = isLoading || isFetching;

  const isEmpty = !showLoading && data && data.length === 0;

  return(
      <section className={styles.mainContainer}>
          <p className={styles.title}>
            {query ? `Результаты поиска по запросу: "${query}"` : 'Введите запрос для поиска'}
          </p>
          <div className={styles.resultList}>
            {
                showLoading && (
                    <div className={styles.skeletonContainer}>
                      <VideoSkeleton/>
                      <VideoSkeleton/>
                      <VideoSkeleton/>
                    </div>
                )
            }

            {
                isEmpty && query && (
                    <div className={styles.emptyState}>
                      <p>Ничего не найдено. Попробуйте изменить запрос.</p>
                    </div>
                )
            }

            {
                !showLoading && data && data.length > 0 && data.map((videoItem: SearchResultItem) => (
                    <VideoCard 
                        key={videoItem.video_id} 
                        {...videoItem}
                    />
                ))
            }
          </div>
      </section>
  )
}

export default SearchResultsPage;
