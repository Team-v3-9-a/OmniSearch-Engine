import styles from './Header.module.css'
import {ThemeSwitcher} from "@/components/ThemeSwitcher";
import logo from '@/assets/Icons/Logo.svg'
import {Link} from "react-router-dom";

const Header = () => {
    return(
        <header className={styles.header}>
          <Link to="/" className={styles.containerLogo}>
            <img className={styles.logo} src={logo} alt='logo'/>
            <p className={styles.logoText}>NexusV</p>
          </Link>
          <ThemeSwitcher/>
        </header>
    )
}

export default Header