package com.suyashbelekar.exoplayerdoviconvert.video.transformers

/**
 * Filters out HDR10+ NAL units.
 *
 * NAL unit type 39 (which contains HDR10+ metadata) is dropped from the stream.
 * All other NALUs pass through unmodified.
 *
 * **Note:** While NALU 39 may carry other metadata as well, filtering out it entirely should be
 * safe provided both the source stream and target devices support Dolby Vision.
 */
class Hdr10PlusFilter : NaluTransformer {
    override fun transformNalu(type: Int, data: ByteArray): ByteArray {
        if (type != 39) {
            return data
        }

        return byteArrayOf()
    }
}