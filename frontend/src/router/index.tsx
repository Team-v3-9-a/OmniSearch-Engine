import { createBrowserRouter} from "react-router-dom";
import {MainLayout} from "@/components/Pages/MainLayout";
import {SearchResultsPage} from "@/components/Pages/SearchResultsPage";
import {VideoPage} from "@/components/Pages/VideoPage";
import {HomePage} from "@/components/Pages/HomePage";
import {NotFoundPage} from "@/components/Pages/NotFoundPage";
import { MyVideosPage } from "@/components/Pages/MyVideosPage/MyVideosPage";

export const router = createBrowserRouter([
  {
    path: '/',
    element: <MainLayout/>,
    errorElement: <NotFoundPage/>,
    children: [
      {
        index: true,
        element: <HomePage/>
      },
      {
        path: 'search',
        element: <SearchResultsPage/>
      },
      {
        path: 'video/:id',
        element: <VideoPage/>
      },
      {
        path: 'my-videos',
        element: <MyVideosPage/>
      }
    ]
  }
])
