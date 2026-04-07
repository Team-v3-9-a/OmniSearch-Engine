import styles from './Button.module.css'
import uploadIcon from '../../assets/Icons/UploadIcon.svg';
import { useRef } from "react";
import * as React from "react";
import {useUploadVideo} from "@/hooks/useUploadVideo.ts";

const Button = () => {
    const [uploadVideo] = useUploadVideo()

    const fileInputRef = useRef<HTMLInputElement>(null)

    const handleUploadClick = () => {
        fileInputRef.current?.click()
    }

    const handleFileChange = async (event: React.ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.[0]
        if(!file) return
        
        await uploadVideo(file)

        if (event.target) {
            event.target.value = ''
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