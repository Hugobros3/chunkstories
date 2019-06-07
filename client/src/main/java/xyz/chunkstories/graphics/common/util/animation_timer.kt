package xyz.chunkstories.graphics.common.util

fun getAnimationTime() : Double {
    val realWorldTimeTruncated = (System.nanoTime() % 1000_000_000_000)
    val realWorldTimeMs = realWorldTimeTruncated / 1000_000
    val animationTime = (realWorldTimeMs / 1000.0) * 1000.0

    return animationTime
}