//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.client.net

import xyz.chunkstories.api.net.RemoteServer
import xyz.chunkstories.net.Connection

/** A connection from a client *to* a server.  */
abstract class ServerConnection(val connectionSequence: ClientConnectionSequence) : Connection(connectionSequence.serverAddress, connectionSequence.serverPort) {
    abstract val remoteServer: RemoteServer

    open fun onDisconnect(reason: String) {

    }
}