import './App.css'
import MainPage from "./components/Pages/MainPage/MainPage.tsx";
import {ToastContainer} from "react-toastify";
import Header from "@/components/Header/Header.tsx";
import UploadProgress from "@/components/UploadProgress/UploadProgress.tsx";

function App() {
  return (
    <div className="app-container">
      <Header />
      <MainPage />
      <ToastContainer />
      <UploadProgress/>
    </div>
  )
}

export default App
