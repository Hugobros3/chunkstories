package xyz.chunkstories.graphics.vulkan.world

import xyz.chunkstories.api.client.IngameClient
import xyz.chunkstories.api.graphics.GraphicsBackend
import xyz.chunkstories.api.graphics.TextureFormat
import xyz.chunkstories.api.graphics.TextureTilingMode
import xyz.chunkstories.api.graphics.rendergraph.*
import xyz.chunkstories.api.graphics.systems.dispatching.*
import xyz.chunkstories.api.graphics.systems.drawing.FullscreenQuadDrawer
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.api.math.random.PrecomputedSimplexSeed
import xyz.chunkstories.api.world.World
import xyz.chunkstories.graphics.common.getConditions
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.systems.drawing.rt.VulkanWorldVolumetricTexture
import xyz.chunkstories.graphics.vulkan.textures.VulkanSampler
import xyz.chunkstories.world.WorldImplementation

fun createWorldRaytracingRenderGraph(client: IngameClient, backend: VulkanGraphicsBackend, world: World) = renderGraph {
    val volumeSideLength = 256
    val sampler = VulkanSampler(backend, tilingMode = TextureTilingMode.REPEAT)
    val volumetricTexture = VulkanWorldVolumetricTexture(backend, client.ingame?.world as WorldImplementation, volumeSideLength)

    setup {
        shaderResources.supplyUniformBlock("world", world.getConditions())
    }

    renderTask {
        name = "main"
        finalPassName = "gui"

        renderBuffers {
            renderBuffer {
                name = "finalBuffer"

                format = TextureFormat.RGBA_8
                size = viewportSize
            }
        }

        setup {
            volumetricTexture.updateArround(camera.position)
        }

        passes {
            pass {
                name = "primary"

                setup {

                }

                draws {
                    system(FullscreenQuadDrawer::class) {
                        shader = "raytraced"
                        defines["VOLUME_TEXTURE_SIZE"] = "$volumeSideLength"

                        setup {
                            this.shaderResources.supplyImage("blueNoise") {
                                source = asset("textures/noise/blue1024.png")
                            }
                            this.shaderResources.supplyImage("voxelData") {
                                source = ImageSource.TextureReference(volumetricTexture.texture)
                            }
                            this.shaderResources.supplyUniformBlock("voxelDataInfo", volumetricTexture.info)
                        }
                    }
                }

                outputs {
                    output {
                        name = "colorOut"
                        target = renderBuffer("finalBuffer")
                        blending = PassOutput.BlendMode.OVERWRITE
                    }
                }
            }

            pass {
                name = "gui"

                dependsOn("primary")

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

    cleanup {
        sampler.cleanup()
        volumetricTexture.cleanup()
    }
}
