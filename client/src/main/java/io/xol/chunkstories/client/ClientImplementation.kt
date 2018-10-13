//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.client

import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar

import io.xol.chunkstories.api.GameContext
import io.xol.chunkstories.api.client.Client
import io.xol.chunkstories.api.gui.Gui
import io.xol.chunkstories.api.plugin.PluginManager
import io.xol.chunkstories.client.glfw.GLFWWindow
import io.xol.chunkstories.client.ingame.IngameClientImplementation
import io.xol.chunkstories.content.GameContentStore
import io.xol.chunkstories.gui.ClientGui
import io.xol.chunkstories.sound.ALSoundManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import io.xol.chunkstories.Constants
import io.xol.chunkstories.api.client.ClientIdentity
import io.xol.chunkstories.api.util.Configuration
import io.xol.chunkstories.content.GameDirectory
import io.xol.chunkstories.gui.layer.LoginPrompt
import io.xol.chunkstories.gui.layer.SkyBoxBackground
import io.xol.chunkstories.input.lwjgl3.Lwjgl3ClientInputsManager
import io.xol.chunkstories.util.LogbackSetupHelper
import io.xol.chunkstories.util.VersionInfo

/** Client implementation entry point, is the root of the systems and holds state through them  */
class ClientImplementation internal constructor(coreContentLocation: File, modsStringArgument: String?) : Client, GameContext {
    private val logger: Logger
    private val chatLogger = LoggerFactory.getLogger("game.chat")

    private val configFile: File = File("./config/client.config")
    override val configuration: Configuration = Configuration(this, configFile)

    override val content: GameContentStore
    override val tasks: ClientTasksPool

    override val gameWindow: GLFWWindow
    override val soundManager: ALSoundManager

    override val gui = ClientGui(this)

    override var ingame: IngameClientImplementation? = null

    override val inputsManager: Lwjgl3ClientInputsManager
        get() = gameWindow.inputsManager

    override val pluginManager: PluginManager
        get() = throw UnsupportedOperationException("There is no plugin manager in a non-ingame context ! Use an IngameClient object instead.")

    override lateinit var user: ClientIdentity

    init {
        // Name the thread
        Thread.currentThread().name = "Main thread"
        Thread.currentThread().priority = Constants.MAIN_THREAD_PRIORITY

        // Start logging system
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("YYYY.MM.dd HH.mm.ss")
        val time = sdf.format(cal.time)

        logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
        val loggingFilename = GameDirectory.getGameFolderPath() + "/logs/" + time + ".log"
        LogbackSetupHelper(loggingFilename)

        soundManager = ALSoundManager(this)

        // Creates game window, no use of any user content up to this point
        gameWindow = GLFWWindow(this, "Chunk Stories " + VersionInfo.version)

        // Create game content manager
        content = GameContentStore(this, coreContentLocation, modsStringArgument)
        content.reload()

        //gameWindow.stage_2_init(); // TODO this is bs, don't need this plz
        configuration.load()

        inputsManager.reload()

        // Spawns worker threads
        var nbThreads : Int = configuration.getIntValue("client.performance.workerThreads")

        if (nbThreads <= 0) {
            nbThreads = Runtime.getRuntime().availableProcessors() / 2

            // Fail-safe
            if (nbThreads < 1)
                nbThreads = 1
        }

        tasks = ClientTasksPool(this, nbThreads)
        tasks.start()

        // Load the correct language
        val lang : String = configuration.getValue("client.game.language")
        if (lang != "")
            content.localization().loadTranslation(lang)

        // Initlializes windows screen to main menu ( and ask for login )
        gui.topLayer = LoginPrompt(gui, SkyBoxBackground(gui))

        gameWindow.mainLoop()
        cleanup()
    }

    fun cleanup() {
        tasks.destroy()
        configuration.save()
    }

    fun reloadAssets() {
        content.reload()
        gameWindow.inputsManager.reload()
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
                if (launchArgument == "--forceobsolete") {

                    ClientLimitations.ignoreObsoleteHardware = false
                    println(
                            "Ignoring hardware checks. This is absolutely definitely not going to make the game run, proceed at your own risk of imminent failure." + "You are stripped of any tech support rights when running the game using this.")
                } else if (launchArgument.contains("--mods")) {
                    modsStringArgument = launchArgument.replace("--mods=", "")
                } else if (launchArgument.contains("--dir")) {
                    GameDirectory.set(launchArgument.replace("--dir=", ""))
                } else if (launchArgument.contains("--core")) {
                    val coreContentLocationPath = launchArgument.replace("--core=", "")
                    coreContentLocation = File(coreContentLocationPath)
                } else {
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

            ClientImplementation(coreContentLocation, modsStringArgument)

            // Not supposed to happen, gets there when ClientImplementation crashes badly.
            System.exit(-1)
        }
    }
}
