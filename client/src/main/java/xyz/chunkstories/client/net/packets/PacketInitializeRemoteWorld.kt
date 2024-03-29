//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.client.net.packets

import xyz.chunkstories.api.Engine
import xyz.chunkstories.api.server.UserConnection
import xyz.chunkstories.net.packets.PacketSendWorldInfo
import xyz.chunkstories.world.deserializeWorldInfo
import java.io.DataInputStream
import java.io.IOException

class PacketInitializeRemoteWorld(engine: Engine) : PacketSendWorldInfo(engine) {

    @Throws(IOException::class)
    override fun receive(dis: DataInputStream, user: UserConnection?) {
        val initializationString = dis.readUTF()

        worldInfo = deserializeWorldInfo(initializationString)

        /*if (processor is ClientPacketsEncoderDecoder) {
            processor.logger().info("Received World initialization packet")

            val contentTranslator = processor.contentTranslator
            if (contentTranslator == null) {
                processor.logger().error("Can't initialize a world without a ContentTranslator initialized first!")
                return
            }

            val sequence = processor.connection.connectionSequence
            val client = sequence.client

            val ingame = IngameClientRemoteHost(client, processor.connection) {
                WorldClientRemote(it as IngameClientRemoteHost, worldInfo, processor.contentTranslator as AbstractContentTranslator, processor.connection)
            }
            ingame.world.startLogic()
            client.ingame = ingame

            sequence.worldSemaphore.release()

            //TODO("remake this mechanism but make it actually any good")
            //throw new RuntimeException("TODO");
            //val client = processor.client

                    /*val client = processor.context as IngameClientRemoteHost
                    (client.gameWindow as GLFWWindow).mainThread {
                        val world: WorldClientRemote
                        try {
                            world = WorldClientRemote(client, worldInfo, contentTranslator as AbstractContentTranslator, processor.connection)
                            client.gui.topLayer = IngameLayer()
                            //(client as ClientImplementation).changeWorld(world)

                            processor.connection.handleSystemRequest("world/ok")
                        } catch (e: WorldLoadingException) {
                            client.exitToMainMenu(e.message!!)
                        }
                    }*/
        } else {
            error("shit")
        }*/
        TODO()
    }
}
