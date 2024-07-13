package xyz.chunkstories.graphics.common.shaders.compiler

public fun Int.hex(): String {
    var lol = ""
    var t = this

    for (nibble in 0..31 step 4) {
        val r = t and 0xF
        lol = hexs[r] + lol
        t = t shr 4
    }

    return lol
}

//TODO move
val hexs = "0123456789ABCDEF".toCharArray()