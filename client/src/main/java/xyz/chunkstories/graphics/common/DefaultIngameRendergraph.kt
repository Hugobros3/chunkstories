package xyz.chunkstories.graphics.common

import org.joml.Vector4d
import xyz.chunkstories.api.entity.traits.serializable.TraitControllable
import xyz.chunkstories.api.graphics.TextureFormat
import xyz.chunkstories.api.graphics.rendergraph.PassOutput
import xyz.chunkstories.api.graphics.rendergraph.RenderGraphDeclarationScript
import xyz.chunkstories.api.graphics.structs.Camera
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.graphics.vulkan.systems.SkyDrawer
import xyz.chunkstories.graphics.vulkan.systems.VulkanFullscreenQuadDrawer
import xyz.chunkstories.graphics.vulkan.systems.models.VulkanModelsDispatcher
import xyz.chunkstories.graphics.vulkan.systems.world.ChunkRepresentationsDispatcher
import xyz.chunkstories.graphics.vulkan.systems.world.getConditions

object DefaultIngameRendergraph {
    val instructions: RenderGraphDeclarationScript = {

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
                    name = "shadowBuffer0"

                    format = TextureFormat.DEPTH_32
                    size = 1024 by 1024
                }

                renderBuffer {
                    name = "shadowBuffer1"

                    format = TextureFormat.DEPTH_32
                    size = 1024 by 1024
                }

                renderBuffer {
                    name = "shadowBuffer2"

                    format = TextureFormat.DEPTH_32
                    size = 1024 by 1024
                }

                renderBuffer {
                    name = "shadowBuffer3"

                    format = TextureFormat.DEPTH_32
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
                        system(ChunkRepresentationsDispatcher::class)
                        system(VulkanModelsDispatcher::class)
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
                            blending = PassOutput.BlendMode.OVERWRITE
                        }
                    }
                }

                pass {
                    name = "water"

                    dependsOn("deferredShading")

                    draws {
                        system(ChunkRepresentationsDispatcher::class)
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
                    format = TextureFormat.DEPTH_32
                }
            }

            passes {
                pass {
                    name = "cubes"

                    draws {
                        system(ChunkRepresentationsDispatcher::class) {

                        }
                        system(VulkanModelsDispatcher::class)
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