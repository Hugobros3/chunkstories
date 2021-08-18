//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server

import org.fusesource.jansi.AnsiConsole
import org.slf4j.Logger
import xyz.chunkstories.EngineImplemI
import xyz.chunkstories.api.Engine
import xyz.chunkstories.api.content.ContentTranslator
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.player.PlayerID
import xyz.chunkstories.api.plugin.commands.CommandEmitter
import xyz.chunkstories.api.server.PermissionsManager
import xyz.chunkstories.api.server.Host
import xyz.chunkstories.api.util.configuration.Configuration
import xyz.chunkstories.api.util.convertToAnsi
import xyz.chunkstories.api.world.World
import xyz.chunkstories.api.world.WorldSize
import xyz.chunkstories.content.GameContentStore
import xyz.chunkstories.content.mods.ModsManagerImplementation
import xyz.chunkstories.gameName
import xyz.chunkstories.plugin.DefaultPluginManager
import xyz.chunkstories.server.commands.DedicatedServerConsole
import xyz.chunkstories.server.commands.installHostCommands
import xyz.chunkstories.server.net.ConnectionsManager
import xyz.chunkstories.server.net.vanillasockets.TCPConnectionsManager
import xyz.chunkstories.server.propagation.ServerModsProvider
import xyz.chunkstories.setupLogFile
import xyz.chunkstories.task.WorkerThreadPool
import xyz.chunkstories.util.VersionInfo
import xyz.chunkstories.world.*
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

private val configFile = File("config/server.config")

fun main(args: Array<String>) {
    var coreContentLocation = File("core_content.zip")

    var requestedMods = emptyList<String>()
    for (argument in args) {
        when {
            argument.contains("--mods") -> {
                val modsString = argument.replace("--mods=", "")
                requestedMods = modsString.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            }
            argument.contains("--core") -> {
                val coreContentLocationPath = argument.replace("--core=", "")
                coreContentLocation = File(coreContentLocationPath)
            }
            else -> {
                var helpText = "Chunk Stories server " + VersionInfo.versionJson.verboseVersion + "\n"

                helpText += if (argument == "-h" || argument == "--help")
                    "Valid parameters: \n"
                else
                    "Unrecognized parameter: $argument\n"

                helpText += "--mods=xxx,yyy | -mods=* Tells the game to start with those mods enabled\n"
                helpText += "--dir=whatever Tells the game not to look for .chunkstories at it's normal location and instead use the argument\n"
                helpText += "--core=whaterverfolder/ or --core=whatever.zip Tells the game to use some specific folder or archive as it's base content.\n"

                println(helpText)
                return
            }
        }
    }

    DedicatedServer(coreContentLocation, requestedMods)
}

class DedicatedServer(coreContentLocation: File, requestedMods: List<String>) : Host, EngineImplemI {
    override val engine: Engine
        get() = this
    override val content: GameContentStore
    override val tasks: WorkerThreadPool

    val console = DedicatedServerConsole(this)

    val config = Configuration(configFile)
    val userPrivileges = UserPrivileges()
    override lateinit var permissionsManager: PermissionsManager

    override val pluginManager: DefaultPluginManager

    override val world: WorldMasterImplementation

    internal val connectionsManager: ConnectionsManager
    internal val modsProvider: ServerModsProvider

    internal val keepRunning = AtomicBoolean(true)

    private val initTimestamp = System.currentTimeMillis() / 1000
    val uptime: Long
        get() = System.currentTimeMillis() / 1000 - initTimestamp

    override val players: Sequence<Player>
        get() = connectionsManager.authenticatedPlayers.values.asSequence()
    override val contentTranslator: ContentTranslator
        get() = world.contentTranslator
    override val modsManager: ModsManagerImplementation
        get() = content.modsManager

    override val logger: Logger

