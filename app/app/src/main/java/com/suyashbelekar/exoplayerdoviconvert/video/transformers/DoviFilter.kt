package com.suyashbelekar.exoplayerdoviconvert.video.transformers

/**
 * Filters out Dolby Vision NAL units.
 *
 * NAL unit type 62 (which contains Dolby Vision metadata) is dropped from the stream.
 * All other NALUs pass through unmodified.
 */
class DoviFilter : NaluTransformer {
    override fun transformNalu(type: Int, data: ByteArray): ByteArray {
        if (type != 62) {
            return data
        }

        return byteArrayOf()
    }
}