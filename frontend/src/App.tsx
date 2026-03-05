import './App.css'
import Header from "./components/Header/Header.tsx";
import MainPage from "./components/Pages/MainPage/MainPage.tsx";
import {ToastContainer} from "react-toastify";

function App() {
  return (
    <div className="app-container">
      <Header />
      <MainPage />
      <ToastContainer />
    </div>
  )
}

export default App
