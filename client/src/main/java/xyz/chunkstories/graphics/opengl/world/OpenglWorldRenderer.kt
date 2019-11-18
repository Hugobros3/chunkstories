package xyz.chunkstories.graphics.opengl.world

import xyz.chunkstories.graphics.common.WorldRenderer
import xyz.chunkstories.graphics.common.world.EntitiesRepresentationsProvider
import xyz.chunkstories.graphics.opengl.OpenglGraphicsBackend
import xyz.chunkstories.graphics.opengl.world.chunks.OpenglChunkRepresentationsProvider
import xyz.chunkstories.world.WorldClientCommon

class OpenglWorldRenderer(override val backend: OpenglGraphicsBackend, world: WorldClientCommon) : WorldRenderer(world) {
    val chunksRepresentationsProvider = OpenglChunkRepresentationsProvider(backend, world)
    val entitiesProvider = EntitiesRepresentationsProvider(world)

    init {
        backend.graphicsEngine.loadRenderGraph(createInstructions(world.client))
        backend.graphicsEngine.representationsProviders.registerProvider(chunksRepresentationsProvider)
        backend.graphicsEngine.representationsProviders.registerProvider(entitiesProvider)
    }

    override fun cleanup() {
        backend.graphicsEngine.representationsProviders.unregisterProvider(chunksRepresentationsProvider)
        backend.graphicsEngine.representationsProviders.unregisterProvider(entitiesProvider)
    }
}