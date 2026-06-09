package com.suyashbelekar.exoplayerhdrutils

import androidx.media3.common.C
import androidx.media3.common.DataReader
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorInput
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.PositionHolder
import androidx.media3.extractor.SeekMap
import androidx.media3.extractor.TrackOutput
import com.suyashbelekar.exoplayerhdrutils.video.transformers.TransformStrategy
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

@UnstableApi
class BitstreamModifyingExtractorOutput(
    private val delegate: ExtractorOutput,
    private val videoFrameTransformer: VideoFrameTransformer
) : ExtractorOutput {

    override fun track(id: Int, type: Int): TrackOutput {
        val realTrackOutput = delegate.track(id, type)
        return if (type == C.TRACK_TYPE_VIDEO) {
            BitstreamModifyingTrackOutput(realTrackOutput, videoFrameTransformer)
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

@UnstableApi
class BitstreamTransformingExtractorsFactory(
    transformStrategy: TransformStrategy,
    private val defaultFactory: ExtractorsFactory = DefaultExtractorsFactory()
    ) : ExtractorsFactory {
    private val videoFrameTransformer = VideoFrameTransformer(transformStrategy)

    override fun createExtractors(): Array<Extractor> {
        return defaultFactory.createExtractors().map { realExtractor ->
            object : Extractor {
                override fun sniff(input: ExtractorInput) = realExtractor.sniff(input)

                override fun init(output: ExtractorOutput) {
                    realExtractor.init(
                        BitstreamModifyingExtractorOutput(
                            output,
                            videoFrameTransformer
                        )
                    )
                }

                override fun read(input: ExtractorInput, seekPosition: PositionHolder) =
                    realExtractor.read(input, seekPosition)

                override fun seek(position: Long, timeUs: Long) =
                    realExtractor.seek(position, timeUs)

                override fun release() {
                    realExtractor.release()
                    videoFrameTransformer.clearContext()
                }
            }
        }.toTypedArray()
    }
}