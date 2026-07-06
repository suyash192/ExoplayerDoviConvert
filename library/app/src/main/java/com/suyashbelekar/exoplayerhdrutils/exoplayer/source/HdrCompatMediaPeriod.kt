package com.suyashbelekar.exoplayerhdrutils.exoplayer.source

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

    // To prevent leak of delegate
    override fun prepare(callback: MediaPeriod.Callback, positionUs: Long) {
        delegate.prepare(object : MediaPeriod.Callback {

            override fun onPrepared(mediaPeriod: MediaPeriod) {
                callback.onPrepared(this@HdrCompatMediaPeriod)
            }

            override fun onContinueLoadingRequested(source: MediaPeriod) {
                callback.onContinueLoadingRequested(this@HdrCompatMediaPeriod)
            }

        }, positionUs)
    }

    override fun selectTracks(
        selections: Array<ExoTrackSelection?>,
        mayRetainStreamFlags: BooleanArray,
        streams: Array<SampleStream?>,
        streamResetFlags: BooleanArray,
        positionUs: Long
    ): Long {
        // Unwrap the delegate sample stream
        for (i in streams.indices) {
            val stream = streams[i]
            if (stream is HdrCompatSampleStream) {
                streams[i] = stream.delegate
            }
        }

        val position = delegate.selectTracks(
            selections, mayRetainStreamFlags, streams, streamResetFlags, positionUs
        )

        // Re-wrap the delegate sample stream
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