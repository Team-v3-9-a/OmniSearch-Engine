import styles from './Button.module.css'
import uploadIcon from '../../assets/Icons/UploadIcon.svg';
import { useRef } from "react";
import { uploadVideo } from "../../api";
import { toast } from "react-toastify";
import * as React from "react";
import { useUploadStore } from "../../store/useUploadStore.ts";

const Button = () => {
    const {addTask, updateProgress, updateStatus} = useUploadStore()

    const fileInputRef = useRef<HTMLInputElement>(null)

    const handleUploadClick = () => {
        fileInputRef.current?.click()
    }

    const handleFileChange = async (event: React.ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.[0]
        if(!file) return

        const localId = crypto.randomUUID()

        const formData = new FormData();

        formData.append('video', file)
        formData.append('title', file.name)

        addTask(localId, file.name)

        try {
            const data = await uploadVideo(formData, (progress) => {
                updateProgress(localId, progress)
            })
            updateStatus(localId, 'SUCCESS', data.id)
            toast.success(`Видео ${file.name} загружено`)
        } catch {
            updateStatus(localId, 'ERROR')
            toast.error(`Ошибка`)
        } finally {
            if (event.target) {
                event.target.value = ''
            }
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