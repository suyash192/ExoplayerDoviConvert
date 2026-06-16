package com.suyashbelekar.exoplayerhdrutils.video.transformers

data class TransformStrategy(
    val doviP7Fel: DoviStrategy, val doviP7Mel: DoviStrategy, val doviHdr10Plus: Hdr10PlusStrategy
)

enum class DoviStrategy {
    KEEP, CONVERT_TO_P8, DISCARD
}

enum class Hdr10PlusStrategy {
    KEEP, DISCARD
}