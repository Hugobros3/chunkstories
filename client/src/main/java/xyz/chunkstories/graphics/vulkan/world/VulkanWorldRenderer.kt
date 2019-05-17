package xyz.chunkstories.graphics.vulkan.world

import org.joml.Vector4d
import xyz.chunkstories.api.entity.traits.serializable.TraitControllable
import xyz.chunkstories.api.graphics.TextureFormat
import xyz.chunkstories.api.graphics.TextureTilingMode
import xyz.chunkstories.api.graphics.rendergraph.*
import xyz.chunkstories.api.graphics.structs.Camera
import xyz.chunkstories.api.graphics.systems.dispatching.ChunksRenderer
import xyz.chunkstories.api.graphics.systems.dispatching.ModelsRenderer
import xyz.chunkstories.api.graphics.systems.dispatching.SpritesRenderer
import xyz.chunkstories.api.graphics.systems.drawing.FullscreenQuadDrawer
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.graphics.common.CommonGraphicsOptions
import xyz.chunkstories.graphics.common.WorldRenderer
import xyz.chunkstories.graphics.common.getConditions
import xyz.chunkstories.graphics.common.world.doShadowMapping
import xyz.chunkstories.graphics.vulkan.VulkanBackendOptions
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.systems.Vulkan3DVoxelRaytracer
import xyz.chunkstories.graphics.vulkan.systems.world.VulkanChunkRepresentationsProvider
import xyz.chunkstories.graphics.vulkan.world.entities.EntitiesRepresentationsProvider
import xyz.chunkstories.world.WorldClientCommon

class VulkanWorldRenderer(val backend: VulkanGraphicsBackend, world: WorldClientCommon) : WorldRenderer(world) {
    val chunksRepresentationsProvider = VulkanChunkRepresentationsProvider(backend, world)
    val entitiesProvider = EntitiesRepresentationsProvider(world)

    val client = world.gameContext

    init {
        backend.graphicsEngine.loadRenderGraph(createInstructions())

        backend.graphicsEngine.representationsProviders.registerProvider(chunksRepresentationsProvider)
        backend.graphicsEngine.representationsProviders.registerProvider(entitiesProvider)
    }

    override fun cleanup() {
        backend.graphicsEngine.representationsProviders.unregisterProvider(chunksRepresentationsProvider)
        backend.graphicsEngine.representationsProviders.unregisterProvider(entitiesProvider)
        //chunksRepresentationsProvider.cleanup()
    }

    fun createInstructions(): RenderGraphDeclarationScript = {
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

                renderBuffer {
                    name = "bloom_temp"

                    format = TextureFormat.RGB_HDR
                    size = viewportSize * 0.5
                }

                renderBuffer {
                    name = "bloom"

                    format = TextureFormat.RGB_HDR
                    size = viewportSize * 0.5
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

                    dependsOn("sky")

                    draws {
                        system(ChunksRenderer::class) {
                            shader = "blockMeshes"
                            materialTag = "opaque"
                        }
                        system(ModelsRenderer::class) {
                            shader = "models"
                            materialTag = "opaque"
                            supportsAnimations = true
                        }
                        system(SpritesRenderer::class) {
                            shader = "sprites"
                            materialTag = "opaque"
                        }
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

                    dependsOn("opaque")

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
                        if (client.configuration.getBooleanValue(VulkanBackendOptions.raytracedGI)) {
                            system(Vulkan3DVoxelRaytracer::class)
                        } else {
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
                    }

                    outputs {
                        output {
                            name = "shadedBuffer"
                            blending = PassOutput.BlendMode.OVERWRITE
                        }
                    }
                }

                pass {
                    name = "forward"

                    dependsOn("deferredShading")

                    draws {
                        system(ChunksRenderer::class) {
                            shader = "water"
                            materialTag = "water"

                            setup {
                                shaderResources.supplyImage("waterNormalDeep") {
                                    source = asset("textures/water/deep.png")
                                    tilingMode = TextureTilingMode.REPEAT
                                    scalingMode = ImageInput.ScalingMode.LINEAR
                                }
                                shaderResources.supplyImage("waterNormalShallow") {
                                    source = asset("textures/water/shallow.png")
                                    tilingMode = TextureTilingMode.REPEAT
                                    scalingMode = ImageInput.ScalingMode.LINEAR
                                }
                            }
                        }
                    }

                    outputs {
                        output {
                            name = "shadedBuffer"
                            blending = PassOutput.BlendMode.MIX
                        }
                    }

                    depth {
                        enabled = true
                        depthBuffer = renderBuffer("depthBuffer")
                    }
                }

                pass {
                    name = "bloom_blurH"

                    dependsOn("forward")

                    draws {
                        system(FullscreenQuadDrawer::class) {
                            shader = "blur_horizontal"
                        }
                    }

                    setup {
                        shaderResources.supplyImage("inputTexture") {
                            source = renderBuffer("shadedBuffer")
                            scalingMode = ImageInput.ScalingMode.LINEAR
                        }
                    }

                    outputs {
                        output {
                            name = "fragColor"
                            target = renderBuffer("bloom_temp")
                            blending = PassOutput.BlendMode.OVERWRITE
                        }
                    }
                }

                pass {
                    name = "bloom_blurV"

                    dependsOn("bloom_blurH")

                    draws {
                        system(FullscreenQuadDrawer::class) {
                            shader = "blur_vertical"
                        }
                    }

                    setup {
                        shaderResources.supplyImage("inputTexture") {
                            source = renderBuffer("bloom_temp")
                            scalingMode = ImageInput.ScalingMode.LINEAR
                        }
                    }

                    outputs {
                        output {
                            name = "fragColor"
                            target = renderBuffer("bloom")
                            blending = PassOutput.BlendMode.OVERWRITE
                        }
                    }
                }

                pass {
                    name = "postprocess"

                    dependsOn("forward")
                    dependsOn("bloom_blurV")

                    setup {
                        shaderResources.supplyImage("shadedBuffer") {
                            source = renderBuffer("shadedBuffer")
                        }

                        shaderResources.supplyImage("bloomBuffer") {
                            source = renderBuffer("bloom")
                            scalingMode = ImageInput.ScalingMode.LINEAR
                        }
                    }

                    draws {
                        fullscreenQuad()
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

                    dependsOn("postprocess")

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
                        system(ModelsRenderer::class) {
                            shader = "models"
                            materialTag = "opaque"
                            supportsAnimations = true
                        }
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