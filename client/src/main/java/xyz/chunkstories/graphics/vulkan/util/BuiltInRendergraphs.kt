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
import xyz.chunkstories.graphics.vulkan.systems.world.ChunkMeshesDispatcher
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

    val debugRenderGraph: RenderGraphDeclarationScript = {

        renderTask {
            name = "main"

            finalPassName = "gui"

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
                    name = "normalBuffer"

                    format = RGBA_8
                    size = viewportSize
                }

                renderBuffer {
                    name = "shadedBuffer"

                    format = RGB_HDR
                    size = viewportSize
                }

                renderBuffer {
                    name = "finalBuffer"

                    format = RGBA_8
                    size = viewportSize
                }

                renderBuffer {
                    name = "shadowBuffer0"

                    format = DEPTH_32
                    size = 1024 by 1024
                }

                renderBuffer {
                    name = "shadowBuffer1"

                    format = DEPTH_32
                    size = 1024 by 1024
                }

                renderBuffer {
                    name = "shadowBuffer2"

                    format = DEPTH_32
                    size = 1024 by 1024
                }

                renderBuffer {
                    name = "shadowBuffer3"

                    format = DEPTH_32
                    size = 1024 by 1024
                }
            }

            passes {
                pass {
                    name = "sky"

                    draws {
                        system(SkyDrawer::class)
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
                    name = "cubes"

                    dependsOn("sky")

                    draws {
                        system(ChunkMeshesDispatcher::class)
                    }

                    outputs {
                        output {
                            name = "colorBuffer"
                            clear = true
                            clearColor = Vector4d(0.0, 0.0, 0.0, 0.0)
                            blending = OVERWRITE
                        }

                        output {
                            name = "normalBuffer"
                            clear = true
                            clearColor = Vector4d(0.0, 0.0, 0.0, 0.0)
                            blending = OVERWRITE
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

                    dependsOn("cubes")

                    inputs {
                        imageInput {
                            name = "colorBuffer"
                            source = renderBuffer("colorBuffer")
                        }

                        imageInput {
                            name = "normalBuffer"
                            source = renderBuffer("normalBuffer")
                        }

                        imageInput {
                            name = "depthBuffer"
                            source = renderBuffer("depthBuffer")
                        }
                    }

                    draws {
                        system(VulkanFullscreenQuadDrawer::class) {
                            shaderBindings {
                                val camera = client.player.controlledEntity?.traits?.get(TraitControllable::class)?.camera ?: Camera()
                                it.bindUBO("camera", camera)
                                it.bindUBO("world",client.world.getConditions())
                            }

                            //TODO hacky api, plz fix
                            doShadowMap = true
                        }
                    }

                    outputs {
                        output {
                            name = "shadedBuffer"
                            blending = OVERWRITE
                        }
                    }
                }

                pass {
                    name = "water"

                    dependsOn("deferredShading")

                    draws {
                        system(ChunkMeshesDispatcher::class)
                    }

                    outputs {
                        output {
                            name = "shadedBuffer"
                            blending = MIX
                        }
                    }

                    depth {
                        enabled = true
                        depthBuffer = renderBuffer("depthBuffer")
                    }
                }

                pass {
                    name = "postprocess"

                    dependsOn("deferredShading", "water")

                    inputs {
                        imageInput {
                            name = "shadedBuffer"
                            source = renderBuffer("shadedBuffer")
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
                }
            }
        }

        renderTask {
            name = "sunShadow"

            finalPassName = "cubes"

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
                    format = DEPTH_32
                }
            }

            passes {
                pass {
                    name = "cubes"

                    draws {
                        system(ChunkMeshesDispatcher::class)
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
