import styles from './MainPage.module.css'
import Search from "../../Search/Seacrh.tsx";


const MainPage = () => {
    return(
        <section className={styles.mainPage}>
            <div className={styles.searchContainer}>
                <h1>
                    Найдите <span className={styles.blueText}>тот самый момент</span>
                </h1>
                <Search/>
            </div>

        </section>
    )
}

export default MainPage