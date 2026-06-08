package com.suyashbelekar.libdovi

class LibDovi {
    init {
        System.loadLibrary("dovi_android")
    }

    external fun parseUnspec62Nalu(rpuBytes: ByteArray): Long

    external fun convertRpuWithMode(rpuPtr: Long, mode: Int): Boolean

    external fun writeUnspec62Nalu(rpuPtr: Long): ByteArray?

    external fun freeRpu(rpuPtr: Long)

    external fun getDoviProfile(rpuPtr: Long): Int

    external fun getElType(rpuPtr: Long): String?
}

val libDovi = LibDovi()