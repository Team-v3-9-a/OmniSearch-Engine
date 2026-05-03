import './App.css'
import MainLayout from "@/components/Pages/MainLayout/MainLayout.tsx";
import {ToastContainer} from "react-toastify";
import UploadProgress from "@/components/UploadProgress/UploadProgress.tsx";
import Header from "@/components/Header/Header.tsx";
import {Outlet} from "react-router-dom";

function App() {
  return (
    <MainLayout className="app-container">
      <Header/>
      <Outlet/>
      <ToastContainer />
      <UploadProgress/>
    </MainLayout>
  )
}

export default App
