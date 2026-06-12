package com.suyashbelekar.exoplayerhdrutils.exoplayer

import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.MediaPeriod
import androidx.media3.exoplayer.source.SampleStream
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import com.suyashbelekar.exoplayerhdrutils.video.transformers.TransformStrategy

@UnstableApi
class HdrCompatMediaPeriod(
    internal val delegate: MediaPeriod,
    private val transformStrategy: TransformStrategy
) : MediaPeriod by delegate {

    override fun selectTracks(
        selections: Array<ExoTrackSelection?>,
        mayRetainStreamFlags: BooleanArray,
        streams: Array<SampleStream?>,
        streamResetFlags: BooleanArray,
        positionUs: Long
    ): Long {
        val position = delegate.selectTracks(
            selections, mayRetainStreamFlags, streams, streamResetFlags, positionUs
        )

        for (i in streams.indices) {
            val stream = streams[i]
            val selection = selections[i]

            if (stream != null && stream !is HdrCompatSampleStream && selection != null) {
                val mimeType = selection.trackGroup.getFormat(0).sampleMimeType
                if (mimeType != null && mimeType == MimeTypes.VIDEO_DOLBY_VISION) {
                    streams[i] = HdrCompatSampleStream(stream, transformStrategy)
                }
            }
        }

        return position
    }
}