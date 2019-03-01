package xyz.chunkstories.graphics.vulkan.util

import org.joml.Vector4d
import xyz.chunkstories.api.entity.traits.serializable.TraitControllable
import xyz.chunkstories.api.graphics.TextureFormat.*
import xyz.chunkstories.api.graphics.rendergraph.PassOutput.BlendMode.MIX
import xyz.chunkstories.api.graphics.rendergraph.PassOutput.BlendMode.OVERWRITE
import xyz.chunkstories.api.graphics.rendergraph.RenderGraphDeclarationScript
import xyz.chunkstories.api.graphics.structs.Camera
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.graphics.vulkan.systems.SkyDrawer
import xyz.chunkstories.graphics.vulkan.systems.VulkanFullscreenQuadDrawer
import xyz.chunkstories.graphics.vulkan.systems.VulkanSpinningCubeDrawer
import xyz.chunkstories.graphics.vulkan.systems.world.ChunkRepresentationsDispatcher
import xyz.chunkstories.graphics.vulkan.systems.world.getConditions

object BuiltInRendergraphs {
    val onlyGuiRenderGraph: RenderGraphDeclarationScript = {
        renderTask {
            name = "main"

            finalPassName = "gui"

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
                        depthBuffer = renderBuffer("menuDepth")
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

                    depth {
                        enabled = false
                    }
                }
            }
        }
    }
}
