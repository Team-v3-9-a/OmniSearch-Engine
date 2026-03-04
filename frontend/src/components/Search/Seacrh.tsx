import styles from './Search.module.css'
import searchIcon from '../../assets/Icons/SearchIcon.svg'
import {useState} from "react";
import {toast} from "react-toastify";


const Search = () => {
    const [searchQuery, setSearchQuery] = useState("");

    const handleSearch = (e: React.FormEvent) => {
        e.preventDefault()

        // Пока тестовое поведение
        if (!searchQuery.trim()) return;

        setSearchQuery("")

        toast(searchQuery, {
            position: "top-right",
            autoClose: 5000,
        });
    }

    return(
        <form onSubmit={handleSearch} className={styles.searchString}>
            <img  className={styles.searchIcon} src={searchIcon} alt='search icon'/>
            <input
                className={styles.searchInput}
                type='text'
                value={searchQuery}
                placeholder='Поиск по ключевым кадрам, темам или созданиям...'
                onChange={(e) => setSearchQuery(e.target.value)}
            />
            <button
                className={styles.searchButton}
                type='submit'
            >
                Поиск
            </button>
        </form>
    )
}

export default Search