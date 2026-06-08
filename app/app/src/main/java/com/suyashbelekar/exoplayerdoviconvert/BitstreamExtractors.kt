package com.suyashbelekar.exoplayerdoviconvert

import androidx.media3.common.C
import androidx.media3.common.DataReader
import androidx.media3.common.Format
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
import com.suyashbelekar.exoplayerdoviconvert.video.transformers.TransformStrategy
import com.suyashbelekar.exoplayerdoviconvert.video.transformers.VideoFrameTransformer
import java.io.ByteArrayOutputStream
import java.io.EOFException

// ============================================================================
// 1. The Custom TrackOutput (Core NAL Modification Logic)
// ============================================================================
@UnstableApi
class BitstreamModifyingTrackOutput(
    private val delegate: TrackOutput,
    private val videoFrameTransformer: VideoFrameTransformer
) : TrackOutput {
    private val frameBuffer = ByteArrayOutputStream()

    override fun format(format: Format) {
        delegate.format(format)
    }

    override fun sampleData(
        input: DataReader, length: Int, allowEndOfInput: Boolean, sampleDataPart: Int
    ): Int {
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
        val bytes = ByteArray(length)
        data.readBytes(bytes, 0, length)
        frameBuffer.write(bytes)
    }

    override fun sampleMetadata(
        timeUs: Long, flags: Int, size: Int, offset: Int, cryptoData: TrackOutput.CryptoData?
    ) {
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

// ============================================================================
// 2. The Custom ExtractorOutput Wrapper
// ============================================================================
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

// ============================================================================
// 3. The Custom ExtractorsFactory Wrapper
// ============================================================================
@UnstableApi
class BitstreamTransformingExtractorsFactory(
    private val transformStrategy: TransformStrategy,
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