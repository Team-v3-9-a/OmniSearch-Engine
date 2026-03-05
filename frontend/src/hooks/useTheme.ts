import {useEffect, useState} from "react";

export const useTheme = (): [string, () => void] => {
    const [theme, setTheme] = useState(() => {
        return localStorage.getItem("theme") || 'light';
    })

    useEffect(() => {
        document.documentElement.setAttribute('data-theme', theme)
        localStorage.setItem("theme", theme)
    }, [theme]);

    const toggleTheme = () => {
        setTheme((prevTheme) => {
            return prevTheme === 'light' ? 'dark' : 'light';})
    };

    return [theme, toggleTheme]
}