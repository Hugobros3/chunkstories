//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.net.announcer

import xyz.chunkstories.api.util.configuration.Configuration
import xyz.chunkstories.net.http.SimplePostRequest
import xyz.chunkstories.net.http.SimpleWebRequest
import xyz.chunkstories.server.DedicatedServer
import xyz.chunkstories.server.DedicatedServerOptions
import xyz.chunkstories.util.VersionInfo

import java.net.Inet4Address
import java.util.Random
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Small background thread that is tasked with putting and keeping up to date the server's entry in the global list
 * TODO: this is SERIOUSLY OLD junk (circa 2015!)
 */
class ServerAnnouncerThread(private val server: DedicatedServer) : Thread() {
    private val run = AtomicBoolean(true)

    private var lolcode: Int = 0 // TODO this is nowhere enough rng to be secure

    init {

        lolcode = server.config.getIntValue(DedicatedServerOptions.announcerLolcode)
        if (lolcode.toLong() == 0L) {
            val rnd = Random()
            lolcode = rnd.nextInt(Integer.MAX_VALUE)
            val option = server.config.get<Configuration.OptionInt>(DedicatedServerOptions.announcerLolcode)
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
                if (server.config.getBooleanValue(DedicatedServerOptions.announcerEnable)) {

                    val serverName = server.config.getValue(DedicatedServerOptions.serverName)
                    val serverDescription = server.config.getValue(DedicatedServerOptions.serverDescription)

                    val postData = ("srvname=$serverName&desc=$serverDescription&ip=$externalIp&iip=$internalIp&mu=" + server.connectionsManager
                            .maxClients + "&u=" + server.connectionsManager.playersNumber + "&n=0&w=default&p=1&v=" + VersionInfo.versionJson.version
                            + "&lolcode=" + lolcode)

                    SimplePostRequest("https://chunkstories.xyz/api/serverAnnounce.php", postData)

                    sleep(server.config.getIntValue(DedicatedServerOptions.announcerDutyCycle).toLong())
                } else
                    sleep(6000)
            }
        } catch (e: Exception) {
            server.logger.error("An unexpected error happened during multiverse stuff. More info below.")
            e.printStackTrace()
        }

    }
}
