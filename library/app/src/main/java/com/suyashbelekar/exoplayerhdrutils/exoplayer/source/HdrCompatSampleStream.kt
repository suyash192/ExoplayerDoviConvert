package com.suyashbelekar.exoplayerhdrutils.exoplayer.source

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.decoder.DecoderInputBuffer
import androidx.media3.exoplayer.FormatHolder
import androidx.media3.exoplayer.source.SampleStream
import com.suyashbelekar.exoplayerhdrutils.video.transformers.HevcFrameTransformer
import com.suyashbelekar.exoplayerhdrutils.video.transformers.TransformStrategy
import java.nio.ByteBuffer

@UnstableApi
class HdrCompatSampleStream(
    private val delegate: SampleStream, transformStrategy: TransformStrategy
) : SampleStream by delegate {
    private val videoFrameTransformer = HevcFrameTransformer(transformStrategy)

    override fun readData(
        formatHolder: FormatHolder, buffer: DecoderInputBuffer, readFlags: Int
    ): Int {
        val result = delegate.readData(formatHolder, buffer, readFlags)

        if (result == C.RESULT_BUFFER_READ && !buffer.isEndOfStream) {
            onBuffer(buffer)
        }

        return result
    }

    private fun onBuffer(buffer: DecoderInputBuffer) {
        val byteBuffer: ByteBuffer = buffer.data ?: return

        val frameSize = byteBuffer.position()

        // Add 10 KB space in buffer just in case if conversion increases the frame size
        buffer.ensureSpaceForWrite(frameSize + (1024 * 10))

        // Get the new buffer in case it was created
        val newByteBuffer: ByteBuffer = buffer.data ?: return

        val newSize = videoFrameTransformer.transformFrame(newByteBuffer, frameSize)

        byteBuffer.position(newSize)
    }
}
