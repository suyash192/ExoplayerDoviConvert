package com.suyashbelekar.exoplayerhdrutils.exoplayer.source

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import com.suyashbelekar.exoplayerhdrutils.video.transformers.TransformStrategy

/**
 * A [MediaSource.Factory] implementation that generates [HdrCompatMediaSource] instances.
 *
 * @param delegate The underlying [MediaSource.Factory] responsible for creating the base media sources.
 * @param transformStrategy The [TransformStrategy] dictating how the HDR media should be transformed.
 */
@UnstableApi
class HdrCompatMediaSourceFactory(
    private val delegate: MediaSource.Factory,
    private val transformStrategy: TransformStrategy
) : MediaSource.Factory by delegate {

    /**
     * Creates an [HdrCompatMediaSourceFactory] using default components.
     *
     * This convenience constructor automatically instantiates a [DefaultMediaSourceFactory]
     * to act as the underlying base factory.
     *
     * @param context The application or activity [Context].
     * @param transformStrategy The [TransformStrategy] dictating how the HDR media should be transformed.
     */
    constructor(context: Context, transformStrategy: TransformStrategy) : this(
        DefaultMediaSourceFactory(context), transformStrategy
    )

    override fun createMediaSource(mediaItem: MediaItem): MediaSource {
        val standardSource = delegate.createMediaSource(mediaItem)

        return HdrCompatMediaSource(standardSource, transformStrategy)
    }
}