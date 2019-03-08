//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server

import org.fusesource.jansi.Ansi.Color.*
import org.fusesource.jansi.Ansi.ansi
import org.fusesource.jansi.AnsiConsole
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.server.PermissionsManager
import xyz.chunkstories.api.server.Server
import xyz.chunkstories.api.util.ColorsTools
import xyz.chunkstories.api.util.configuration.Configuration
import xyz.chunkstories.api.workers.Tasks
import xyz.chunkstories.content.GameContentStore
import xyz.chunkstories.content.GameDirectory
import xyz.chunkstories.plugin.DefaultPluginManager
import xyz.chunkstories.server.commands.DedicatedServerConsole
import xyz.chunkstories.server.commands.installServerCommands
import xyz.chunkstories.server.net.ClientsManager
import xyz.chunkstories.server.net.announcer.ServerAnnouncerThread
import xyz.chunkstories.server.net.vanillasockets.VanillaClientsManager
import xyz.chunkstories.server.propagation.ServerModsProvider
import xyz.chunkstories.task.WorkerThreadPool
import xyz.chunkstories.util.LogbackSetupHelper
import xyz.chunkstories.util.VersionInfo
import xyz.chunkstories.world.WorldLoadingException
import xyz.chunkstories.world.WorldServer
import xyz.chunkstories.world.deserializeWorldInfo
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The server class handles and make the link between all server components It
 * also takes care of the command line input as it's the main thread, thought
 * the processing of command lines is handled by ServerConsole.java
 */
class DedicatedServer internal constructor(coreContentLocation: File, modsString: String?) : Runnable, Server {
    private val gameContent: GameContentStore
    private val workers: WorkerThreadPool

    val console = DedicatedServerConsole(this)

    private val configFile = File("./config/server.config")
    val serverConfig = Configuration(this, configFile)

    private val running = AtomicBoolean(true)
    val isRunning: Boolean
        get() = running.get()

    private val initTimestamp = System.currentTimeMillis() / 1000

    override lateinit var world: WorldServer private set

    val handler: ClientsManager

    override val userPrivileges = FileBasedUsersPrivileges()
    override lateinit var permissionsManager: PermissionsManager

    // Sleeper thread to keep servers list updated
    private var announcer: ServerAnnouncerThread? = null

    // What mods are required to join this server ?
    val modsProvider: ServerModsProvider

    //private var pluginsManager: DefaultServerPluginManager? = null
    override val pluginManager: DefaultPluginManager

    override val connectedPlayers: Set<Player>
        get() = handler.players

    override val connectedPlayersCount: Int
        get() = handler.playersNumber

    override val uptime: Long
        get() = System.currentTimeMillis() / 1000 - initTimestamp

    override val content: Content
        get() = gameContent

    override
            /** Dedicated servers openly broadcast their public IP  */
    val publicIp: String
        get() = this.handler.ip

    override val tasks: Tasks
        get() = workers

