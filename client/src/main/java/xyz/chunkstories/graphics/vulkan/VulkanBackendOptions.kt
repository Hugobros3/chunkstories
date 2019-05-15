package xyz.chunkstories.graphics.vulkan

import xyz.chunkstories.api.util.configuration.OptionsDeclarationCtx
import xyz.chunkstories.client.InternalClientOptions

object VulkanBackendOptions {
    lateinit var raytracedGI: String private set

    fun create(backend: VulkanGraphicsBackend): OptionsDeclarationCtx.() -> Unit = {
        section("client" ) {
            section("graphics") {
                raytracedGI = optionBoolean("raytracedGI") {
                    default = false
                    hook {
                        backend.reloadRendergraph()
                    }
                }
            }
        }
    }
}