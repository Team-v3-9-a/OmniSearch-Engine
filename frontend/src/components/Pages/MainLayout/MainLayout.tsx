import styles from './MainLayout.module.css'
import Header from "@/components/Header/Header.tsx";
import { Outlet } from "react-router-dom";
import { useVideoPooling } from "@/hooks/useVideoPooling.ts";


const MainLayout = () => {
    // Run background status pooling globally for active uploads
    useVideoPooling();

    return (
        <div className={styles.mainLayout}>
            <Header />
            <main className={styles.content}>
                <Outlet />
            </main>
        </div>
    )
}

export default MainLayout;
