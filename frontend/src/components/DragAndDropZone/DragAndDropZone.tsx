import styles from './DragAndDropZone.module.css'
import React, {useState} from "react";
import {useUploadVideo} from "@/hooks/useUploadVideo.ts";

const DragAndDropZone = () => {
  const [uploadVideo] = useUploadVideo()
  const [isDragging, setIsDragging] = useState(false);

  const handleDragEnter = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  }
  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
  }

  const handleDrop = async (e: React.DragEvent) => {
    e.preventDefault()
    setIsDragging(false)

    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      const droppedFiles = Array.from(e.dataTransfer.files)
      const videoFiles = droppedFiles.filter(file => file.type.startsWith("video/"));
      for (const file of videoFiles) {
        uploadVideo(file)
      }
    }
  }

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault()

  }

  return (
      <div
          className={`${styles.dragAndDropZone} ${isDragging ? styles.dragging : ''}`}
          onDrop={handleDrop}
          onDragOver={handleDragOver}
          onDragEnter={handleDragEnter}
          onDragLeave={handleDragLeave}
      >
        <p className={`${styles.dragText}`}>
          {isDragging ? "Да-да сюда" : "Перетащите сюда файлы"}
        </p>
      </div>
  )
}

export default DragAndDropZone