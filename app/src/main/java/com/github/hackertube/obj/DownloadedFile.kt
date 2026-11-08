package com.github.hackertube.obj

import android.graphics.Bitmap
import com.github.hackertube.api.obj.Streams

data class DownloadedFile(
    val name: String,
    val size: Long,
    var metadata: Streams? = null,
    var thumbnail: Bitmap? = null
)
