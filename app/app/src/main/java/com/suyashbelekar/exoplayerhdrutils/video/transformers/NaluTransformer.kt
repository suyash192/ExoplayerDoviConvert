package com.suyashbelekar.exoplayerhdrutils.video.transformers

/**
 * A contract for intercepting and modifying individual NAL units within a video stream.
 *
 * Implementations can inspect the NALU [type] and its payload [data] to filter,
 * rewrite, or pass it through unmodified. Returning an empty byte array effectively
 * drops the NAL unit from the stream.
 */
interface NaluTransformer {

    /**
     * Processes a single NAL unit.
     *
     * @param type The parsed NAL unit type (e.g, 39 for HDR10+, 62 for Dovi).
     * @param data The raw byte payload of the NAL unit.
     * @return The modified byte array, if to be converted, or an empty array to drop.
     */
    fun transformNalu(type: Int, data: ByteArray): ByteArray
}

