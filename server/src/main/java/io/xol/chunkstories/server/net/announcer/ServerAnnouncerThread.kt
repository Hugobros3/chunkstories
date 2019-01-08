//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.server.net.announcer

import io.xol.chunkstories.api.util.configuration.Configuration
import io.xol.chunkstories.net.http.SimplePostRequest
import io.xol.chunkstories.net.http.SimpleWebRequest
import io.xol.chunkstories.server.DedicatedServer
import io.xol.chunkstories.util.VersionInfo

import java.net.Inet4Address
import java.util.Random
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Small background thread that is tasked with putting and keeping up to date the server's entry in the global list
 * <br></br>
 * Note: This keeps running so it can be hot-enabled/disabled without too much trouble
 */
class ServerAnnouncerThread(private val server: DedicatedServer) : Thread() {
    private val run = AtomicBoolean(true)

    /**
     * Unique key used to authentificate on the servers list.
     */
    private var lolcode: Int = 0 //TODO moar bits

    init {

        lolcode = server.serverConfig.getIntValue("server.announcer.lolcode")
        if (lolcode.toLong() == 0L) {
            val rnd = Random()
            lolcode = rnd.nextInt(Integer.MAX_VALUE)
            val option = server.serverConfig.get<Configuration.OptionInt>("server.announcer.lolcode")
            option!!.trySetting(lolcode)
        }

        name = "Server list announcer thread"
    }

    fun stopAnnouncer() {
        run.set(false)
    }

    override fun run() {
        try {
            val internalIp = Inet4Address.getLocalHost().hostAddress
            val externalIp = SimpleWebRequest("https://chunkstories.xyz/api/sayMyName.php?ip=1").waitForResult()

            while (run.get()) {
                if (server.serverConfig.getBooleanValue("server.announcer.enable")) {

                    val serverName = server.serverConfig.getValue("server.name")
                    val serverDescription = server.serverConfig.getValue("server.description")

                    val postData = ("srvname=$serverName&desc=$serverDescription&ip=$externalIp&iip=$internalIp&mu=" + server.handler
                            .maxClients + "&u=" + server.handler.playersNumber + "&n=0&w=default&p=1&v=" + VersionInfo.version
                            + "&lolcode=" + lolcode)

                    SimplePostRequest("https://chunkstories.xyz/api/serverAnnounce.php", postData)
                    Thread.sleep(server.serverConfig.getIntValue("server.announcer.dutyCycle").toLong())
                } else
                    Thread.sleep(6000)
            }
        } catch (e: Exception) {
            server.logger().error("An unexpected error happened during multiverse stuff. More info below.")
            e.printStackTrace()
        }

    }
}
