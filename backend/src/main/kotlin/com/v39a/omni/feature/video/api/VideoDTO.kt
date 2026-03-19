package com.v39a.omni.feature.video.api

import okhttp3.MultipartBody

data class VideoDTO(val name: String, val path: String, val content: MultipartBody.Part)