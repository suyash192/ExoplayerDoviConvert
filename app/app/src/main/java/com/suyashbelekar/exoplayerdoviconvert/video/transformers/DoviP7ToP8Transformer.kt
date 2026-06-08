package com.suyashbelekar.exoplayerdoviconvert.video.transformers

import com.suyashbelekar.libdovi.libDovi

/**
 * Converts Dolby Vision Profile 7 NAL units to Profile 8.
 *
 * NAL unit type 62 (which contains Dovi metadata) is converted using libdovi (mode 2). All other
 * NAL unit types pass through unmodified.
 *
 * @throws IllegalArgumentException if libdovi fails to parse, convert, or rewrite the NAL unit.
 */
class DoviP7ToP8Transformer : NaluTransformer {
    override fun transformNalu(type: Int, data: ByteArray): ByteArray {
        if (type != 62) {
            return data
        }

        val rpuPtr = libDovi.parseUnspec62Nalu(data)
        if (rpuPtr == 0L) {
            throw IllegalArgumentException("Failed to parse Dolby Vision RPU.")
        }

        val result = libDovi.convertRpuWithMode(rpuPtr, 2)
        if (!result) {
            throw IllegalArgumentException("Failed to convert Dolby Vision RPU.")
        }

        val newNalu = libDovi.writeUnspec62Nalu(rpuPtr)
            ?: throw IllegalArgumentException("Failed to write Dolby Vision RPU.")

        libDovi.freeRpu(rpuPtr)

        return newNalu
    }
}