package xyz.chunkstories.client

import xyz.chunkstories.api.util.configuration.OptionsDeclarationCtx

/** Hidden and/or internal options this implementation of the client requires */
object InternalClientOptions {
    // We use those fields to capture the fully-qualified option name so we can't make spelling errors
    lateinit var debugMode: String private set
    lateinit var showDebugInformation: String private set
    lateinit var debugWireframe: String private set

    lateinit var viewDistance: String private set

    val options: OptionsDeclarationCtx.() -> Unit = {
        section("client" ) {
            section("debug") {

                debugMode = optionBoolean("enable") {
                    hidden = true
                    transient = true
                }

                showDebugInformation = optionBoolean("showDebugInfo") {
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
            }
        }
    }
}