import styles from './Header.module.css'
import logoIcon from '../../assets/Logotype/VK Logo.svg';
import Button from "../Button/Button.tsx";


const Header = () => {
    return(
        <header className={styles.header}>
            <div className={styles.containerLogo}>
                <img className={styles.logo} src={logoIcon} alt="OmniSearch logo"/>
                <p className={styles.logoText}>OmniSearch</p>
            </div>
            <div className={styles.containerButtons}>
                <Button/>
            </div>
        </header>
    )
}

export default Header