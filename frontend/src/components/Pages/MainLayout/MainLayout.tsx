import styles from './MainLayout.module.css'
import Header from "@/components/Header/Header.tsx";
import {Outlet} from "react-router-dom";
import {ToastContainer} from "react-toastify";
import UploadProgress from "@/components/UploadProgress/UploadProgress.tsx";


const MainLayout = () => {
    return (
        <div className={styles.mainLayout}>
            <Header/>
            <main className={styles.content}>
                <Outlet/>
            </main>
            <ToastContainer/>
            <UploadProgress/>
        </div>
    )
}

export default MainLayout;
