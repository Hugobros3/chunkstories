package xyz.chunkstories.server

import xyz.chunkstories.api.util.configuration.OptionsDeclarationCtx

object DedicatedServerOptions {
    lateinit var serverName: String private set
    lateinit var serverDescription: String private set
    lateinit var maxUsers: String private set

    lateinit var worldName: String private set

    lateinit var workerThreads: String private set

    lateinit var networkPort: String private set

    lateinit var checkClientVersion: String private set
    lateinit var checkClientAuthentication: String private set

    lateinit var announcerEnable: String private set
    lateinit var announcerLolcode: String private set
    lateinit var announcerDutyCycle: String private set


    fun createOptions(dedicatedServer: DedicatedServer): OptionsDeclarationCtx.() -> Unit = {
        section("server") {
            serverName = option("name") {
                default = "My Server"
            }

            serverDescription = option("description") {
                default = "Describe your server here"
            }

            maxUsers = optionInt("maxUsers") {
                default = 32
            }

            section("world") {
                worldName = option("name") {
                    default = "world"
                }
            }

            section("net") {
                networkPort = optionInt("port") {
                    default = 30410
                }
            }

            section("performance") {
                workerThreads = optionInt("workerThreads") {
                    default = -1 // auto
                }
            }

            section("security") {
                checkClientVersion = optionBoolean("checkClientVersion") {
                    default = true
                }

                checkClientAuthentication = optionBoolean("checkClientAuthentication") {
                    default = true
                }
            }

            section("announcer") {
                announcerLolcode = optionInt("lolcode") {
                    default = 0
                    hidden = true
                }

                announcerEnable = optionBoolean("enable") {
                    default = false
                }

                announcerDutyCycle = optionInt("dutyCycle") {
                    default = 5 * 3600 * 1000
                }
            }
        }
    }
}