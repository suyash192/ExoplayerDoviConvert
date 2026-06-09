package com.suyashbelekar.exoplayerhdrutils.extractor

import androidx.media3.common.C
import androidx.media3.common.DataReader
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.TrackOutput
import com.suyashbelekar.exoplayerhdrutils.video.transformers.VideoFrameTransformer
import java.io.ByteArrayOutputStream
import java.io.EOFException

@UnstableApi
class BitstreamModifyingTrackOutput(
    private val delegate: TrackOutput,
    private val videoFrameTransformer: VideoFrameTransformer
) : TrackOutput {
    private val frameBuffer = ByteArrayOutputStream()

    private var isDolbyVision = false

    override fun format(format: Format) {
        isDolbyVision = format.sampleMimeType == MimeTypes.VIDEO_DOLBY_VISION
        delegate.format(format)
    }

    override fun sampleData(
        input: DataReader, length: Int, allowEndOfInput: Boolean, sampleDataPart: Int
    ): Int {
        if (!isDolbyVision) {
            return delegate.sampleData(input, length, allowEndOfInput, sampleDataPart)
        }

        val buffer = ByteArray(length)
        val bytesRead = input.read(buffer, 0, length)
        if (bytesRead == -1) {
            if (allowEndOfInput) return C.RESULT_END_OF_INPUT
            throw EOFException()
        }
        frameBuffer.write(buffer, 0, bytesRead)
        return bytesRead
    }

    override fun sampleData(data: ParsableByteArray, length: Int, sampleDataPart: Int) {
        if (!isDolbyVision) {
            delegate.sampleData(data, length, sampleDataPart)
            return
        }

        val bytes = ByteArray(length)
        data.readBytes(bytes, 0, length)
        frameBuffer.write(bytes)
    }

    override fun sampleMetadata(
        timeUs: Long, flags: Int, size: Int, offset: Int, cryptoData: TrackOutput.CryptoData?
    ) {
        if (!isDolbyVision) {
            delegate.sampleMetadata(timeUs, flags, size, offset, cryptoData)
            return
        }

        val fullFrameBytes = frameBuffer.toByteArray()
        frameBuffer.reset()

        val modifiedBytes = videoFrameTransformer.transformFrame(fullFrameBytes)

        val parsableByteArray = ParsableByteArray(modifiedBytes)
        delegate.sampleData(
            parsableByteArray, modifiedBytes.size, TrackOutput.SAMPLE_DATA_PART_MAIN
        )
        delegate.sampleMetadata(timeUs, flags, modifiedBytes.size, offset, cryptoData)
    }
}

