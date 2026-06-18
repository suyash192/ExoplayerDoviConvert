package com.suyashbelekar.exoplayerhdrutils.video.transformers

import com.suyashbelekar.exoplayerhdrutils.libdovi.ElType
import com.suyashbelekar.exoplayerhdrutils.libdovi.LibDovi
import java.nio.ByteBuffer

class HevcFrameTransformer(
    private val transformStrategy: TransformStrategy
) {
    private val libDovi = LibDovi()

    private var transform: Transform = Transform.Unknown

    fun transformFrame(frameData: ByteBuffer, frameSize: Int): Int {
        return when (transform) {
            is Transform.Needed -> {
                val transform = transform as Transform.Needed
                libDovi.processHevcFrame(
                    frameData,
                    frameSize,
                    transform.doviTransform,
                    transform.needsHdr10PlusStrip
                )
            }

            Transform.NotNeeded -> frameSize

            Transform.Unknown -> {
                analyzeFirstFrame(frameData, frameSize)
                transformFrame(frameData, frameSize)
            }
        }
    }

    private fun analyzeFirstFrame(frameData: ByteBuffer, frameSize: Int) {
        val frameInfo = libDovi.getFrameInfo(frameData, frameSize)
            ?: throw IllegalArgumentException("Unable to parse Dolby Vision RPU")

        val doviTransform = if (frameInfo.doviProfile == 7) {
            val doviP7Strategy =
                if (frameInfo.doviElType == ElType.FEL) transformStrategy.doviP7Fel else transformStrategy.doviP7Mel

            when (doviP7Strategy) {
                DoviStrategy.CONVERT_TO_P8 -> 1
                DoviStrategy.DISCARD -> 2
                DoviStrategy.KEEP -> 0
            }
        } else {
            0
        }

        val needsHdr10PlusStrip = frameInfo.doviProfile != 0 && frameInfo.hasHdr10Plus
                && transformStrategy.doviHdr10Plus == Hdr10PlusStrategy.DISCARD

        transform = if (doviTransform == 0 && !needsHdr10PlusStrip) {
            Transform.NotNeeded
        } else {
            Transform.Needed(doviTransform, needsHdr10PlusStrip)
        }
    }

    fun clearContext() {
        transform = Transform.Unknown
    }
}

private sealed class Transform {
    object Unknown : Transform()
    object NotNeeded : Transform()

    data class Needed(
        val doviTransform: Int,
        val needsHdr10PlusStrip: Boolean
    ) : Transform()
}