    init {
        AnsiConsole.systemInstall()

        // Start server services
        try {
            // Initialize logs to a file bearing the current date
            val cal = Calendar.getInstance()
            val sdf = SimpleDateFormat("YYYY.MM.dd HH.mm.ss")
            val time = sdf.format(cal.time)

            logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)

            val loggingFilename = GameDirectory.getGameFolderPath() + "/serverlogs/" + time + ".log"
            LogbackSetupHelper(loggingFilename)

            logger!!.info("Starting Chunkstories server " + VersionInfo.version + " network protocol version "
                    + VersionInfo.networkProtocolVersion)

            // Loads the mods/build filesystem
            gameContent = GameContentStore(this, coreContentLocation, modsString)
            gameContent.reload()

            // Spawns worker threads
            var nbThreads = this.serverConfig.getIntValue("server.performance.workerThreads")
            if (nbThreads <= 0) {
                nbThreads = Runtime.getRuntime().availableProcessors() - 2

                // Fail-safe
                if (nbThreads < 1)
                    nbThreads = 1
            }

            workers = WorkerThreadPool(nbThreads)
            workers.start()

            handler = VanillaClientsManager(this)

            modsProvider = ServerModsProvider(this)

            // load users privs
            // UsersPrivilegesFile.load();
            pluginManager = DefaultPluginManager(this)

            // Load the world(s)
            val worldName = serverConfig.getValue("server.world")
            val worldPath = GameDirectory.getGameFolderPath() + "/worlds/" + worldName
            val worldDir = File(worldPath)
            if (worldDir.exists()) {
                val worldInfoFile = File(worldDir.path + "/worldInfo.dat")
                if (!worldInfoFile.exists())
                    throw WorldLoadingException("The folder \$folder doesn't contain a worldInfo.dat file !")

                val worldInfo = deserializeWorldInfo(worldInfoFile)

                world = WorldServer(this, worldInfo, worldDir)
            } else {
                serverConfig.save(configFile)
                println("Can't find the world \"$worldName\" in $worldPath. Exiting !")
                Runtime.getRuntime().exit(0)
            }

            // Opens socket and starts accepting clients
            handler.open()
            // Initializes the announcer ( server listings )
            announcer = ServerAnnouncerThread(this)
            announcer!!.start()

            permissionsManager = object : PermissionsManager {
                override fun hasPermission(player: Player, permissionNode: String) = userPrivileges.isUserAdmin(player.name)
            }

            // Load plugins
            pluginManager.reloadPlugins()
            installServerCommands(this)

            // Finally start logic
            world.startLogic()
        } catch (e: Exception) {
            logger.error("Could not initialize server . Stacktrace below")
            throw RuntimeException(e)
        }

    }

    //TODO move to another class
    override// Just a command prompt, the actual server threads run in the background !
    fun run() {
        val br = BufferedReader(InputStreamReader(System.`in`))
        print("> ")
        while (running.get()) {
            try {
                // wait until we have data to complete a readLine()
                while (!br.ready() && running.get()) {
                    printTopScreenDebug()

                    Thread.sleep(1000L)
                }
                if (!running.get())
                    break
                val unparsedCommandText = br.readLine() ?: continue
                try {
                    // Parse and fire
                    var cmdName = unparsedCommandText.toLowerCase()
                    var args = arrayOf<String>()
                    if (unparsedCommandText.contains(" ")) {
                        cmdName = unparsedCommandText.substring(0, unparsedCommandText.indexOf(" "))
                        args = unparsedCommandText
                                .substring(unparsedCommandText.indexOf(" ") + 1, unparsedCommandText.length)
                                .split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    }

                    console.dispatchCommand(console, cmdName, args)

                    print("> ")
                    System.out.flush()
                } catch (e: Exception) {
                    println("error while handling command :")
                    e.printStackTrace()
                }

            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                println("ConsoleInputReadTask() cancelled")
                break
            }

        }
        try {
            br.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        closeServer()
    }

    private fun printTopScreenDebug() {
        var txt = "" + ansi().fg(BLACK).bg(CYAN)

        var ec = 0
        val i = world.allLoadedEntities
        while (i.hasNext()) {
            i.next()
            ec++
        }

        val maxRam = Runtime.getRuntime().maxMemory() / (1024 * 1024)
        val freeRam = Runtime.getRuntime().freeMemory() / (1024 * 1024)
        val usedRam = maxRam - freeRam

        txt += "ChunkStories server " + VersionInfo.version
        txt += " | fps:" + world.gameLogic.simulationFps
        txt += " | ent:$ec"
        txt += " | players:" + this.handler.playersNumber + "/" + this.handler.maxClients
        txt += (" | lc:" + this.world.regionsStorage.regionsList.count() + " ls:"
                + this.world.regionsSummariesHolder.all().count())
        txt += " | ram:$usedRam/$maxRam"
        txt += " | " + this.workers.toShortString()
        txt += " | ioq:" + this.world.ioHandler.size

        txt += ansi().bg(BLACK).fg(WHITE)

        print(
                ansi().saveCursorPosition().cursor(0, 0).eraseLine().fg(RED).toString() + txt + ansi().restoreCursorPosition())
        System.out.flush()
    }

    private fun closeServer() {
        // When stopped, close sockets and save config.
        logger.info("Stopping world logic")

        logger.info("Killing all connections")
        handler.close()

        logger.info("Shutting down plugins ...")
        pluginManager.disablePlugins()

        logger.info("Saving map and waiting for IO to finish")
        world.saveEverything()
        world.ioHandler.waitThenKill()
        world.destroy()

        logger.info("Saving configuration")
        serverConfig.save(configFile)
        userPrivileges.save()

        logger.info("Good night sweet prince")
        Runtime.getRuntime().exit(0)
    }

    fun stop() {
        announcer!!.stopAnnouncer()
        workers.destroy()

        // When stopped, close sockets and save config.
        running.set(false)
    }

    override fun reloadConfig() {
        userPrivileges.load()
        serverConfig.load(configFile)
    }

    override fun toString(): String {
        return "[ChunkStories Server " + VersionInfo.version + "]"
    }

    override fun getPlayerByName(playerName: String): Player? = handler.getPlayerByName(playerName)
    override fun getPlayerByUUID(UUID: Long): Player? = connectedPlayers.find { it.uuid == UUID }

    override fun broadcastMessage(message: String) {
        logger().info(ColorsTools.convertToAnsi(message))
        for (player in connectedPlayers) {
            player.sendMessage(message)
        }
    }

    override fun print(message: String) {
        logger!!.info(message)
    }

    override fun logger(): Logger {
        return logger
    }

    companion object {
        lateinit var logger: Logger

        @JvmStatic
        fun main(args: Array<String>) {
            var coreContentLocation = File("core_content.zip")

            var modsString: String? = null
            for (s in args)
            // Debug arguments
            {
                if (s.contains("--mods")) {
                    modsString = s.replace("--mods=", "")
                } else if (s.contains("--dir")) {
                    GameDirectory.set(s.replace("--dir=", ""))
                } else if (s.contains("--core")) {
                    val coreContentLocationPath = s.replace("--core=", "")
                    coreContentLocation = File(coreContentLocationPath)
                } else {
                    var helpText = "Chunk Stories server " + VersionInfo.version + "\n"

                    if (s == "-h" || s == "--help")
                        helpText += "Command line help: \n"
                    else
                        helpText += "Unrecognized command: $s\n"

                    helpText += "--mods=xxx,yyy | -mods=* Tells the game to start with those mods enabled\n"
                    helpText += "--dir=whatever Tells the game not to look for .chunkstories at it's normal location and instead use the argument\n"
                    helpText += "--core=whaterverfolder/ or --core=whatever.zip Tells the game to use some specific folder or archive as it's base content.\n"

                    println(helpText)
                    return

                    // Runtime.getRuntime().exit(0);
                }
            }

            val server = DedicatedServer(coreContentLocation, modsString)
            server.run()
        }
    }
}
