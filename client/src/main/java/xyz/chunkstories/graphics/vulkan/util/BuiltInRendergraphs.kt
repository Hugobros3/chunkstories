package xyz.chunkstories.graphics.vulkan.util

import org.joml.Matrix4f
import org.joml.Vector3f
import xyz.chunkstories.api.client.Client
import xyz.chunkstories.api.graphics.TextureFormat.*
import xyz.chunkstories.api.graphics.rendergraph.PassOutput.BlendMode.MIX
import xyz.chunkstories.api.graphics.rendergraph.RenderGraphDeclarationScript
import xyz.chunkstories.api.graphics.rendergraph.asset
import xyz.chunkstories.api.graphics.structs.Camera
import xyz.chunkstories.api.graphics.systems.drawing.FullscreenQuadDrawer
import xyz.chunkstories.api.gui.GuiDrawer

object
BuiltInRendergraphs {
    fun onlyGuiRenderGraph(client: Client): RenderGraphDeclarationScript = {
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
                        system(FullscreenQuadDrawer::class) {
                            setup {
                                shaderResources.supplyImage("background") {
                                    source = asset("textures/skybox/")
                                }
                                val pos = Vector3f()
                                val angle = (System.currentTimeMillis() % ((1000L * Math.PI * 2).toLong() * 1000L)) / 5_000.0
                                val angle2 = (System.currentTimeMillis() % ((1000L * Math.PI * 2).toLong() * 2500L)) / 25_000.0
                                pos.x = Math.sin(angle).toFloat()
                                pos.z = Math.cos(angle).toFloat()

                                pos.y = Math.cos(angle2).toFloat() * 0.5f
                                pos.normalize()
                                //println("$angle $pos")

                                val fov = (90.0 / 360.0 * (org.joml.Math.PI * 2)).toFloat()
                                val aspect = client.gameWindow.width.toFloat() / client.gameWindow.height.toFloat()
                                val projectionMatrix = Matrix4f().perspective(fov, aspect, 0.1f, 2000f, true)

                                val lookAt = Vector3f(0f)
                                val up = Vector3f(0f, 1f, 0f)
                                val viewMatrix = Matrix4f()
                                viewMatrix.lookAt(pos, lookAt, up)

                                shaderResources.supplyUniformBlock("camera", Camera(pos, lookAt, up, fov, viewMatrix, projectionMatrix))
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
