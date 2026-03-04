import styles from './Button.module.css'
import uploadIcon from '../../assets/Icons/UploadIcon.svg';
import {useRef, useState} from "react";
import {uploadVideo} from "../../api";
import {toast} from "react-toastify";

const Button = () => {
    const [status, setStatus] = useState<'IDLE' | 'UPLOADING' | 'SUCCESS' | 'ERROR'>('IDLE')

    const fileInputRef = useRef<HTMLInputElement>(null)

    const handleUploadClick = () => {
        fileInputRef.current?.click()
    }

    const handleFileChange = async (event: React.ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.[0]

        if(!file) return

        const formData = new FormData();

        formData.append('video', file)
        formData.append('title', file.name)

        setStatus('UPLOADING')

        try {
            await uploadVideo(formData)
        } catch (e) {
            setStatus('ERROR');
            toast(`Ошибка загрузки видео - ${status}`)
        }
    }

    return(
        <div className={styles.buttonContainer}>
            <input
                type="file"
                ref={fileInputRef}
                onChange={handleFileChange}
                style={{display: "none"}}
                accept="video/*"
            />
            <button
                className={styles.button}
                onClick={handleUploadClick}
            >
                <img src={uploadIcon} alt="upload icon"/>
                <p>Загрузить</p>
            </button>
        </div>

    )
}

export default Button