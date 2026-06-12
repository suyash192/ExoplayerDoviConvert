package com.suyashbelekar.exoplayerhdrutils.exoplayer.source

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.decoder.DecoderInputBuffer
import androidx.media3.exoplayer.FormatHolder
import androidx.media3.exoplayer.source.SampleStream
import com.suyashbelekar.exoplayerhdrutils.video.transformers.TransformStrategy
import com.suyashbelekar.exoplayerhdrutils.video.transformers.VideoFrameTransformer
import java.nio.ByteBuffer

@UnstableApi
class HdrCompatSampleStream(
    private val delegate: SampleStream,
    transformStrategy: TransformStrategy
) : SampleStream by delegate {
    private val videoFrameTransformer = VideoFrameTransformer(transformStrategy)

    override fun readData(
        formatHolder: FormatHolder,
        buffer: DecoderInputBuffer,
        readFlags: Int
    ): Int {
        val result = delegate.readData(formatHolder, buffer, readFlags)

        if (result == C.RESULT_BUFFER_READ && !buffer.isEndOfStream) {
            onBuffer(buffer)
        }

        return result
    }

    private fun onBuffer(buffer: DecoderInputBuffer) {
        val byteBuffer: ByteBuffer? = buffer.data
        if (byteBuffer != null) {
            val frameSize = byteBuffer.position()

            byteBuffer.flip()
            val frameData = ByteArray(frameSize)
            byteBuffer.get(frameData)

            val transformedData = videoFrameTransformer.transformFrame(frameData)

            byteBuffer.clear()
            byteBuffer.put(transformedData)
            byteBuffer.position(transformedData.size)
        }
    }
}

