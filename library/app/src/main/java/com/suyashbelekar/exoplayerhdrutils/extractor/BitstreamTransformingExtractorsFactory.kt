package com.suyashbelekar.exoplayerhdrutils.extractor

import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorInput
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.PositionHolder
import com.suyashbelekar.exoplayerhdrutils.video.transformers.TransformStrategy

@UnstableApi
class BitstreamTransformingExtractorsFactory(
    private val transformStrategy: TransformStrategy,
    private val defaultFactory: ExtractorsFactory = DefaultExtractorsFactory()
) : ExtractorsFactory {
    override fun createExtractors(): Array<Extractor> {
        return defaultFactory.createExtractors().map { realExtractor ->
            object : Extractor {
                override fun sniff(input: ExtractorInput) = realExtractor.sniff(input)

                override fun init(output: ExtractorOutput) {
                    realExtractor.init(
                        BitstreamModifyingExtractorOutput(
                            output,
                            transformStrategy
                        )
                    )
                }

                override fun read(input: ExtractorInput, seekPosition: PositionHolder) =
                    realExtractor.read(input, seekPosition)

                override fun seek(position: Long, timeUs: Long) =
                    realExtractor.seek(position, timeUs)

                override fun release() {
                    realExtractor.release()
                }
            }
        }.toTypedArray()
    }
}