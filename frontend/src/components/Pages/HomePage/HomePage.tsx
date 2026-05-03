import styles from "./HomePage.module.css";
import Search from "@/components/Search/Seacrh.tsx";
import Button from "@/components/Button/Button.tsx";
import DragAndDropZone from "@/components/DragAndDropZone/DragAndDropZone.tsx";

const HomePage = () => {
  return(
      <section className={styles.mainPage}>
        <div className={styles.searchContainer}>
          <h1>
            Найдите <span className={styles.blueText}>тот самый момент</span>
          </h1>
          <Search/>
          <h2>
            Или загрузите <span className={styles.blueText}>свои</span>
          </h2>
          <div className={styles.uploadContainer}>
            <Button/>
            <DragAndDropZone/>
          </div>
        </div>
      </section>
  )
}

export default HomePage;