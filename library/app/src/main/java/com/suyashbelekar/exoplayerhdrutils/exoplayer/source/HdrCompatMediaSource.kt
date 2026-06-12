package com.suyashbelekar.exoplayerhdrutils.exoplayer.source

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaPeriod
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.WrappingMediaSource
import androidx.media3.exoplayer.upstream.Allocator
import com.suyashbelekar.exoplayerhdrutils.video.transformers.TransformStrategy


/**
 * A [MediaSource] that wraps another [MediaSource] to apply HDR compatibility transformations.
 *
 * @param delegate The underlying [MediaSource] to wrap and read data from.
 * @param transformStrategy The [TransformStrategy] dictating how the HDR media should be transformed.
 */
@UnstableApi
class HdrCompatMediaSource(
    delegate: MediaSource,
    private val transformStrategy: TransformStrategy
) : WrappingMediaSource(delegate) {

    /**
     * Creates an [HdrCompatMediaSource] using a [MediaItem].
     *
     * This convenience constructor uses a [DefaultMediaSourceFactory] to automatically
     * build the underlying delegate [MediaSource] from the provided [MediaItem].
     *
     * @param context The application or activity [Context].
     * @param mediaItem The [MediaItem] to build the underlying media source for.
     * @param transformStrategy The [TransformStrategy] dictating how the HDR media should be transformed.
     */
    constructor(
        context: Context,
        mediaItem: MediaItem,
        transformStrategy: TransformStrategy
    ) : this(
        DefaultMediaSourceFactory(context).createMediaSource(mediaItem),
        transformStrategy
    )

    override fun createPeriod(
        id: MediaSource.MediaPeriodId,
        allocator: Allocator,
        startPositionUs: Long
    ): MediaPeriod {
        val originalPeriod = super.createPeriod(id, allocator, startPositionUs)
        return HdrCompatMediaPeriod(originalPeriod, transformStrategy)
    }

    override fun releasePeriod(mediaPeriod: MediaPeriod) {
        val periodToRelease = if (mediaPeriod is HdrCompatMediaPeriod) {
            mediaPeriod.delegate
        } else {
            mediaPeriod
        }

        super.releasePeriod(periodToRelease)
    }
}

