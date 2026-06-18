package com.suyashbelekar.exoplayerhdrutils.extractor

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.SeekMap
import androidx.media3.extractor.TrackOutput
import com.suyashbelekar.exoplayerhdrutils.video.transformers.TransformStrategy

@UnstableApi
class BitstreamModifyingExtractorOutput(
    private val delegate: ExtractorOutput,
    private val transformStrategy: TransformStrategy
) : ExtractorOutput {

    override fun track(id: Int, type: Int): TrackOutput {
        val realTrackOutput = delegate.track(id, type)
        return if (type == C.TRACK_TYPE_VIDEO) {
            BitstreamModifyingTrackOutput(realTrackOutput, transformStrategy)
        } else {
            realTrackOutput
        }
    }

    override fun endTracks() {
        delegate.endTracks()
    }

    override fun seekMap(seekMap: SeekMap) {
        delegate.seekMap(seekMap)
    }
}