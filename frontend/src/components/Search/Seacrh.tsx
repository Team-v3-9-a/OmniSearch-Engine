import styles from './Search.module.css'
import searchIcon from '../../assets/Icons/Search_Magnifying_Glass.svg'
import {useState} from "react";
import {useNavigate} from "react-router-dom";


const Search = () => {
    const [searchQuery, setSearchQuery] = useState("");
    const navigate = useNavigate()

    const handleSearch = (e: React.SubmitEvent) => {
        e.preventDefault()

        if (!searchQuery.trim()) return;

        setSearchQuery("")

        navigate(`/search?query=${searchQuery.trim()}`)


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
                Найти
            </button>
        </form>
    )
}

export default Search