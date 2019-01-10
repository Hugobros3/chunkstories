package xyz.chunkstories.graphics.vulkan.swapchain

import xyz.chunkstories.gui.debug.FrametimesGraph

class VulkanPerformanceCounter(val swapChain: SwapChain) : PerformanceMetrics {
    override var lastFrametimeNs: Long = 0
        private set

    override var avgFps: Double = 0.0
        private set
    override var minFps: Double = 0.0
        private set
    override var maxFps: Double = 0.0
        private set

    var lastResetTime = 0L
    var elapsedFramesSinceLastReset = 0
    var avgFpsAcc = 0.0
    var minFpsAcc = Double.MAX_VALUE
    var maxFpsAcc = 0.0

    var lastFrameBeginTime = 0L
    //val startedTimes = LongArray(swapChain.maxFramesInFlight)
    //var index = 0

    fun whenFrameBegins(frame: Frame) {
        val now = System.nanoTime()
        val delta = now - lastResetTime
        if(delta > ONE_SECOND) {
            avgFps = avgFpsAcc / elapsedFramesSinceLastReset
            minFps = minFpsAcc
            maxFps = maxFpsAcc

            elapsedFramesSinceLastReset = 0

            avgFpsAcc = 0.0
            minFpsAcc = Double.MAX_VALUE
            maxFpsAcc = 0.0

            lastResetTime = now
        }

        val frameDelta = now - lastFrameBeginTime

        FrametimesGraph.receive(frameDelta)
        lastFrametimeNs = frameDelta

        val frameDeltaSeconds = (frameDelta / 1000).toDouble() / (1000.0 * 1000.0)
        val rawFps = 1.0 / frameDeltaSeconds

        if(rawFps > maxFpsAcc)
            maxFpsAcc = rawFps

        if(rawFps < minFpsAcc)
            minFpsAcc = rawFps

        avgFpsAcc += rawFps
        elapsedFramesSinceLastReset++

        lastFrameBeginTime = now
    }

    /*fun whenFrameEnds() {
        val frameDelta = System.nanoTime()  - frame.started

        lastFrametimeNs = frameDelta

        val frameDeltaSeconds = (frameDelta / 1000).toDouble() / (1000.0 * 1000.0)
        val rawFps = 1.0 / frameDeltaSeconds

        if(rawFps > maxFpsAcc)
            maxFpsAcc = rawFps

        if(rawFps < minFpsAcc)
            minFpsAcc = rawFps

        avgFpsAcc += rawFps
        elapsedFramesSinceLastReset++
    }*/
}

/** One second in nanoseconds */
const val ONE_SECOND = 1000 * 1000 * 1000

interface PerformanceMetrics {
    val lastFrametimeNs: Long

    val avgFps: Double
    val minFps: Double
    val maxFps: Double
}