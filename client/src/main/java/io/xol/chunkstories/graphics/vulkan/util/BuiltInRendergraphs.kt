package io.xol.chunkstories.graphics.vulkan.util

import io.xol.chunkstories.api.dsl.RenderGraphDeclarationScript

import io.xol.chunkstories.api.graphics.TextureFormat.*
import io.xol.chunkstories.api.graphics.rendergraph.DepthTestingConfiguration.DepthTestMode.*
import io.xol.chunkstories.api.graphics.rendergraph.PassOutput.BlendMode.*
import io.xol.chunkstories.api.graphics.ImageInput.SamplingMode.*
import io.xol.chunkstories.api.gui.GuiDrawer
import io.xol.chunkstories.graphics.vulkan.systems.VulkanSpinningCubeDrawer

object BuiltInRendergraphs {
    val onlyGuiRenderGraph : RenderGraphDeclarationScript = {
        renderBuffers {
            renderBuffer {
                name = "menuDepth"

                format = DEPTH_32
                size = viewportSize
            }

            renderBuffer {
                name = "guiColorBuffer"

                format = RGBA_8
                size = viewportSize
            }
        }

        passes {
            pass {
                name = "menuBackground"

                draws {
                    //fullscreenQuad()
                    system(VulkanSpinningCubeDrawer::class)
                }

                outputs {
                    output {
                        name = "guiColorBuffer"
                        clear = true
                    }
                }

                depth {
                    enabled = true
                    depthBuffer = "menuDepth"
                    clear = true
                }
            }

            pass {
                name = "gui"

                dependsOn("menuBackground")

                draws {
                    system(GuiDrawer::class)
                }

                outputs {
                    output {
                        name = "guiColorBuffer"

                        //clear = true
                        blending = MIX
                    }
                }

                default = true
                final = true

                depth {
                    enabled = false
                }
            }
        }
    }
}
