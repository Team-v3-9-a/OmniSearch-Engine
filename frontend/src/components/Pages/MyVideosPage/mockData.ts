import type { MyVideoItem } from "@/types/api";

export const mockData: MyVideoItem[] = [
  {
    id: "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4",
    title: "Весеннее пробуждение природы",
    status: "READY",
    createdAt: "2026-05-25T07:22:12.654669",
    updatedAt: "2026-05-25T07:22:31.238531"
  },
  {
    id: "https://vjs.zencdn.net/v/oceans.mp4",
    title: "Тайны океана и коралловых рифов",
    status: "PROCESSING_ML",
    createdAt: "2026-05-05T07:10:38.126477",
    updatedAt: "2026-05-05T07:15:00.742423"
  },
  {
    id: "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/friday.mp4",
    title: "Прогулка по городскому ботаническому саду",
    status: "PROCESSING_MEDIA",
    createdAt: "2026-05-07T11:58:50.837857",
    updatedAt: "2026-05-07T11:58:50.837857"
  },
  {
    id: "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4?stream=archive",
    title: "Цветок макросъемка (Оригинал)",
    status: "UPLOADED",
    createdAt: "2026-05-16T15:32:43.271517",
    updatedAt: "2026-05-16T15:32:52.369733"
  },
  {
    id: "https://vjs.zencdn.net/v/oceans.mp4?stream=uploading",
    title: "Подводная экспедиция_RAW.mp4",
    status: "UPLOADING",
    createdAt: "2026-05-25T07:22:12.654669",
    updatedAt: "2026-05-25T07:22:31.238531"
  },
  {
    id: "corrupted-file-uuid-04429c03",
    title: "Поврежденная_запись_с_камеры.avi",
    status: "ERROR",
    createdAt: "2026-05-25T07:43:03.635634",
    updatedAt: "2026-05-25T07:43:21.997090"
  }
];