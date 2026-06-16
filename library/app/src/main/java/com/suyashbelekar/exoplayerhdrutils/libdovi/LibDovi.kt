package com.suyashbelekar.exoplayerhdrutils.libdovi

class LibDovi {
    init {
        System.loadLibrary("dovi_android")
    }

    external fun convertNaluToP8(rpuBytes: ByteArray): ByteArray?

    external fun getDoviInfo(rpuBytes: ByteArray): IntArray
}