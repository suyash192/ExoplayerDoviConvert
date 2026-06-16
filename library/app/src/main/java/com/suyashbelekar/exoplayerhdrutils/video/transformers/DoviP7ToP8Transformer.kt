package com.suyashbelekar.exoplayerhdrutils.video.transformers

import com.suyashbelekar.exoplayerhdrutils.libdovi.LibDovi

/**
 * Converts Dolby Vision Profile 7 NAL units to Profile 8.
 *
 * NAL unit type 62 (which contains Dovi metadata) is converted using libdovi (mode 2). All other
 * NAL unit types pass through unmodified.
 *
 * @throws IllegalArgumentException if libdovi fails to parse, convert, or rewrite the NAL unit.
 */
class DoviP7ToP8Transformer : NaluTransformer {
    private val libDovi = LibDovi()

    override fun transformNalu(type: Int, data: ByteArray): ByteArray {
        if (type != 62) {
            return data
        }

        return libDovi.convertNaluToP8(data)
            ?: throw IllegalArgumentException("Failed to convert Dolby Vision RPU.")
    }
}