import styles from './Header.module.css'
import Button from "../Button/Button.tsx";
import DragAndDropZone from "@/components/DragAndDropZone/DragAndDropZone.tsx";


const Header = () => {
    return(
        <header className={styles.header}>
            <div className={styles.containerLogo}>
                <p className={styles.logoText}>OmniSearch</p>
            </div>
            <div className={styles.uploadContainer}>
                <Button/>
                <DragAndDropZone/>
            </div>
        </header>
    )
}

export default Header