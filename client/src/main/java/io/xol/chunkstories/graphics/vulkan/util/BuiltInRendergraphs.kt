package io.xol.chunkstories.graphics.vulkan.util

import io.xol.chunkstories.api.dsl.RenderGraphDeclarationScript

import io.xol.chunkstories.api.graphics.TextureFormat.*
import io.xol.chunkstories.api.graphics.rendergraph.PassOutput.BlendMode.*
import io.xol.chunkstories.api.gui.GuiDrawer
import io.xol.chunkstories.graphics.vulkan.systems.world.VulkanCubesDrawer
import io.xol.chunkstories.graphics.vulkan.systems.debug.VulkanDebugDrawer
import io.xol.chunkstories.graphics.vulkan.systems.VulkanSpinningCubeDrawer
import org.joml.Vector4d

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
                shaderName = "cube"

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


    val debugRenderGraph : RenderGraphDeclarationScript = {
        renderBuffers {
            renderBuffer {
                name = "depthBuffer"

                format = DEPTH_32
                size = viewportSize
            }

            renderBuffer {
                name = "colorBuffer"

                format = RGBA_8
                size = viewportSize
            }

            renderBuffer {
                name = "finalBuffer"

                format = RGBA_8
                size = viewportSize
            }
        }

        passes {
            pass {
                name = "cubes"

                draws {
                    system(VulkanCubesDrawer::class)
                }

                outputs {
                    output {
                        name = "colorBuffer"
                        clear = true
                        clearColor = Vector4d(0.0, 0.5, 1.0, 1.0)
                    }
                }

                depth {
                    enabled = true
                    depthBuffer = "depthBuffer"
                    clear = true
                }
            }

            pass {
                name = "debug"
                shaderName = "wireframe"

                dependsOn("cubes")

                draws {
                    system(VulkanDebugDrawer::class)
                }

                outputs {
                    output {
                        name = "colorBuffer"
                        blending = MIX
                    }
                }

                depth {
                    enabled = true
                    depthBuffer = "depthBuffer"
                }
            }

            pass {
                name = "postprocess"

                dependsOn("cubes", "debug")

                inputs {
                    imageInput {
                        name = "colorBuffer"
                        source = renderBuffer("colorBuffer")
                    }
                }

                draws {
                    fullscreenQuad()
                }

                outputs {
                    output {
                        name = "finalBuffer"
                        blending = OVERWRITE
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
                        blending = MIX
                    }
                }

                default = true
                final = true
            }
        }
    }
}
