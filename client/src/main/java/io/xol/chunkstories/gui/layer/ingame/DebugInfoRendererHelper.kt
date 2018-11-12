package io.xol.chunkstories.gui.layer.ingame

import io.xol.chunkstories.api.entity.traits.TraitVoxelSelection
import io.xol.chunkstories.api.entity.traits.serializable.TraitRotation
import io.xol.chunkstories.api.gui.GuiDrawer
import io.xol.chunkstories.api.util.kotlin.toVec3i
import io.xol.chunkstories.client.glfw.GLFWWindow
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.resources.VmaAllocator
import io.xol.chunkstories.graphics.vulkan.systems.world.VulkanCubesDrawer
import io.xol.chunkstories.util.VersionInfo
import io.xol.chunkstories.world.WorldImplementation
import io.xol.chunkstories.world.chunk.ChunkHolderImplementation
import io.xol.chunkstories.world.chunk.CubicChunk

class DebugInfoRendererHelper(ingameLayer: IngameLayer) {
    val gui = ingameLayer.gui

    fun drawDebugInfo(guiDrawer: GuiDrawer) {
        var posY = gui.viewportHeight - 4
        val font = guiDrawer.fonts.getFont("LiberationSans-Regular", 10f)

        fun debugLine(text: String) {
            posY -= 12
            guiDrawer.drawStringWithShadow(font, 4, posY, text)
        }

        val client = gui.client.ingame!!
        val window = (client.gameWindow as GLFWWindow)
        val world = client.world as WorldImplementation
        val swapchain = (window.graphicsBackend as VulkanGraphicsBackend).swapchain

        debugLine("Chunk Stories ${VersionInfo.version} running on the ${window.graphicsBackend.javaClass.simpleName}")
        debugLine("${client.tasks.submittedTasks()} + ${client.tasks}")

        val manualChunksCount = world.regionsHolder.internalGetLoadedRegions().sumBy { it.loadedChunks.size }

        if(world.regionsHolder.internalGetLoadedRegions().toSet().size != world.regionsHolder.internalGetLoadedRegions().size) {
            println("DUPLICATED REGION OMG")
        }

        val globalUserCount = world.regionsHolder.internalGetLoadedRegions().sumBy { it.loadedChunks.sumBy { it.holder().countUsers() } }
        val zombieChunksCount = world.regionsHolder.internalGetLoadedRegions().sumBy { it.loadedChunks.count { it.holder().countUsers() == 0 } }

        debugLine("VMA allocations: ${VmaAllocator.allocations} total ${VmaAllocator.allocatedBytes.get()/1024/1024}mb ")

        debugLine("chunks counter: ${CubicChunk.chunksCounter} chunksR: ${ChunkHolderImplementation.globalRegisteredUsers} manualCount: $manualChunksCount users $globalUserCount zombies: #FF0000$zombieChunksCount")
        debugLine("#FF0000Rendering performance : ${swapchain.fps.toInt()}FPS | ${swapchain.lastFrametime/1000000}ms #00FFFFSimulation performance : ${world.gameLogic.simulationFps}")
        debugLine("Cubes drawn: ${VulkanCubesDrawer.totalCubesDrawn} within ${VulkanCubesDrawer.totalBuffersUsed} vertex buffers")
        debugLine("World info : ${world.allLoadedChunks.count()} chunks loaded, ${world.regionsHolder.stats}")

        val playerEntity = client.player.controlledEntity
        if(playerEntity != null ) {
            debugLine("Controlled entity id ${playerEntity.UUID} position ${playerEntity.location} type ${playerEntity.definition.name}")

            val lookingAt = playerEntity.traits[TraitVoxelSelection::class]?.getBlockLookingAt(false, false)
            debugLine("Looking at $lookingAt in direction ${playerEntity.traits[TraitRotation::class]?.directionLookingAt}")

            val standingAt = playerEntity.location.toVec3i()
            val standingIn = world.peekSafely(playerEntity.location)
            debugLine("Standing at $standingAt in ${standingIn.voxel} (solid=${standingIn.voxel?.solid}, box=${standingIn.voxel?.collisionBoxes?.getOrNull(0)})")

            val region = world.getRegionLocation(playerEntity.location)
            val chunk = world.getChunkWorldCoordinates(playerEntity.location)
            debugLine("Chunk: $chunk, region: $region")
        }

        //val inp = client.inputsManager.getInputByName("forward") as Lwjgl3KeyBind
        //debugLine("forward key ${inp.name} ${inp.isPressed}")
    }
}