package xyz.chunkstories.graphics.opengl.world

import org.joml.Vector4d
import xyz.chunkstories.api.client.IngameClient
import xyz.chunkstories.api.entity.traits.serializable.TraitControllable
import xyz.chunkstories.api.graphics.TextureFormat
import xyz.chunkstories.api.graphics.rendergraph.PassOutput
import xyz.chunkstories.api.graphics.rendergraph.RenderGraphDeclarationScript
import xyz.chunkstories.api.graphics.rendergraph.renderBuffer
import xyz.chunkstories.api.graphics.structs.Camera
import xyz.chunkstories.api.graphics.systems.dispatching.ChunksRenderer
import xyz.chunkstories.api.graphics.systems.dispatching.ModelsRenderer
import xyz.chunkstories.api.graphics.systems.drawing.FullscreenQuadDrawer
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.graphics.common.CommonGraphicsOptions
import xyz.chunkstories.graphics.common.WorldRenderer
import xyz.chunkstories.graphics.common.getConditions
import xyz.chunkstories.graphics.common.world.doShadowMapping
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

    fun createInstructions(client: IngameClient): RenderGraphDeclarationScript = {
        renderTask {
            name = "main"

            finalPassName = "gui"

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

                renderBuffer {
                    name = "shadedBuffer"

                    format = TextureFormat.RGB_HDR
                    size = viewportSize
                }

                renderBuffer {
                    name = "finalBuffer"

                    format = TextureFormat.RGBA_8
                    size = viewportSize
                }

                val shadowCascades = client.configuration.getIntValue(CommonGraphicsOptions.shadowCascades)
                val shadowResolution = client.configuration.getIntValue(CommonGraphicsOptions.shadowMapSize)
                for (i in 0 until shadowCascades) {
                    renderBuffer {
                        name = "shadowBuffer$i"

                        format = TextureFormat.DEPTH_32
                        size = shadowResolution by shadowResolution
                    }
                }
            }

            passes {

                pass {
                    name = "sky"

                    draws {
                        system(FullscreenQuadDrawer::class) {
                            setup {
                                val entity = client.player.controlledEntity
                                val camera = entity?.traits?.get(TraitControllable::class)?.camera ?: Camera()
                                val world = client.world

                                shaderResources.supplyUniformBlock("camera", camera)
                                shaderResources.supplyUniformBlock("world", world.getConditions())
                            }
                        }
                    }

                    outputs {
                        output {
                            name = "shadedBuffer"
                            clear = true
                            clearColor = Vector4d(0.0, 0.5, 1.0, 1.0)
                        }
                    }
                }

                pass {
                    name = "opaque"

                    draws {
                        system(ChunksRenderer::class) {
                            shader = "blockMeshes"
                            materialTag = "opaque"
                        }
                        //system(GuiDrawer::class)
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

                pass {
                    name = "deferredShading"

                    dependsOn("opaque", "sky")

                    setup {
                        shaderResources.supplyImage("colorBuffer") {
                            source = renderBuffer("colorBuffer")
                        }

                        shaderResources.supplyImage("normalBuffer") {
                            source = renderBuffer("normalBuffer")
                        }

                        shaderResources.supplyImage("depthBuffer") {
                            source = renderBuffer("depthBuffer")
                        }
                    }

                    draws {
                        system(FullscreenQuadDrawer::class) {
                            setup {
                                val camera = client.player.controlledEntity?.traits?.get(TraitControllable::class)?.camera
                                        ?: Camera()

                                shaderResources.supplyUniformBlock("camera", camera)
                                shaderResources.supplyUniformBlock("world", world.getConditions())

                                doShadowMapping(this, world)
                            }
                        }
                    }

                    outputs {
                        output {
                            name = "finalBuffer"
                            blending = PassOutput.BlendMode.OVERWRITE
                        }
                    }
                }

                pass {
                    name = "gui"

                    dependsOn("deferredShading")

                    draws {
                        system(GuiDrawer::class)
                    }

                    outputs {
                        output {
                            name = "finalBuffer"

                            //clear = true
                            blending = PassOutput.BlendMode.MIX
                        }
                    }
                }
            }
        }

        renderTask {
            name = "sunShadow"

            finalPassName = "opaque"

            renderBuffers {
                /*renderBuffer {
                    name = "shadowBuffer"

                    format = DEPTH_32
                    size = 1024 by 1024
                }*/
            }

            taskInputs {
                input {
                    name = "shadowBuffer"
                    format = TextureFormat.DEPTH_32
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
                        /*system(ModelsRenderer::class) {
                            shader = "models"
                            materialTag = "opaque"
                            supportsAnimations = true
                        }*/
                    }

                    outputs {

                    }

                    depth {
                        enabled = true
                        depthBuffer = taskInput("shadowBuffer")
                        clear = true
                    }
                }
            }
        }
    }
}