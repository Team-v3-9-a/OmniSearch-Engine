package com.v39a.omni.core.exceptions

import com.v39a.omni.feature.video.domain.VideoStatus
import java.util.UUID

abstract class DomainException(message: String) : Exception(message)

class VideoNotFoundException(id: UUID) :
    DomainException("Video (id=$id) not found")

class VideoNotReadyException(id: UUID, currentStatus: VideoStatus) :
    DomainException("Video (id=$id) is not ready for streaming. Current status: $currentStatus")

class VideoEngineUnavailableException(videoId: UUID) :
    DomainException("Video Engine is currently unavailable to process video (id=$videoId)")

class MLEngineUnavailableException(message: String) :
    DomainException(message)

