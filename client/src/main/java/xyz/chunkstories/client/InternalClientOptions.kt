package xyz.chunkstories.client

import xyz.chunkstories.api.util.configuration.OptionsDeclarationCtx
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend

/** Hidden and/or internal options this implementation of the client requires */
object InternalClientOptions {
    // We use those fields to capture the fully-qualified option name so we can't make spelling errors
    lateinit var debugMode: String private set
    lateinit var showDebugInformation: String private set
    lateinit var showFrametimeGraph: String private set
    lateinit var debugWireframe: String private set

    lateinit var viewDistance: String private set
    lateinit var syncMode: String private set

    lateinit var workerThreads: String private set
    // did you mean dedicated wam ?
    lateinit var dedicatedRam: String private set

    lateinit var lastServer: String private set

    fun createOptions(client: ClientImplementation): OptionsDeclarationCtx.() -> Unit = {
        section("client" ) {
            section("debug") {

                debugMode = optionBoolean("enable") {
                    hidden = true
                    transient = true
                }

                showDebugInformation = optionBoolean("showDebugInfo") {
                    default = false
                }

                showFrametimeGraph = optionBoolean("showFrametimeGraph") {
                    default = false
                }

                debugWireframe = optionBoolean("debugWireframe") {
                    default = false
                }
            }

            section("graphics") {
                viewDistance = optionMultipleChoicesInt("viewDistance") {
                    default = 128
                    possibleChoices = listOf(64, 128, 192, 256, 384, 512, 768, 1024)
                }

                syncMode = optionMultipleChoices("syncMode") {
                    default = "fastest"
                    possibleChoices = listOf("fastest", "vsync", "tripleBuffering")

                    hook {
                        (client.gameWindow.graphicsEngine.backend as? VulkanGraphicsBackend)?.let {
                            it.swapchain.expired = true
                        }
                    }
                }
            }

            section("performance") {
                workerThreads = optionInt("workerThreads") {
                    default = -1
                }

                dedicatedRam = optionMultipleChoicesInt("dedicatedRam") {
                    default = 2048
                    //TODO list sensible choices based on detected machine specs
                    possibleChoices = listOf(512, 1024, 2048, 4096, 6144, 8192, 12288, 16384, 24576, 32768)
                }
            }

            section("game") {
                lastServer = option("lastServer") {
                    default = ""
                    hidden = true
                }
            }
        }
    }
}