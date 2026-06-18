package com.suyashbelekar.exoplayerhdrutils.video.transformers

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer

@RunWith(AndroidJUnit4::class)
class HevcFrameTransformerInstrumentedTest {
    // --- Unchanged Files Tests ---

    @Test
    fun unchangedFiles_shouldRemainIdentical_keepStrategy() {
        runUnchangedFilesTest(
            TransformStrategy(
                doviP7Fel = DoviStrategy.KEEP,
                doviP7Mel = DoviStrategy.KEEP,
                doviHdr10Plus = Hdr10PlusStrategy.KEEP
            )
        )
    }

    @Test
    fun unchangedFiles_shouldRemainIdentical_discardStrategy() {
        runUnchangedFilesTest(
            TransformStrategy(
                doviP7Fel = DoviStrategy.DISCARD,
                doviP7Mel = DoviStrategy.DISCARD,
                doviHdr10Plus = Hdr10PlusStrategy.DISCARD
            )
        )
    }

    @Test
    fun unchangedFiles_shouldRemainIdentical_convertToP8Strategy() {
        runUnchangedFilesTest(
            TransformStrategy(
                doviP7Fel = DoviStrategy.CONVERT_TO_P8,
                doviP7Mel = DoviStrategy.CONVERT_TO_P8,
                doviHdr10Plus = Hdr10PlusStrategy.DISCARD
            )
        )
    }

    // --- Dolby Vision Profile 7 FEL Tests ---

    @Test
    fun dvP7Fel_convertToP8_matchesExpectedFile() {
        val strategy = TransformStrategy(
            doviP7Fel = DoviStrategy.CONVERT_TO_P8,
            doviP7Mel = DoviStrategy.KEEP,
            doviHdr10Plus = Hdr10PlusStrategy.KEEP
        )
        runTransformTest(
            inputAsset = "input-dvP7Fel.hevc",
            expectedAsset = "expected-dvP8-from-input-dvP7Fel.hevc",
            strategy = strategy
        )
    }

    @Test
    fun dvP7Fel_discard_matchesExpectedFile() {
        val strategy = TransformStrategy(
            doviP7Fel = DoviStrategy.DISCARD,
            doviP7Mel = DoviStrategy.KEEP,
            doviHdr10Plus = Hdr10PlusStrategy.KEEP
        )
        runTransformTest(
            inputAsset = "input-dvP7Fel.hevc",
            expectedAsset = "expected-discard-from-input-dvP7Fel.hevc",
            strategy = strategy
        )
    }

    @Test
    fun dvP7Fel_keep_matchesExpectedFile() {
        val strategy = TransformStrategy(
            doviP7Fel = DoviStrategy.KEEP,
            doviP7Mel = DoviStrategy.KEEP,
            doviHdr10Plus = Hdr10PlusStrategy.KEEP
        )
        runTransformTest(
            inputAsset = "input-dvP7Fel.hevc",
            expectedAsset = "input-dvP7Fel.hevc",
            strategy = strategy
        )
    }

    // --- Dolby Vision Profile 7 MEL Tests ---

    @Test
    fun dvP7Mel_convertToP8_matchesExpectedFile() {
        val strategy = TransformStrategy(
            doviP7Fel = DoviStrategy.KEEP,
            doviP7Mel = DoviStrategy.CONVERT_TO_P8,
            doviHdr10Plus = Hdr10PlusStrategy.KEEP
        )
        runTransformTest(
            inputAsset = "input-dvP7Mel.hevc",
            expectedAsset = "expected-dvP8-from-input-dvP7Mel.hevc",
            strategy = strategy
        )
    }

    @Test
    fun dvP7Mel_discard_matchesExpectedFile() {
        val strategy = TransformStrategy(
            doviP7Fel = DoviStrategy.KEEP,
            doviP7Mel = DoviStrategy.DISCARD,
            doviHdr10Plus = Hdr10PlusStrategy.KEEP
        )
        runTransformTest(
            inputAsset = "input-dvP7Mel.hevc",
            expectedAsset = "expected-discard-from-input-dvP7Mel.hevc",
            strategy = strategy
        )
    }

    @Test
    fun dvP7Mel_keep_matchesExpectedFile() {
        val strategy = TransformStrategy(
            doviP7Fel = DoviStrategy.KEEP,
            doviP7Mel = DoviStrategy.KEEP,
            doviHdr10Plus = Hdr10PlusStrategy.KEEP
        )
        runTransformTest(
            inputAsset = "input-dvP7Mel.hevc",
            expectedAsset = "input-dvP7Mel.hevc",
            strategy = strategy
        )
    }

    // --- HDR10+ tests ---

    @Test
    fun dvP8Hdr10Plus_discard_matchesExpectedFile() {
        val strategy = TransformStrategy(
            doviP7Fel = DoviStrategy.KEEP,
            doviP7Mel = DoviStrategy.KEEP,
            doviHdr10Plus = Hdr10PlusStrategy.DISCARD
        )
        runTransformTest(
            inputAsset = "input-dvP8Hdr10Plus.hevc",
            expectedAsset = "expected-discard-from-input-dvP8Hdr10Plus.hevc",
            strategy = strategy
        )
    }

    @Test
    fun dvP8Hdr10Plus_keep_matchesExpectedFile() {
        val strategy = TransformStrategy(
            doviP7Fel = DoviStrategy.KEEP,
            doviP7Mel = DoviStrategy.KEEP,
            doviHdr10Plus = Hdr10PlusStrategy.KEEP
        )
        runTransformTest(
            inputAsset = "input-dvP8Hdr10Plus.hevc",
            expectedAsset = "input-dvP8Hdr10Plus.hevc",
            strategy = strategy
        )
    }

    // --- Helper Methods ---

    fun runUnchangedFilesTest(transformStrategy: TransformStrategy) {
        val unchangedFiles = listOf("dvP5.hevc", "dvP8.hevc", "hdr10.hevc", "sdr.hevc")

        for (fileName in unchangedFiles) {
            runTransformTest(
                inputAsset = fileName,
                expectedAsset = fileName,
                strategy = transformStrategy
            )
        }
    }

    private fun runTransformTest(
        inputAsset: String,
        expectedAsset: String,
        strategy: TransformStrategy
    ) {
        val transformer = HevcFrameTransformer(strategy)

        // Read input bytes
        val inputBytes = readAssetToByteArray(inputAsset)
        val originalSize = inputBytes.size

        buffer.clear()
        buffer.put(inputBytes)

        // Transform
        val newSize = transformer.transformFrame(buffer, originalSize)

        // Verify
        val expectedBytes = readAssetToByteArray(expectedAsset)

        assertEquals(
            "Frame size mismatch for conversion: $inputAsset -> $expectedAsset",
            expectedBytes.size,
            newSize
        )

        buffer.position(0)
        val actualBytes = ByteArray(newSize)
        buffer.get(actualBytes)

        assertArrayEquals(
            "Frame data mismatch for conversion: $inputAsset -> $expectedAsset",
            expectedBytes,
            actualBytes
        )
    }

    private fun readAssetToByteArray(assetName: String): ByteArray {
        val context = InstrumentationRegistry.getInstrumentation().context
        return context.assets.open(assetName).use { it.readBytes() }
    }

    companion object {
        // Sufficiently large buffer for the entire test suite.
        private const val MAX_BUFFER_SIZE = 5 * 1024 * 1024
        private val buffer: ByteBuffer = ByteBuffer.allocateDirect(MAX_BUFFER_SIZE)
    }
}