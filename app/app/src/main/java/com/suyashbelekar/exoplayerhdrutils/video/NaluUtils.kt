package com.suyashbelekar.exoplayerhdrutils.video

object NaluUtils {
    fun parseHevcNalus(
        frameData: ByteArray,
        onData: (type: Int, data: ByteArray, startCodeSize: Int) -> Unit
    ) {
        var zeroCount = 0
        var dataStartIndex = 0
        var prevStartCodeSize = 0

        for ((i, byte) in frameData.withIndex()) {
            when (byte) {
                0.toByte() -> {
                    if (zeroCount < 3) {
                        zeroCount++
                    }
                }

                1.toByte() -> {
                    if (zeroCount >= 2) {
                        // Start code found
                        val dataEndIndex = i - zeroCount

                        extractNalu(
                            frameData,
                            dataStartIndex,
                            dataEndIndex,
                            prevStartCodeSize,
                            onData
                        )

                        prevStartCodeSize = zeroCount + 1
                        zeroCount = 0
                        dataStartIndex = i + 1
                    } else {
                        zeroCount = 0
                    }
                }

                else -> {
                    zeroCount = 0
                }
            }
        }

        extractNalu(frameData, dataStartIndex, frameData.size, prevStartCodeSize, onData)
    }

    private fun extractNalu(
        frameData: ByteArray, startIndex: Int, endIndex: Int, prevStartCodeSize: Int,
        onData: (type: Int, data: ByteArray, startCodeSize: Int) -> Unit
    ) {
        if (endIndex > startIndex) {
            val naluData = frameData.copyOfRange(startIndex, endIndex)
            val naluType = if (naluData.isNotEmpty()) getType(naluData) else -1

            onData(naluType, naluData, prevStartCodeSize)
        }
    }

    private fun getType(naluData: ByteArray): Int {
        val firstByte = naluData[0].toInt()

        // NALU type is in bits 1-6
        return (firstByte and 0x7E) ushr 1
    }
}