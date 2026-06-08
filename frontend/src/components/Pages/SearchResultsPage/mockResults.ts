import type { SearchResultItem } from "@/types/api.ts";

export const mockSearchResults: SearchResultItem[] = [
  {
    video_id: "dQw4w9WgXcQ",
    title: "Весеннее пробуждение природы",
    thumbnail_url: "https://images.unsplash.com/photo-1469474968028-56623f02e42e?w=400&h=300&fit=crop",
    score: 0.99,
    segments: [
      {
        text_snippet: "...на поляне распускается первый весенний цветок, его бутон тянется к солнцу...",
        start_time: 0.0,
        end_time: 2.5
      },
      {
        text_snippet: "...пчела аккуратно садится на розовые лепестки цветка, чтобы собрать нектар...",
        start_time: 3.0,
        end_time: 5.5
      }
    ]
  },
  {
    video_id: "https://vjs.zencdn.net/v/oceans.mp4",
    title: "Тайны океана и коралловых рифов",
    thumbnail_url: "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=400&h=300&fit=crop",
    score: 0.88,
    segments: [
      {
        text_snippet: "...на дне рифа распустилась морская актиния — удивительный хищный подводный цветок...",
        start_time: 14.0,
        end_time: 18.5
      }
    ]
  },
  {
    video_id: "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/friday.mp4",
    title: "Прогулка по городскому ботаническому саду",
    thumbnail_url: "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400&h=300&fit=crop",
    score: 0.75,
    segments: [
      {
        text_snippet: "...люди гуляют по центральной аллее, наслаждаясь теплым вечером...",
        start_time: 0.0,
        end_time: 3.0
      }
    ]
  }
];