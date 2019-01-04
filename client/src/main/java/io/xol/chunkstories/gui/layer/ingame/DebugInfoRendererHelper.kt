package io.xol.chunkstories.gui.layer.ingame

import io.xol.chunkstories.api.entity.traits.TraitVoxelSelection
import io.xol.chunkstories.api.entity.traits.serializable.TraitRotation
import io.xol.chunkstories.api.gui.GuiDrawer
import io.xol.chunkstories.api.util.kotlin.toVec3i
import io.xol.chunkstories.client.glfw.GLFWWindow
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.resources.VmaAllocator
import io.xol.chunkstories.graphics.vulkan.systems.world.VulkanCubesDrawer
import io.xol.chunkstories.gui.ClientGui
import io.xol.chunkstories.util.VersionInfo
import io.xol.chunkstories.world.WorldImplementation
import io.xol.chunkstories.world.chunk.CubicChunk

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
        val swapchain = (window.graphicsBackend as VulkanGraphicsBackend).swapchain

        debugLine("Chunk Stories ${VersionInfo.version} running on the ${window.graphicsBackend.javaClass.simpleName}")

        val performanceMetrics = swapchain.performanceCounter
        debugLine("#FF0000Rendering: ${performanceMetrics.lastFrametimeNs/1000000}ms fps: ${performanceMetrics.avgFps.toInt()} (min ${performanceMetrics.minFps.toInt()}, max ${performanceMetrics.maxFps.toInt()}) #00FFFFSimulation performance : ${world.gameLogic.simulationFps}")

        debugLine("RAM usage: ${Runtime.getRuntime().freeMemory() / 1024 / 1024} mb free")
        debugLine("VRAM usage: ${VmaAllocator.allocations} allocations totalling ${VmaAllocator.allocatedBytes.get()/1024/1024}mb ")

        debugLine("Tasks queued: ${client.tasks.submittedTasks()} IO operations queud: ${world.ioHandler.size}")

        debugLine("Vertices drawn: ${VulkanCubesDrawer.totalCubesDrawn} within ${VulkanCubesDrawer.totalBuffersUsed} vertex buffers")
        debugLine("World info : ${world.allLoadedChunks.count()} chunks loaded, ${world.regionsStorage.regionsList.count()} regions")

        //debugLine("#FFFF00Extra counters for debug info ${CubicChunk.chunksCounter.get()}")

        val playerEntity = client.player.controlledEntity
        if(playerEntity != null ) {
            val region = world.getRegionLocation(playerEntity.location)
            val holder = region?.let {
                val cx = playerEntity.location.x.toInt() / 32
                val cy = playerEntity.location.y.toInt() / 32
                val cz = playerEntity.location.z.toInt() / 32
                it.getChunkHolder(cx, cy, cz)
            }
            val chunk = holder?.chunk

            debugLine("Region: $region")
            debugLine("ChunkHolder: $holder")
            debugLine("Chunk: $chunk")

            debugLine("Controlled entity id ${playerEntity.UUID} position ${playerEntity.location} type ${playerEntity.definition.name}")

            val lookingAt = playerEntity.traits[TraitVoxelSelection::class]?.getBlockLookingAt(false, false)
            debugLine("Looking at $lookingAt in direction ${playerEntity.traits[TraitRotation::class]?.directionLookingAt}")

            val standingAt = playerEntity.location.toVec3i()
            val standingIn = world.peekSafely(playerEntity.location)
            debugLine("Standing at $standingAt in ${standingIn.voxel} (solid=${standingIn.voxel?.solid}, box=${standingIn.voxel?.collisionBoxes?.getOrNull(0)})")

            /*holder?.apply {
                debugLine("${CubicChunk.chunksCounter} history: H: ${holder.stateHistory} R: ${region.stateHistory}")
            }*/
        }

        (guiDrawer.gui as ClientGui).guiScaleOverride = -1

        //val inp = client.inputsManager.getInputByName("forward") as Lwjgl3KeyBind
        //debugLine("forward key ${inp.name} ${inp.isPressed}")
    }
}