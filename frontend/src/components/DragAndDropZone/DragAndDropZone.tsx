import styles from './DragAndDropZone.module.css'
import React, { useEffect, useRef, useState } from "react";
import { useUploadVideo } from "@/hooks/useUploadVideo.ts";

const DragAndDropZone = () => {
  const [uploadVideo] = useUploadVideo();
  const [isDragging, setIsDragging] = useState(false);
  const dragCounter = useRef(0);

  // Чтобы при промахе мимо зоны браузер не открывал файл как страницу
  useEffect(() => {
    const prevent = (e: DragEvent) => e.preventDefault();
    window.addEventListener('dragover', prevent);
    window.addEventListener('drop', prevent);
    return () => {
      window.removeEventListener('dragover', prevent);
      window.removeEventListener('drop', prevent);
    };
  }, []);

  const handleDragEnter = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    dragCounter.current += 1;
    if (e.dataTransfer.types.includes('Files')) {
      setIsDragging(true);
    }
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    dragCounter.current -= 1;
    if (dragCounter.current <= 0) {
      dragCounter.current = 0;
      setIsDragging(false);
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    e.dataTransfer.dropEffect = 'copy';
  };

  const handleDrop = async (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    dragCounter.current = 0;
    setIsDragging(false);

    const droppedFiles = Array.from(e.dataTransfer.files ?? []);
    const videoFiles = droppedFiles.filter(file => file.type.startsWith("video/"));
    for (const file of videoFiles) {
      uploadVideo(file);
    }
  };

  return (
    <div
      className={`${styles.dragAndDropZone} ${isDragging ? styles.dragging : ''}`}
      onDrop={handleDrop}
      onDragOver={handleDragOver}
      onDragEnter={handleDragEnter}
      onDragLeave={handleDragLeave}
    >
      <p className={styles.dragText}>
        {isDragging ? "Да-да сюда" : "Перетащите сюда файлы"}
      </p>
    </div>
  );
};

export default DragAndDropZone;
