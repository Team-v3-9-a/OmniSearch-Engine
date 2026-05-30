import type { UploadStatus } from "@/types/api"
import { getStatusText } from "@/utils/statusText"
import styles from './StatusLabel.module.css'

interface StatusLabelProps {
  status: UploadStatus;
}

export const StatusLabel = ({ status }: StatusLabelProps) => {
  return (
    <span className={`${styles.status} ${styles[`${status.toLowerCase()}`]}`}>
      {getStatusText(status)}
    </span>
  )
}