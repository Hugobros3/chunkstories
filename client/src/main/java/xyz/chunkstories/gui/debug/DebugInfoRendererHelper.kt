package xyz.chunkstories.gui.debug

import xyz.chunkstories.api.entity.traits.TraitSight
import xyz.chunkstories.api.entity.traits.serializable.TraitRotation
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.api.player.entityIfIngame
import xyz.chunkstories.api.world.getCell
import xyz.chunkstories.api.world.heightmap.getHeight
import xyz.chunkstories.client.glfw.GLFWWindow
import xyz.chunkstories.client.ingame.IngameClientImplementation
import xyz.chunkstories.graphics.opengl.OpenglGraphicsBackend
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.swapchain.PerformanceCounter
import xyz.chunkstories.gui.ClientGui
import xyz.chunkstories.gui.layer.ingame.IngameUI
import xyz.chunkstories.util.VersionInfo
import xyz.chunkstories.world.WorldImplementation

class DebugInfoRendererHelper(ingameUI: IngameUI) {
    val gui = ingameUI.gui

    fun drawDebugInfo(guiDrawer: GuiDrawer) {
        //(guiDrawer.gui as ClientGui).guiScaleOverride = Math.max(1, (guiDrawer.gui as ClientGui).guiScale / 2)
        (guiDrawer.gui as ClientGui).guiScaleOverride = 1

        var posY = gui.viewportHeight
        val font = guiDrawer.fonts.getFont("LiberationSans-Regular", 16f)
        posY -= 4

        fun debugLine(text: String) {
            posY -= font.lineHeight + 0
            guiDrawer.drawStringWithShadow(font, 4, posY, text)
        }

        val window = (gui.client.gameWindow as GLFWWindow)
        val graphicsBackend = window.graphicsEngine.backend

        debugLine("Chunk Stories ${VersionInfo.versionJson.verboseVersion} running on the ${graphicsBackend.javaClass.simpleName}")
        val ingameClient: IngameClientImplementation = gui.client.ingame as? IngameClientImplementation ?: let {
            debugLine("Not ingame !")
            return
        }
        val world = ingameClient.world as WorldImplementation

        fun PerformanceCounter.print() {
            debugLine("#FF0000Rendering: ${lastFrametimeNs/1000000}ms fps: ${avgFps.toInt()} (min ${minFps.toInt()}, max ${maxFps.toInt()}) #00FFFFSimulation performance : ${ingameClient.tickingThread.simulationFps}")
        }

        when(graphicsBackend) {
            is OpenglGraphicsBackend -> {
                graphicsBackend.performance.print()
            }
            is VulkanGraphicsBackend -> {
                val swapchain = graphicsBackend.swapchain
                val performanceMetrics = swapchain.performanceCounter
                performanceMetrics.print()

                debugLine("VRAM usage: ${graphicsBackend.memoryManager.stats}")

                val frame = swapchain.lastFrame
                val stats = frame.stats
                debugLine("Vertices drawn: ${stats.totalVerticesDrawn} in ${stats.totalDrawcalls} drawcalls")
            }
        }
        debugLine("RAM usage: ${Runtime.getRuntime().freeMemory() / 1024 / 1024} mb free")
        //debugLine("VMA usage: ${VmaAllocator.allocations} allocations totalling ${VmaAllocator.allocatedBytes.get()/1024/1024}mb ")

        debugLine("Tasks queued: ${ingameClient.engine.tasks.submittedTasks()} IO operations queud: ${world.ioThread.size}")

        var chunksCount = 0
        var regionsCount = 0
        for(region in world.regionsManager.allLoadedRegions) {
            regionsCount++
            chunksCount += region.loadedChunks.size
        }
        debugLine("World info : $chunksCount chunks, $regionsCount regions, ${world.entities.count()} entities")

        //debugLine("#FFFF00Extra counters for debug info ${CubicChunk.chunksCounter.get()}")

        val location = ingameClient.player.state.location
        if (location != null) {
            val region = world.regionsManager.getRegionLocation(location)
            val heightmap = region?.heightmap
            val holder = region?.let {
                val cx = location.x.toInt() / 32
                val cy = location.y.toInt() / 32
                val cz = location.z.toInt() / 32
                it.getChunkHolder(cx, cy, cz)
            }
            val chunk = holder?.chunk

            debugLine("Region: $region")
            debugLine("Heightmap: $heightmap")
            debugLine("ChunkHolder: $holder")
            debugLine("Chunk: $chunk")

            val entity = ingameClient.player.entityIfIngame
            if (entity != null) {
                debugLine("Controlled entity id ${entity.id} position ${location} type ${entity.definition.name}")

                val lookingAt = entity.traits[TraitSight::class]?.getLookingAt(10.0)
                debugLine("Looking at $lookingAt in direction ${entity.traits[TraitRotation::class]?.directionLookingAt}")

            } else {
                debugLine("Position: $location")
            }

            val height = world.heightmapsManager.getHeight(location)

            val cell = world.getCell(location)
            if (cell != null) {
                debugLine("Standing at ${location.x()} ${location.y()} ${location.z} in ${cell.data.blockType.name} metadata=${cell.data.extraData} bl=${cell.data.blockType} sl=${cell.data.sunlightLevel} heightmap=$height")
            } else {
                debugLine("Standing at ${location.x()} ${location.y()} ${location.z} in [unloaded] heightmap=$height\"")
            }
        } else {
            debugLine("No controlled entity")
        }

        (guiDrawer.gui as ClientGui).guiScaleOverride = -1
    }
}