    init {
        config.addOptions(DedicatedServerOptions.createOptions(this))
        config.load(configFile)
        AnsiConsole.systemInstall()

        logger = setupLogFile("server_logs")

        logger.info("Starting $gameName server " + VersionInfo.versionJson.verboseVersion + " (network protocol version " + VersionInfo.networkProtocolVersion + ")")

        content = GameContentStore(this, coreContentLocation, requestedMods)
        content.reload()

        // Spawns worker threads
        var nbThreads = this.config.getIntValue(DedicatedServerOptions.workerThreads)
        if (nbThreads <= 0) {
            nbThreads = Runtime.getRuntime().availableProcessors() - 2

            // Fail-safe
            if (nbThreads < 1)
                nbThreads = 1
        }

        tasks = WorkerThreadPool(nbThreads)
        tasks.start()

        connectionsManager = TCPConnectionsManager(this)
        modsProvider = ServerModsProvider(this)
        pluginManager = DefaultPluginManager(this)

        // Load the world(s)
        val worldName = config.getValue(DedicatedServerOptions.worldName)
        val worldPath = "worlds/$worldName"
        val worldDir = File(worldPath)
        if (!worldDir.exists()) {
            val internalName = worldName.replace("[^\\w\\s]".toRegex(), "_")
            val size = config.getValue(DedicatedServerOptions.worldSize).let { WorldSize.getWorldSize(it.toUpperCase()) }
                    ?: WorldSize.MEDIUM
            initializeWorld(worldDir, World.Properties(
                    internalName = internalName,
                    name = worldName,
                    description = "Automatically generated server map",
                    seed = Random().nextLong().toString(),
                    size = size,
                    generator = config.getValue(DedicatedServerOptions.worldGenerator),
                    spawn = TODO("generate a decent spawn point")
            ))
        }

        if (worldDir.exists()) {
            val worldInfoFile = File(worldDir.path + "/" + WorldImplementation.worldPropertiesFilename)
            if (!worldInfoFile.exists())
                throw WorldLoadingException("The folder $worldDir doesn't contain a ${WorldImplementation.worldPropertiesFilename} file !")

            val worldInfo = deserializeWorldInfo(worldInfoFile)

            world = loadWorld(this, worldDir)
        } else {
            throw Exception("Can't find the world $worldName in $worldPath.")
        }

        connectionsManager.open()

        permissionsManager = object : PermissionsManager {
            override fun hasPermission(player: Player, permissionNode: String) = userPrivileges.admins.contains(player.name)
        }

        // Load plugins
        pluginManager.reloadPlugins()
        installHostCommands(this)

        // Finally start logic
        // TODO("world.startTicking()")

        console.run()
        shutdown()
    }

    private fun shutdown() {
        // When stopped, close sockets and save config.
        logger.info("Stopping world logic")
        TODO("world.stopTicking()")

        logger.info("Killing all connections")
        connectionsManager.terminate()

        logger.info("Shutting down plugins ...")
        pluginManager.disablePlugins()

        logger.info("Saving map and waiting for IO to finish")
        world.saveEverything()
        world.ioThread.waitThenKill()
        world.destroy()

        logger.info("Saving configuration")
        config.save(configFile)
        userPrivileges.save()

        logger.info("Good night sweet prince")

        // TODO check this terminates OK
        // Runtime.getRuntime().exit(0)
    }

    internal fun requestShutdown() {
        tasks.cleanup()
        keepRunning.set(false)
    }

    internal fun reloadConfig() {
        config.load(configFile)
        userPrivileges.load()
    }

    override fun getPlayer(playerName: String): Player? = connectionsManager.getPlayerByName(playerName)
    override fun getPlayer(id: PlayerID): Player? = players.find { it.id == id }

    /*override fun Player.disconnect(disconnectMessage: String) {
        (this as ServerPlayer).disconnect(disconnectMessage)
    }*/

    override fun broadcastMessage(message: String) {
        logger.info(convertToAnsi(message))
        for (player in players) {
            player.sendMessage(message)
        }
    }

    fun dispatchCommand(emitter: CommandEmitter, command: String, arguments: Array<String>): Boolean {
        logger.info("[" + emitter.name + "] " + "Entered command : " + command)
        try {
            return pluginManager.dispatchCommand(emitter, command, arguments)
        } catch (e: Exception) {
            emitter.sendMessage("An error occurred: " + e.localizedMessage)
            logger.warn("Player command triggered an exception", e)
        }
        return false
    }

    override fun toString(): String {
        return "[ChunkStories Server " + VersionInfo.versionJson.verboseVersion + "]"
    }
}