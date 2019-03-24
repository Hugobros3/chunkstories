//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.client

import org.lwjgl.glfw.GLFW
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.chunkstories.Constants
import xyz.chunkstories.api.GameContext
import xyz.chunkstories.api.client.Client
import xyz.chunkstories.api.client.ClientIdentity
import xyz.chunkstories.api.entity.traits.serializable.TraitControllable
import xyz.chunkstories.api.plugin.PluginManager
import xyz.chunkstories.api.util.configuration.Configuration
import xyz.chunkstories.client.glfw.GLFWWindow
import xyz.chunkstories.client.ingame.IngameClientImplementation
import xyz.chunkstories.content.GameContentStore
import xyz.chunkstories.content.GameDirectory
import xyz.chunkstories.graphics.GraphicsEngineImplementation
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.gui.ClientGui
import xyz.chunkstories.gui.layer.LoginPrompt
import xyz.chunkstories.input.lwjgl3.Lwjgl3ClientInputsManager
import xyz.chunkstories.sound.ALSoundManager
import xyz.chunkstories.util.LogbackSetupHelper
import xyz.chunkstories.util.VersionInfo
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/** Client implementation entry point, is the root of the systems and holds state through them  */
class ClientImplementation internal constructor(coreContentLocation: File, modsStringArgument: String?) : Client, GameContext {
    private val logger: Logger
    private val chatLogger = LoggerFactory.getLogger("game.chat")

    private val configFile: File = File("./config/client.config")
    override val configuration: Configuration = Configuration(this, configFile)

    override val content: GameContentStore
    override val tasks: ClientTasksPool


    override val graphics: GraphicsEngineImplementation
    override val inputsManager: Lwjgl3ClientInputsManager

    override val gameWindow: GLFWWindow
        get() = graphics.window

    override val soundManager: ALSoundManager

    override val gui = ClientGui(this)

    override var ingame: IngameClientImplementation? = null


    override val pluginManager: PluginManager
        get() = throw UnsupportedOperationException("There is no plugin manager in a non-ingame context ! Use an IngameClient object instead.")

    override lateinit var user: ClientIdentity

    init {
        // Name the thread
        Thread.currentThread().name = "Main thread"
        Thread.currentThread().priority = Constants.MAIN_THREAD_PRIORITY

        configuration.addOptions(InternalClientOptions.options)

        // Start logging system
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("YYYY.MM.dd HH.mm.ss")
        val time = sdf.format(cal.time)

        logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
        val loggingFilename = GameDirectory.getGameFolderPath() + "/logs/" + time + ".log"
        LogbackSetupHelper(loggingFilename)

        soundManager = ALSoundManager(this)

        graphics = GraphicsEngineImplementation(this)
        inputsManager = Lwjgl3ClientInputsManager(gameWindow)

        // Create game content manager
        content = GameContentStore(this, coreContentLocation, modsStringArgument)
        content.reload()

        //gameWindow.stage_2_init(); // TODO this is bs, don't need this plz
        configuration.load()

        inputsManager.reload()

        // Spawns worker threads
        var nbThreads: Int = configuration.getIntValue("client.performance.workerThreads")

        if (nbThreads <= 0) {
            nbThreads = Runtime.getRuntime().availableProcessors() / 2

            // Fail-safe
            if (nbThreads < 1)
                nbThreads = 1
        }

        tasks = ClientTasksPool(this, nbThreads)
        tasks.start()

        // Load the correct language
        val lang: String = configuration.getValue("client.game.language")
        if (lang != "")
            content.localization().loadTranslation(lang)

        // Initlializes windows screen to main menu ( and ask for login )
        gui.topLayer = LoginPrompt(gui, null)

        mainLoop()
        cleanup()
    }

    fun mainLoop() {
        while (!gameWindow.shouldClose) {
            gameWindow.executeMainThreadChores()
            gameWindow.checkStillInFocus()

            GLFW.glfwPollEvents()
            inputsManager.updateInputs()

            soundManager.updateAllSoundSources()

            ingame?.player?.controlledEntity?.let { it.traits[TraitControllable::class]?.onEachFrame() }

            graphics.renderGame()
        }

        ingame?.exitToMainMenu()
    }

    fun cleanup() {
        graphics.cleanup()
        inputsManager.cleanup()
        tasks.destroy()
        configuration.save()
    }

    fun reloadAssets() {
        content.reload()
        inputsManager.reload()
        //TODO hook some rendering stuff in here
    }

    override fun print(message: String) {
        chatLogger.info(message)
    }

    override fun logger(): Logger {
        return this.logger
    }

    companion object {

        @JvmStatic
        fun main(launchArguments: Array<String>) {
            // Check for folder
            GameDirectory.check()
            GameDirectory.initClientPath()

            //osx dirty fix
            System.setProperty("java.awt.headless", "true")

            var coreContentLocation = File("core_content.zip")

            var modsStringArgument: String? = null
            for (launchArgument in launchArguments) {
                when {
                    launchArgument == "--forceobsolete" -> //TODO ClientLimitations.ignoreObsoleteHardware = false
                        println("Ignoring hardware checks. This is absolutely definitely not going to make the game run, proceed at your own risk of imminent failure." + "You are stripped of any tech support rights when running the game using this.")
                    launchArgument.contains("--mods") -> modsStringArgument = launchArgument.replace("--mods=", "")
                    launchArgument.contains("--dir") -> GameDirectory.set(launchArgument.replace("--dir=", ""))
                    launchArgument.contains("--enableValidation") -> VulkanGraphicsBackend.useValidationLayer = true
                    launchArgument.contains("--core") -> {
                        val coreContentLocationPath = launchArgument.replace("--core=", "")
                        coreContentLocation = File(coreContentLocationPath)
                    }
                    else -> {
                        var helpText = "Chunk Stories client " + VersionInfo.version + "\n"

                        if (launchArgument == "-h" || launchArgument == "--help")
                            helpText += "Command line help: \n"
                        else
                            helpText += "Unrecognized command: $launchArgument\n"

                        helpText += "--forceobsolete Forces the game to run even if requirements aren't met. No support will be offered when using this! \n"
                        helpText += "--mods=xxx,yyy | -mods=* Tells the game to start with those mods enabled\n"
                        helpText += "--dir=whatever Tells the game not to look for .chunkstories at it's normal location and instead use the argument\n"
                        helpText += "--core=whaterverfolder/ or --core=whatever.zip Tells the game to use some specific folder or archive as it's base content.\n"

                        println(helpText)
                        return
                    }
                }
            }

            ClientImplementation(coreContentLocation, modsStringArgument)

            // Not supposed to happen, gets there when ClientImplementation crashes badly.
            //System.exit(-1)
        }
    }
}
