package com.suyashbelekar.exoplayerhdrutils.exoplayer.source

import android.util.Log
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
    val delegate: SampleStream, transformStrategy: TransformStrategy
) : SampleStream by delegate {
    private val videoFrameTransformer = HevcFrameTransformer(transformStrategy)

    override fun readData(
        formatHolder: FormatHolder, buffer: DecoderInputBuffer, readFlags: Int
    ): Int {
        val result = delegate.readData(formatHolder, buffer, readFlags)

        if (result == C.RESULT_FORMAT_READ) {
            // If it is dovi p7 format, change it to dovi p8
            val format = formatHolder.format
            val codecs = format?.codecs

            Log.i(TAG, "Received codecs from video: $codecs")

            if (format != null && codecs != null && codecs.contains(".07.")) {
                Log.i(TAG, "Changing dovi P7 codec to dovi P8 codec")

                val p8Codecs = codecs.replace(".07.", ".08.")
                formatHolder.format = format.buildUpon()
                    .setCodecs(p8Codecs)
                    .build()

                Log.i(TAG, "New codecs: $p8Codecs")
            }
        } else if (result == C.RESULT_BUFFER_READ && !buffer.isEndOfStream) {
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

    companion object {
        val TAG = HdrCompatSampleStream::class.simpleName
    }
}
