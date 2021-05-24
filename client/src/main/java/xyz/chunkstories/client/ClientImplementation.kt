//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.client

import org.lwjgl.glfw.GLFW
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.chunkstories.EngineImplemI
import xyz.chunkstories.api.client.Client
import xyz.chunkstories.api.client.ClientIdentity
import xyz.chunkstories.api.util.configuration.Configuration
import xyz.chunkstories.client.glfw.GLFWWindow
import xyz.chunkstories.client.ingame.IngameClientImplementation
import xyz.chunkstories.content.GameContentStore
import xyz.chunkstories.content.mods.ModsManagerImplementation
import xyz.chunkstories.gameName
import xyz.chunkstories.graphics.GraphicsBackendsEnum
import xyz.chunkstories.graphics.GraphicsEngineImplementation
import xyz.chunkstories.gui.ClientGui
import xyz.chunkstories.gui.layer.LoginUI
import xyz.chunkstories.input.lwjgl3.GLFWInputManager
import xyz.chunkstories.sound.ALSoundManager
import xyz.chunkstories.task.WorkerThreadPool
import xyz.chunkstories.util.LogbackSetupHelper
import xyz.chunkstories.util.VersionInfo
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

fun main(launchArguments: Array<String>) {
    val argumentsMap = mutableMapOf<String, String>()
    for (launchArgument in launchArguments) {
        if (launchArgument.startsWith("--")) {
            val stripped = launchArgument.removePrefix("--")

            if (launchArgument.contains('=')) {
                val firstIndex = stripped.indexOf('=')
                val argName = stripped.substring(0, firstIndex)
                val argValue = stripped.substring(firstIndex + 1, stripped.length).removeSurrounding("\"")

                argumentsMap[argName] = argValue
            } else {
                argumentsMap[stripped] = "true"
            }
        } else {
            println("Unrecognized launch argument: $launchArgument")
        }
    }

    if (argumentsMap["help"] != null) {
        printHelp()
        System.exit(0)
    }

    ClientImplementation(argumentsMap)
}

private fun printHelp() {
    println("""
                $gameName Client version: ${VersionInfo.versionJson.verboseVersion}

                Available commandline options:
                --core=... Specifies the folder/file to use as the base content
                --mods=... Specifies some mods to load
                --backend=${GraphicsBackendsEnum.values().map { it.name }} Forces a specific backend to be used.

                Backend-specific options:

                Vulkan-specific options:
                --enableValidation Enables the validation layers

                OpenGL-specific options:
            """.trimIndent())
}

/** Client implementation entry point, is the root of the systems and holds state through them  */
class ClientImplementation internal constructor(val arguments: Map<String, String>) : EngineImplemI, Client {
    override val tasks: WorkerThreadPool

    override val logger: Logger
    val chatLogger: Logger = LoggerFactory.getLogger("game.chat")

    private val configFile: File = File("./config/client.config")
    override val configuration: Configuration = Configuration(configFile)

    override val content: GameContentStore
    override val modsManager: ModsManagerImplementation
        get() = content.modsManager

    override val graphics: GraphicsEngineImplementation
    override val inputsManager: GLFWInputManager

    override val gameWindow: GLFWWindow
        get() = graphics.window

    override val soundManager: ALSoundManager

    override val gui = ClientGui(this)

    override lateinit var user: ClientIdentity
    override var ingame: IngameClientImplementation? = null

    init {
        // Name the thread
        Thread.currentThread().name = "Main thread"

        configuration.addOptions(InternalClientOptions.createOptions(this))

        // Start logging system
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("YYYY.MM.dd HH.mm.ss")
        val time = sdf.format(cal.time)

        logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
        val loggingFilename = "./logs/$time.log"
        LogbackSetupHelper(loggingFilename)

        soundManager = ALSoundManager(this)

        graphics = GraphicsEngineImplementation(this)
        inputsManager = GLFWInputManager(gameWindow)

        val coreContentLocation = File(arguments["core"] ?: "core_content.zip")

        // Create game content manager
        content = GameContentStore(this, coreContentLocation, arguments["mods"]?.split(",")?.map { it.trim() } ?: emptyList())
        content.reload()

        configuration.load()

        inputsManager.reload()

        // Spawns worker threads
        var workerThreadsCount: Int = configuration.getIntValue(InternalClientOptions.workerThreads)

        if (workerThreadsCount <= 0) {
            workerThreadsCount = Runtime.getRuntime().availableProcessors() / 2

            // Fail-safe
            if (workerThreadsCount < 1)
                workerThreadsCount = 1
        }

        tasks = WorkerThreadPool(workerThreadsCount)
        tasks.start()

        // Load the correct language
        val lang: String = configuration.getValue("client.game.language")
        if (lang != "")
            content.localization().loadTranslation(lang)

        // Initializes windows screen to main menu ( and ask for login )
        gui.topLayer = LoginUI(gui, null)

        mainLoop()
        cleanup()

        System.exit(0)
    }

    private fun mainLoop() {
        while (!gameWindow.shouldClose) {
            gameWindow.executeMainThreadChores()
            gameWindow.checkStillInFocus()

            GLFW.glfwPollEvents()
            inputsManager.updateInputs()
            soundManager.updateAllSoundSources()

            ingame?.onFrame()

            graphics.renderFrame()
        }

        ingame?.exitToMainMenu()
    }

    fun cleanup() {
        graphics.cleanup()
        inputsManager.cleanup()
        tasks.cleanup()
        configuration.save()
    }

    fun reloadAssets() {
        content.reload()
        inputsManager.reload()
        //TODO hook some rendering stuff in here
    }
}
