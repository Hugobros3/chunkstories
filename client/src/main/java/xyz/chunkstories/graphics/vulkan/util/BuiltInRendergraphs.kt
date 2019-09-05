package xyz.chunkstories.graphics.vulkan.util

import org.joml.Matrix4f
import org.joml.Vector3d
import org.joml.Vector3f
import xyz.chunkstories.api.client.Client
import xyz.chunkstories.api.graphics.TextureFormat
import xyz.chunkstories.api.graphics.TextureFormat.*
import xyz.chunkstories.api.graphics.rendergraph.*
import xyz.chunkstories.api.graphics.rendergraph.PassOutput.BlendMode.MIX
import xyz.chunkstories.api.graphics.structs.Camera
import xyz.chunkstories.api.graphics.structs.makeCamera
import xyz.chunkstories.api.graphics.systems.drawing.FullscreenQuadDrawer
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.api.util.kotlin.toVec3f
import xyz.chunkstories.graphics.vulkan.systems.debug.VulkanSpinningCubeDrawer

object
BuiltInRendergraphs {
    fun onlyGuiRenderGraph(client: Client): RenderGraphDeclarationScript = {
        renderTask {
            name = "main"

            finalPassName = "gui"

            renderBuffers {
                /*renderBuffer {
                    name = "menuDepth"

                    format = DEPTH_32
                    size = viewportSize
                }*/

                renderBuffer {
                    name = "guiColorBuffer"

                    format = RGBA_8
                    size = viewportSize
                }

                renderBuffer {
                    name = "blur_temp"

                    format = TextureFormat.RGB_HDR
                    size = viewportSize * 0.5
                }
            }

            passes {
                pass {
                    name = "menuBackground"

                    draws {
                        /*system(VulkanSpinningCubeDrawer::class) {

                        }*/
                        system(FullscreenQuadDrawer::class) {
                            setup {
                                shaderResources.supplyImage("background") {
                                    source = asset("textures/skybox/")
                                }
                                val pos = Vector3d()
                                val angle = (System.currentTimeMillis() % ((1000L * Math.PI * 2).toLong() * 1000L)) / 5_000.0
                                val angle2 = (System.currentTimeMillis() % ((1000L * Math.PI * 2).toLong() * 2500L)) / 25_000.0
                                pos.x = Math.sin(angle)
                                pos.z = Math.cos(angle)

                                pos.y = Math.cos(angle2) * 0.5
                                pos.normalize()

                                val lookAt = pos.toVec3f().negate()
                                val up = Vector3f(0f, 1f, 0f)
                                //println("$angle $pos")

                                /*val fov = (90.0 / 360.0 * (org.joml.Math.PI * 2)).toFloat()
                                val aspect = client.gameWindow.width.toFloat() / client.gameWindow.height.toFloat()
                                val projectionMatrix = Matrix4f().perspective(fov, aspect, 0.1f, 2000f, true)

                                val viewMatrix = Matrix4f()
                                viewMatrix.lookAt(pos.toVec3f(), lookAt, up)*/

                                //val camera = Camera(pos, lookAt, up, fov, viewMatrix, projectionMatrix)
                                val camera = client.makeCamera(pos, lookAt, up, 90f)

                                shaderResources.supplyUniformBlock("camera", camera)
                                //shaderResources.supplyUniformBlock("camera", Camera())
                            }
                        }
                    }

                    outputs {
                        output {
                            name = "guiColorBuffer"
                            clear = true
                        }
                    }

                    depth {
                        enabled = false
                        //enabled = true
                        //depthBuffer = renderBuffer("menuDepth")
                        //clear = true
                    }
                }

                pass {
                    name = "bloom_blurH"

                    dependsOn("menuBackground")

                    draws {
                        system(FullscreenQuadDrawer::class) {
                            shader = "blur_horizontal"
                        }
                    }

                    setup {
                        shaderResources.supplyImage("inputTexture") {
                            source = renderBuffer("guiColorBuffer")
                            scalingMode = ImageInput.ScalingMode.LINEAR
                        }
                    }

                    outputs {
                        output {
                            name = "fragColor"
                            target = renderBuffer("blur_temp")
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
                            source = renderBuffer("blur_temp")
                            scalingMode = ImageInput.ScalingMode.LINEAR
                        }
                    }

                    outputs {
                        output {
                            name = "fragColor"
                            target = renderBuffer("guiColorBuffer")
                            blending = PassOutput.BlendMode.OVERWRITE
                        }
                    }
                }

                pass {
                    name = "gui"

                    dependsOn("menuBackground")
                    dependsOn("bloom_blurV")

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
