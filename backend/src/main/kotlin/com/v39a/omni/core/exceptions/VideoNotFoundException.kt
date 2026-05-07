package com.v39a.omni.core.exceptions

import java.util.UUID

class VideoNotFoundException(id: UUID) : Exception() {
    override val message: String = "Video (id=$id) not found"
}