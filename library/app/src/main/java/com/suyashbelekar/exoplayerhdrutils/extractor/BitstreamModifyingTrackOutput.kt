package com.suyashbelekar.exoplayerhdrutils.extractor

import androidx.media3.common.C
import androidx.media3.common.DataReader
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.TrackOutput
import com.suyashbelekar.exoplayerhdrutils.video.transformers.HevcFrameTransformer
import com.suyashbelekar.exoplayerhdrutils.video.transformers.TransformStrategy
import java.io.EOFException
import java.nio.ByteBuffer

@UnstableApi
class BitstreamModifyingTrackOutput(
    private val delegate: TrackOutput,
    transformStrategy: TransformStrategy
) : TrackOutput {
    private val hevcFrameTransformer = HevcFrameTransformer(transformStrategy)

    // To hold the frame, it will auto-expand if needed
    private var buffer: ByteBuffer = ByteBuffer.allocateDirect(1024 * 1024)

    // Reusable array to read from DataReader without allocating on every call
    private var tempBuffer = ByteArray(4096)

    // Reusable array and wrapper to pass the final transformed frame to ExoPlayer
    private var frameOutputBuffer = ByteArray(1024 * 1024)
    private val parsableByteArray = ParsableByteArray()

    private var isDolbyVision = false

    override fun format(format: Format) {
        hevcFrameTransformer.clearContext()
        isDolbyVision = format.sampleMimeType == MimeTypes.VIDEO_DOLBY_VISION
        delegate.format(format)
    }

    override fun sampleData(
        input: DataReader, length: Int, allowEndOfInput: Boolean, sampleDataPart: Int
    ): Int {
        if (!isDolbyVision) {
            return delegate.sampleData(input, length, allowEndOfInput, sampleDataPart)
        }

        tempBuffer = ensureCapacity(tempBuffer, length)
        val bytesRead = input.read(tempBuffer, 0, length)

        if (bytesRead == -1) {
            if (allowEndOfInput) return C.RESULT_END_OF_INPUT
            throw EOFException()
        }

        buffer = ensureCapacity(buffer, buffer.position() + bytesRead)
        buffer.put(tempBuffer, 0, bytesRead)

        return bytesRead
    }

    override fun sampleData(data: ParsableByteArray, length: Int, sampleDataPart: Int) {
        if (!isDolbyVision) {
            delegate.sampleData(data, length, sampleDataPart)
            return
        }

        buffer = ensureCapacity(buffer, buffer.position() + length)
        buffer.put(data.data, data.position, length)
        data.skipBytes(length)
    }

    override fun sampleMetadata(
        timeUs: Long, flags: Int, size: Int, offset: Int, cryptoData: TrackOutput.CryptoData?
    ) {
        if (!isDolbyVision) {
            delegate.sampleMetadata(timeUs, flags, size, offset, cryptoData)
            return
        }

        // Read mode
        buffer.flip()
        val currentLength = buffer.remaining()

        // Transform the frame
        val newSize = hevcFrameTransformer.transformFrame(buffer, currentLength)

        frameOutputBuffer = ensureCapacity(frameOutputBuffer, newSize)

        // Extract to output array
        buffer.position(0)
        buffer.get(frameOutputBuffer, 0, newSize)

        buffer.clear()

        // Pass output
        parsableByteArray.reset(frameOutputBuffer, newSize)

        delegate.sampleData(
            parsableByteArray, newSize, TrackOutput.SAMPLE_DATA_PART_MAIN
        )
        delegate.sampleMetadata(timeUs, flags, newSize, offset, cryptoData)
    }

    private fun ensureCapacity(currentBuffer: ByteBuffer, requiredCapacity: Int): ByteBuffer {
        if (currentBuffer.capacity() >= requiredCapacity) {
            return currentBuffer
        }

        val newBuffer = ByteBuffer.allocateDirect(requiredCapacity * 2)
        currentBuffer.flip()
        newBuffer.put(currentBuffer)

        return newBuffer
    }

    private fun ensureCapacity(currentBuffer: ByteArray, requiredCapacity: Int): ByteArray {
        if (currentBuffer.size >= requiredCapacity) {
            return currentBuffer
        }

        return ByteArray(requiredCapacity * 2)
    }
}