package xyz.chunkstories.client.ingame

import xyz.chunkstories.api.events.player.PlayerLogoutEvent
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.server.PermissionsManager
import xyz.chunkstories.api.server.Server
import xyz.chunkstories.api.world.WorldInfo
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.client.ClientImplementation
import xyz.chunkstories.entity.SerializedEntityFile
import xyz.chunkstories.server.FileBasedUsersPrivileges
import org.slf4j.LoggerFactory
import xyz.chunkstories.plugin.DefaultPluginManager
import xyz.chunkstories.world.*
import java.io.File
import java.util.*
import java.util.function.BinaryOperator
import kotlin.streams.toList

fun ClientImplementation.enterExistingWorld(folder: File) {
    if (!folder.exists() || !folder.isDirectory)
        throw WorldLoadingException("The folder $folder doesn't exist !")

    val worldInfoFile = File(folder.path + "/" + WorldImplementation.worldInfoFilename)
    if (!worldInfoFile.exists())
        throw WorldLoadingException("The folder $folder doesn't contain a ${WorldImplementation.worldInfoFilename} file !")

    val worldInfo = deserializeWorldInfo(worldInfoFile)
    logger().debug("Entering world $worldInfo")

    // Create the context for the local server
    val localHostCtx = IngameClientLocalHost(this) {
        WorldClientLocal(it as IngameClientLocalHost, worldInfo, folder)
    }
    localHostCtx.world.startLogic()
    this.ingame = localHostCtx
}

fun ClientImplementation.createAndEnterWorld(folder: File, worldInfo: WorldInfo) {
    if(folder.exists())
        throw Exception("The folder $folder already exists !")

    logger().debug("Creating new singleplayer world")
    folder.mkdirs()
    val worldInfoFile = File(folder.path + "/" + WorldImplementation.worldInfoFilename)
    worldInfoFile.writeText(serializeWorldInfo(worldInfo, true))

    val internalData = WorldInternalData()
    val random = Random((worldInfo.seed + "_spawn").codePoints().toList().reduce(Int::xor).toLong())
    val randomWeather = random.nextFloat()
    internalData.weather = randomWeather
    val spawnCoordinateX = random.nextInt(worldInfo.size.sizeInChunks * 32)
    val spawnCoordinateZ = random.nextInt(worldInfo.size.sizeInChunks * 32)
    internalData.spawnLocation.set(spawnCoordinateX + 0.5, 64.0, spawnCoordinateZ + 0.5)

    val internalDataFile = File(folder.path + "/" + WorldImplementation.worldInternalDataFilename)
    internalData.writeToDisk(internalDataFile)
    logger().debug("Created new world '${worldInfo.name}' ; now entering world")

    enterExistingWorld(folder)
}

/** Represent an IngameClient that is also a local Server (with minimal server functionality). Used in local SP. */
class IngameClientLocalHost(client: ClientImplementation, worldInitializer: (IngameClientImplementation) -> WorldClientLocal) : IngameClientImplementation(client, worldInitializer), Server {
    override val world: WorldClientLocal = super.internalWorld as WorldClientLocal
    override val pluginManager: DefaultPluginManager
            get() = super.internalPluginManager

    override val userPrivileges = FileBasedUsersPrivileges()
    override var permissionsManager: PermissionsManager = object: PermissionsManager {
        override fun hasPermission(player: Player, permissionNode: String): Boolean {
            //TODO have an actual permissions system
            return true
        }
    }

    /** When exiting a localhost world, ensure to save everything */
    override fun exitCommon() {
        if (internalWorld is WorldMaster) {
            // Stop the world clock so hopefully as to freeze it's state
            internalWorld.stopLogic().traverse()

            player.save()

            player.loadingAgent.unloadEverything(true)
            // Save everything the world contains
            //internalWorld.saveEverything().traverse()
        }
        super.exitCommon()
    }

    override fun getPlayerByUUID(UUID: Long): Player? {
        if (UUID == player.uuid)
            return player
        return null
    }

    override fun reloadConfig() {
        // doesn't do shit
    }

    override fun getPlayerByName(playerName: String): Player? {
        if (playerName == player.name)
            return player
        return null
    }

    override fun broadcastMessage(message: String) {
        print(message)
    }

    override val connectedPlayers = setOf(player)

    override val connectedPlayersCount: Int = 1
    override val publicIp: String = "127.0.0.1"
    override val uptime: Long = -1L

    private fun LocalPlayerImplementation.save() {
        val playerDisconnectionEvent = PlayerLogoutEvent(this)
        this.client.pluginManager.fireEvent(playerDisconnectionEvent)

        val playerEntity = this.controlledEntity
        if (playerEntity != null) {
            val playerEntityFile = SerializedEntityFile(
                    world.folderPath + "/players/" + this.name.toLowerCase() + ".csf")
            playerEntityFile.write(playerEntity)
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger("client.world")
    }
}