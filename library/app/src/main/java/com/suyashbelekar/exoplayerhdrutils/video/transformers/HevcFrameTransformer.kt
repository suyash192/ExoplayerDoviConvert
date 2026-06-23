package com.suyashbelekar.exoplayerhdrutils.video.transformers

import android.util.Log
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
        Log.i(TAG, "Transform strategy: $transformStrategy")
        Log.i(TAG, "Analyzing first video frame to decide needed transformations")

        val frameInfo = libDovi.getFrameInfo(frameData, frameSize)
            ?: throw IllegalArgumentException("Unable to parse Dolby Vision RPU")

        Log.i(TAG, "Video Frame Info: $frameInfo")

        val doviTransform = if (frameInfo.doviProfile == 7) {
            val doviP7Strategy =
                if (frameInfo.doviElType == ElType.FEL) transformStrategy.doviP7Fel else transformStrategy.doviP7Mel

            when (doviP7Strategy) {
                DoviStrategy.CONVERT_TO_P8 -> {
                    Log.i(TAG, "Transforming dolby vision RPU to profile 8 in video frames")
                    1
                }

                DoviStrategy.DISCARD -> {
                    Log.i(TAG, "Discarding dolby vision RPU in video frames")
                    2
                }

                DoviStrategy.KEEP -> {
                    Log.i(TAG, "Keeping dolby vision RPU intact in video frames")
                    0
                }
            }
        } else {
            Log.i(TAG, "Keeping dolby vision RPU intact in video frames")
            0
        }

        val needsHdr10PlusStrip = if (frameInfo.hasHdr10Plus) {
            if (
                frameInfo.doviProfile != 0
                && transformStrategy.doviHdr10Plus == Hdr10PlusStrategy.DISCARD
            ) {
                Log.i(TAG, "Discarding HDR10+ metadata in video frames")
                true
            } else {
                Log.i(TAG, "Keeping HDR10+ metadata intact in video frames")
                false
            }
        } else {
            false
        }


        transform = if (doviTransform == 0 && !needsHdr10PlusStrip) {
            Log.i(TAG, "No bitstream transformations needed")
            Transform.NotNeeded
        } else {
            Transform.Needed(doviTransform, needsHdr10PlusStrip)
        }
    }

    fun clearContext() {
        transform = Transform.Unknown
    }

    companion object {
        val TAG = HevcFrameTransformer::class.simpleName
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