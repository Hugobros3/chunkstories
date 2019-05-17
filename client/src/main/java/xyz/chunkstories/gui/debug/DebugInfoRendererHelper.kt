package xyz.chunkstories.gui.debug

import xyz.chunkstories.api.entity.traits.TraitVoxelSelection
import xyz.chunkstories.api.entity.traits.serializable.TraitRotation
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.api.util.kotlin.toVec3i
import xyz.chunkstories.client.glfw.GLFWWindow
import xyz.chunkstories.graphics.opengl.OpenglGraphicsBackend
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.swapchain.PerformanceCounter
import xyz.chunkstories.gui.ClientGui
import xyz.chunkstories.gui.layer.ingame.IngameLayer
import xyz.chunkstories.util.VersionInfo
import xyz.chunkstories.world.WorldImplementation

class DebugInfoRendererHelper(ingameLayer: IngameLayer) {
    val gui = ingameLayer.gui

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

        val client = gui.client.ingame!!
        val window = (client.gameWindow as GLFWWindow)
        val world = client.world as WorldImplementation
        val graphicsBackend = window.graphicsEngine.backend

        debugLine("Chunk Stories ${VersionInfo.version} running on the ${graphicsBackend.javaClass.simpleName}")

        fun PerformanceCounter.print() {
            debugLine("#FF0000Rendering: ${lastFrametimeNs/1000000}ms fps: ${avgFps.toInt()} (min ${minFps.toInt()}, max ${maxFps.toInt()}) #00FFFFSimulation performance : ${world.gameLogic.simulationFps}")
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

        debugLine("Tasks queued: ${client.tasks.submittedTasks()} IO operations queud: ${world.ioHandler.size}")


        var chunksCount = 0
        var regionsCount = 0
        for(region in world.allLoadedRegions) {
            regionsCount++
            chunksCount += region.loadedChunks.size
        }
        debugLine("World info : $chunksCount chunks, $regionsCount regions, ${world.allLoadedEntities.count()} entities")

        //debugLine("#FFFF00Extra counters for debug info ${CubicChunk.chunksCounter.get()}")

        val playerEntity = client.player.controlledEntity
        if(playerEntity != null ) {
            val region = world.getRegionLocation(playerEntity.location)
            val heightmap = region?.heightmap
            val holder = region?.let {
                val cx = playerEntity.location.x.toInt() / 32
                val cy = playerEntity.location.y.toInt() / 32
                val cz = playerEntity.location.z.toInt() / 32
                it.getChunkHolder(cx, cy, cz)
            }
            val chunk = holder?.chunk

            debugLine("Region: $region")
            debugLine("Heightmap: $heightmap")
            debugLine("ChunkHolder: $holder")
            debugLine("Chunk: $chunk")

            debugLine("Controlled entity id ${playerEntity.UUID} position ${playerEntity.location} type ${playerEntity.definition.name}")

            val lookingAt = playerEntity.traits[TraitVoxelSelection::class]?.getBlockLookingAt(false, false)
            debugLine("Looking at $lookingAt in direction ${playerEntity.traits[TraitRotation::class]?.directionLookingAt}")

            val standingAt = playerEntity.location.toVec3i()
            val standingIn = world.peekSafely(playerEntity.location)
            debugLine("Standing at $standingAt in ${standingIn.voxel} (solid=${standingIn.voxel?.solid}, box=${standingIn.voxel?.collisionBoxes?.getOrNull(0)})")
        } else {
            debugLine("No controlled entity")
        }

        (guiDrawer.gui as ClientGui).guiScaleOverride = -1
    }
}