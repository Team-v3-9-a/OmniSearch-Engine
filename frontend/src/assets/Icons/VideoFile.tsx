interface IconProps {
  className: string;
}

export const VideoFileIcon = ({ className }: IconProps) => {
  return (
    <svg className={className} width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <g clipPath="url(#clip0_2497_26595)">
        <path d="M7 21C5.89543 21 5 20.1046 5 19V3H14L19 8V19C19 20.1046 18.1046 21 17 21H7Z" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        <path d="M13 3V9H19" stroke="white" strokeWidth="2" strokeLinejoin="round" />
        <path d="M14 14.5L11 16.2321L11 12.7679L14 14.5Z" stroke="white" strokeWidth="2" strokeLinejoin="round" />
      </g>
      <defs>
        <clipPath id="clip0_2497_26595">
          <rect width="24" height="24" fill="white" />
        </clipPath>
      </defs>
    </svg>
  )
}