//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.client.net

import xyz.chunkstories.EngineImplemI
import xyz.chunkstories.api.Engine
import xyz.chunkstories.api.entity.Subscriber
import xyz.chunkstories.net.Connection

/** A connection from a client *to* a server.  */
abstract class ServerConnection(engine: EngineImplemI, val connectionSequence: ClientConnectionSequence) : Connection(engine, connectionSequence.serverAddress, connectionSequence.serverPort) {
    abstract val remoteServer: Subscriber

    open fun onDisconnect(reason: String) {

    }
}