package xyz.chunkstories.client.ingame

import org.slf4j.Logger
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.server.PermissionsManager
import xyz.chunkstories.api.server.Host
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.client.ClientImplementation
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.Engine
import xyz.chunkstories.api.content.ContentTranslator
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.player.PlayerID
import xyz.chunkstories.api.world.World
import xyz.chunkstories.world.*
import java.io.File

fun ClientImplementation.enterExistingWorld(folder: File) {
    if (!folder.exists() || !folder.isDirectory)
        throw WorldLoadingException("The folder $folder doesn't exist !")

    val worldInfoFile = File(folder.path + "/" + WorldImplementation.worldPropertiesFilename)
    if (!worldInfoFile.exists())
        throw WorldLoadingException("The folder $folder doesn't contain a ${WorldImplementation.worldPropertiesFilename} file !")

    val worldInfo = deserializeWorldInfo(worldInfoFile)
    logger.debug("Entering world $worldInfo")

    // Create the context for the local server
    val localHostCtx = IngameClientLocalHost(this) {
        loadWorld(it as IngameClientLocalHost,  folder)
    }
    localHostCtx.world.startLogic()
    this.ingame = localHostCtx
}

fun ClientImplementation.createAndEnterWorld(folder: File, properties: World.Properties) {
    if (folder.exists())
        throw Exception("The folder $folder already exists !")

    logger.debug("Creating new singleplayer world")
    initializeWorld(folder, properties)
    logger.debug("Created new world '${properties.name}' ; now entering world")

    enterExistingWorld(folder)
}

/** Represent an IngameClient that is also a local Server (with minimal server functionality). Used in local SP. */
class IngameClientLocalHost(client: ClientImplementation, worldInitializer: (IngameClientImplementation) -> WorldImplementation) : IngameClientImplementation(client, worldInitializer), Host {
    override val world: WorldMasterImplementation = super.world_ as WorldMasterImplementation
    override val contentTranslator: ContentTranslator
        get() = world.contentTranslator
    override val logger: Logger = LoggerFactory.getLogger("client.world")

    override var permissionsManager: PermissionsManager = object : PermissionsManager {
        override fun hasPermission(player: Player, permissionNode: String): Boolean {
            //TODO have an actual permissions system
            return true
        }
    }

    /** When exiting a localhost world, ensure to save everything */
    override fun exitCommon() {
        if (world_ is WorldMaster) {
            // Stop the world clock so hopefully as to freeze it's state
            world_.stopLogic().traverse()

            val playerWorldMetadata = world.playersMetadata[player.id]!!

            player.destroy()
            // Save everything the world contains
            //internalWorld.saveEverything().traverse()
        }
        super.exitCommon()
    }

    override fun startPlayingAs_(entity: Entity) {
        TODO("Not yet implemented")
    }

    override fun startSpectating_() {
        TODO("Not yet implemented")
    }

    override fun getPlayer(playerName: String): Player? {
        if (playerName == player.name)
            return player
        return null
    }

    override fun getPlayer(id: PlayerID): Player? {
        if (id == player.id)
            return player
        return null
    }

    /*override fun Player.disconnect(disconnectMessage: String) {
        throw UnsupportedOperationException()
    }*/

    override fun broadcastMessage(message: String) {
        print(message)
    }

    override val players = setOf(player).asSequence()
}