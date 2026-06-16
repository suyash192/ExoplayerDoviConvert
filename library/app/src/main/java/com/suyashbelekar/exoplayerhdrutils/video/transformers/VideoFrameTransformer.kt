package com.suyashbelekar.exoplayerhdrutils.video.transformers

import com.suyashbelekar.exoplayerhdrutils.libdovi.ElType
import com.suyashbelekar.exoplayerhdrutils.libdovi.LibDovi
import com.suyashbelekar.exoplayerhdrutils.video.NaluUtils
import java.io.ByteArrayOutputStream

class VideoFrameTransformer(
    private val transformStrategy: TransformStrategy
) {
    private val libDovi = LibDovi()

    private var firstFrameDone = false

    private val naluTransformers = mutableListOf<NaluTransformer>()

    private val startCode3 = byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x01.toByte())
    private val startCode4 = byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte())

    fun transformFrame(frameData: ByteArray): ByteArray {
        if (!firstFrameDone) {
            populateTransformers(frameData)
            firstFrameDone = true
        }

        if (naluTransformers.isEmpty()) {
            return frameData
        }

        val outputStream = ByteArrayOutputStream()

        NaluUtils.parseHevcNalus(frameData) { type, data, startCodeSize: Int ->
            val transformedData = naluTransformers.fold(data) { data, transformer ->
                transformer.transformNalu(type, data)
            }

            if (transformedData.isNotEmpty()) {
                when (startCodeSize) {
                    3 -> outputStream.write(startCode3)
                    4 -> outputStream.write(startCode4)
                }
                outputStream.write(transformedData)
            }
        }

        return outputStream.toByteArray()
    }

    private fun populateTransformers(frameData: ByteArray) {
        val (doviProfile, elType, hasHdr10Plus) = getFrameHdrInfo(frameData)

        if (doviProfile == 7) {
            val doviP7Strategy =
                if (elType == ElType.FEL) transformStrategy.doviP7Fel else transformStrategy.doviP7Mel

            when (doviP7Strategy) {
                DoviStrategy.CONVERT_TO_P8 -> naluTransformers.add(DoviP7ToP8Transformer())
                DoviStrategy.DISCARD -> naluTransformers.add(DoviFilter())
                DoviStrategy.KEEP -> {}
            }
        }

        if (doviProfile != 0 && hasHdr10Plus && transformStrategy.doviHdr10Plus == Hdr10PlusStrategy.DISCARD) {
            naluTransformers.add(Hdr10PlusFilter())
        }
    }

    private fun getFrameHdrInfo(frameData: ByteArray): Triple<Int, ElType, Boolean> {
        var doviProfile = 0
        var elType: ElType = ElType.NONE
        var hasHdr10Plus = false

        NaluUtils.parseHevcNalus(frameData) { naluType, naluData, _ ->
            if (naluType == 62) {
                val rpuPtr = libDovi.parseUnspec62Nalu(naluData)
                if (rpuPtr == 0L) {
                    throw IllegalArgumentException("Failed to parse Dolby Vision RPU.")
                }

                doviProfile = libDovi.getDoviProfile(rpuPtr)
                elType = ElType.valueOf(libDovi.getElType(rpuPtr) ?: "NONE")
            } else if (naluType == 39 && containsHdr10PlusSignature(naluData)) {
                hasHdr10Plus = true
            }
        }

        return Triple(doviProfile, elType, hasHdr10Plus)
    }

    private fun containsHdr10PlusSignature(naluData: ByteArray): Boolean {
        if (naluData.size < 6) return false

        // The HDR10+ signature: { 0xB5, 0x00, 0x3C, 0x00, 0x01, 0x04 }
        val sig0 = 0xB5.toByte()
        val sig1 = 0x00.toByte()
        val sig2 = 0x3C.toByte()
        val sig3 = 0x00.toByte()
        val sig4 = 0x01.toByte()
        val sig5 = 0x04.toByte()

        // Slide a 6-byte window across the array
        for (i in 0..naluData.size - 6) {
            // Fast-fail on the first byte, then check the rest
            if (naluData[i] == sig0 && naluData[i + 1] == sig1 && naluData[i + 2] == sig2 && naluData[i + 3] == sig3 && naluData[i + 4] == sig4 && naluData[i + 5] == sig5) {
                return true
            }
        }

        return false
    }

    fun clearContext() {
        naluTransformers.clear()
        firstFrameDone = false
    }
}

