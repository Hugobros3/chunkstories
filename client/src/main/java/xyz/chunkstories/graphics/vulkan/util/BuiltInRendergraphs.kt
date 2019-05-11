package xyz.chunkstories.graphics.vulkan.util

import xyz.chunkstories.api.graphics.TextureFormat.*
import xyz.chunkstories.api.graphics.rendergraph.PassOutput.BlendMode.MIX
import xyz.chunkstories.api.graphics.rendergraph.RenderGraphDeclarationScript
import xyz.chunkstories.api.gui.GuiDrawer

object
BuiltInRendergraphs {
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
                        //system(VulkanSpinningCubeDrawer::class)
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
