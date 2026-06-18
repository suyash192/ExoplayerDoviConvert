package com.suyashbelekar.exoplayerhdrutils.libdovi

import java.nio.ByteBuffer

class LibDovi {
    init {
        System.loadLibrary("dovi_android")
    }

    private external fun getRpuFrameInfo(rpuBuffer: ByteBuffer, frameSize: Int): IntArray?

    fun getFrameInfo(frameBuffer: ByteBuffer, frameSize: Int): FrameInfo? {
        val result = getRpuFrameInfo(frameBuffer, frameSize) ?: return null

        val profile = result[0]
        val doviElType = when (result[1]) {
            2 -> ElType.FEL
            1 -> ElType.MEL
            else -> ElType.NONE
        }
        val hasDoviHdr10Plus = (result[2] == 1)

        return FrameInfo(profile, doviElType, hasDoviHdr10Plus)
    }

    external fun processHevcFrame(
        frameData: ByteBuffer,
        frameSize: Int,
        doviTransform: Int,
        needsHdr10PlusStrip: Boolean
    ): Int
}

data class FrameInfo(
    val doviProfile: Int,
    val doviElType: ElType,
    val hasHdr10Plus: Boolean,
)

enum class ElType {
    MEL, FEL, NONE
}