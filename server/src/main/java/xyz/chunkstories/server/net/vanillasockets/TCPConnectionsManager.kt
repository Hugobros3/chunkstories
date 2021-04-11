//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//
package xyz.chunkstories.server.net.vanillasockets

import xyz.chunkstories.server.DedicatedServer
import xyz.chunkstories.server.DedicatedServerOptions.networkPort
import xyz.chunkstories.server.net.ConnectionsManager
import java.io.IOException
import java.lang.Exception
import java.net.ServerSocket
import java.net.SocketException

class TCPConnectionsManager(server: DedicatedServer?) : ConnectionsManager(server!!) {
    private lateinit var socketThread: SocketThread

    internal inner class SocketThread(private val serverSocket: ServerSocket) : Thread() {
        override fun run() {
            name = "TCP socket accepting thread"
            try {
                while (true) {
                    val newSocket = serverSocket.accept()
                    val acceptedConnection = TCPClientConnection(server, this@TCPConnectionsManager, newSocket)
                    when {
                        server.userPrivileges.bannedIps.contains(acceptedConnection.remoteAddress) -> acceptedConnection.disconnect("Banned IP address - " + acceptedConnection.remoteAddress)
                        connections.size > maxClients -> acceptedConnection.disconnect("Server is full")
                        else -> connections.add(acceptedConnection)
                    }
                }
            } catch (se: SocketException) { /* this is thrown when we close the socket, and kills the loop */ }
        }

        fun close() {
            serverSocket.close()
        }
    }

    override fun open() {
        try {
            val serverSocket = ServerSocket(server.config.getIntValue(networkPort))
            socketThread = SocketThread(serverSocket)
            socketThread.start()
            server.logger.info("Started server on port " + serverSocket.localPort + ", ip=" + serverSocket.inetAddress)
        } catch (e: IOException) {
            server.logger.error("Can't open server socket. Double check that there is no other instance already running or an application using server port.", e)
            throw Exception("Failed to open sever for incoming connections")
        }
    }

    override fun terminate() {
        super.terminate()
        socketThread.close()
    }
}