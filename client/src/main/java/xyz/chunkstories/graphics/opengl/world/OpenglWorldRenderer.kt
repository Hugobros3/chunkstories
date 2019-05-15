package xyz.chunkstories.graphics.opengl.world

import org.joml.Vector4d
import xyz.chunkstories.api.client.Client
import xyz.chunkstories.api.graphics.TextureFormat
import xyz.chunkstories.api.graphics.rendergraph.PassOutput
import xyz.chunkstories.api.graphics.rendergraph.RenderGraphDeclarationScript
import xyz.chunkstories.api.graphics.systems.dispatching.ChunksRenderer
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.graphics.common.WorldRenderer
import xyz.chunkstories.graphics.opengl.OpenglGraphicsBackend
import xyz.chunkstories.graphics.opengl.world.chunks.OpenglChunksRepresentationsProvider
import xyz.chunkstories.world.WorldClientCommon

class OpenglWorldRenderer(val backend: OpenglGraphicsBackend, world: WorldClientCommon) : WorldRenderer(world) {

    val chunksRepresentationsProvider = OpenglChunksRepresentationsProvider(backend, world)

    init {
        backend.graphicsEngine.loadRenderGraph(createInstructions(world.client))

        backend.graphicsEngine.representationsProviders.registerProvider(chunksRepresentationsProvider)
    }

    override fun cleanup() {
        backend.graphicsEngine.representationsProviders.unregisterProvider(chunksRepresentationsProvider)
    }

    fun createInstructions(client: Client): RenderGraphDeclarationScript = {
        renderTask {
            name = "main"

            finalPassName = "opaque"

            renderBuffers {
                renderBuffer {
                    name = "depthBuffer"

                    format = TextureFormat.DEPTH_32
                    size = viewportSize
                }

                renderBuffer {
                    name = "colorBuffer"

                    format = TextureFormat.RGBA_8
                    size = viewportSize
                }

                renderBuffer {
                    name = "normalBuffer"

                    format = TextureFormat.RGBA_8
                    size = viewportSize
                }
            }

            passes {

                pass {
                    name = "opaque"

                    draws {
                        system(ChunksRenderer::class) {
                            shader = "blockMeshes"
                            materialTag = "opaque"
                        }
                        system(GuiDrawer::class)
                        /*system(ModelsRenderer::class) {
                            shader = "models"
                            materialTag = "opaque"
                            supportsAnimations = true
                        }
                        system(SpritesRenderer::class) {
                            shader = "sprites"
                            materialTag = "opaque"
                        }*/
                    }

                    outputs {
                        output {
                            name = "colorBuffer"
                            clear = true
                            clearColor = Vector4d(0.0, 0.0, 0.0, 0.0)
                            blending = PassOutput.BlendMode.OVERWRITE
                        }

                        output {
                            name = "normalBuffer"
                            clear = true
                            clearColor = Vector4d(0.0, 0.0, 0.0, 0.0)
                            blending = PassOutput.BlendMode.OVERWRITE
                        }
                    }

                    depth {
                        enabled = true
                        depthBuffer = renderBuffer("depthBuffer")
                        clear = true
                    }
                }
            }
        }
    }
}