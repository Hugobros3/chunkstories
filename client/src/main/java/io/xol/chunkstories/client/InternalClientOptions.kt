package io.xol.chunkstories.client

import io.xol.chunkstories.api.util.configuration.OptionsDeclarationCtx

/** Hidden and/or internal options this implementation of the client requires */
object InternalClientOptions {
    // We use those fields to capture the fully-qualified option name so we can't make spelling errors
    lateinit var debugMode: String private set
    lateinit var showDebugInformation: String private set

    val options: OptionsDeclarationCtx.() -> Unit = {
        section("client" ) {
            section("debug") {

                debugMode = optionBoolean("enable") {
                    hidden = true
                    transient = true
                }

                showDebugInformation = optionBoolean("showDebugInfo") {

                }
            }
        }
    }
}