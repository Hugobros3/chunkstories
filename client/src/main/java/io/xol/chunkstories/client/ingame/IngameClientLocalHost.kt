package io.xol.chunkstories.client.ingame

import io.xol.chunkstories.api.events.player.PlayerLogoutEvent
import io.xol.chunkstories.api.player.Player
import io.xol.chunkstories.api.server.PermissionsManager
import io.xol.chunkstories.api.server.Server
import io.xol.chunkstories.api.util.IterableIterator
import io.xol.chunkstories.api.world.WorldInfo
import io.xol.chunkstories.api.world.WorldMaster
import io.xol.chunkstories.client.ClientImplementation
import io.xol.chunkstories.client.ClientMasterPluginManager
import io.xol.chunkstories.entity.SerializedEntityFile
import io.xol.chunkstories.server.FileBasedUsersPrivileges
import io.xol.chunkstories.world.WorldClientLocal
import io.xol.chunkstories.world.WorldLoadingException
import io.xol.chunkstories.world.deserializeWorldInfo
import io.xol.chunkstories.world.serializeWorldInfo
import org.slf4j.LoggerFactory
import java.io.File

fun ClientImplementation.enterExistingWorld(folder: File) : WorldClientLocal {
    if (!folder.exists() || !folder.isDirectory)
        throw WorldLoadingException("The folder $folder doesn't exist !")

    val worldInfoFile = File(folder.path + "/worldInfo.dat")
    if (!worldInfoFile.exists())
        throw WorldLoadingException("The folder $folder doesn't contain a worldInfo.dat file !")

    val worldInfo = deserializeWorldInfo(worldInfoFile)
    logger().debug("Entering world $worldInfo")

    // Create the context for the local server
    val localHostCtx = IngameClientLocalHost(this) {
        WorldClientLocal(it as IngameClientLocalHost, worldInfo, folder)
    }
    this.ingame = localHostCtx
    return localHostCtx.world
}

fun ClientImplementation.createAndEnterWorld(folder: File, worldInfo: WorldInfo) : WorldClientLocal {
    if(folder.exists())
        throw Exception("The folder $folder already exists !")

    logger().debug("Creating new singleplayer world")
    folder.mkdirs()
    val worldInfoFile = File(folder.path + "/worldInfo.dat")
    worldInfoFile.writeText(serializeWorldInfo(worldInfo, true))
    logger().debug("Created directory & wrote worldInfo.dat ; now entering world")

    return enterExistingWorld(folder)
}

/** Represent an IngameClient that is also a local Server (with minimal server functionality). Used in local SP. */
class IngameClientLocalHost(client: ClientImplementation, worldInitializer: (IngameClientImplementation) -> WorldClientLocal) : IngameClientImplementation(client, worldInitializer), Server {
    override val world: WorldClientLocal = super.internalWorld as WorldClientLocal
    override val pluginManager: ClientMasterPluginManager = super.internalPluginManager as ClientMasterPluginManager

    override val userPrivileges = FileBasedUsersPrivileges()
    override var permissionsManager: PermissionsManager = PermissionsManager { _, _ ->
        true // TODO implement something better
    }

    /** When exiting a localhost world, ensure to save everything */
    override fun exitCommon() {
        if (internalWorld is WorldMaster) {
            // Stop the world clock so hopefully as to freeze it's state
            internalWorld.stopLogic().traverse()

            player.save()

            // Save everything the world contains
            internalWorld.saveEverything().traverse()
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

    override val connectedPlayers: IterableIterator<Player>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

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