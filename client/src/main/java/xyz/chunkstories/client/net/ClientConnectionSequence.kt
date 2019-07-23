//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.client.net

import java.io.File
import java.util.HashSet
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

import org.slf4j.LoggerFactory

import xyz.chunkstories.api.exceptions.content.mods.ModLoadFailureException
import xyz.chunkstories.client.ClientImplementation
import xyz.chunkstories.client.identity.LocalClientIdentity
import xyz.chunkstories.client.identity.LoggedInClientIdentity
import xyz.chunkstories.client.net.vanillasockets.TCPServerConnection
import xyz.chunkstories.content.mods.ModZip
import xyz.chunkstories.net.Connection
import xyz.chunkstories.util.VersionInfo
import xyz.chunkstories.net.http.SimplePostRequest


/**
 * The job of the ClientConnectionSequence is to execute the required steps to login
 * in a server, while monitoring back progress to the main thread
 */
class ClientConnectionSequence constructor(val client: ClientImplementation, val serverAddress: String, val serverPort: Int) : Thread() {
    private val connection: ServerConnection
    var isDone = false
        private set

    private val authSemaphore = Semaphore(0)

    private var modsString: String? = null
    private val modsSemaphore = Semaphore(0)

    private val translatorSemaphore = Semaphore(0)

    internal val worldSemaphore = Semaphore(0)

    open class ConnectionState(open val text: String)
    class Failure(text: String) : ConnectionState(text)
    class Finished(text: String) : ConnectionState(text)

    var state: ConnectionState

    init {
        this.connection = object : TCPServerConnection(this@ClientConnectionSequence) {

            override fun handleSystemRequest(msg: String): Boolean {
                if (msg.startsWith("info/mods:")) {
                    modsString = msg.substring(10, msg.length)
                    modsSemaphore.release()
                    return true
                } else if (msg == "login/ok") {
                    authSemaphore.release()
                    return true
                } /*else if (msg == "world/ok") {
                    worldSemaphore.release()
                    return true
                } */else if (msg == "world/translator_ok") {
                    translatorSemaphore.release()
                    return true
                }
                return super.handleSystemRequest(msg)
            }

            override fun onDisconnect(reason: String) {
                client.ingame?.exitToMainMenu(reason)
            }
        }

        state = ConnectionState("Establishing connection to $serverAddress:$serverPort")
        this.start()
    }

    override fun run() {
        logger.info("Connection sequence initialized.")
        try {
            if (!connection.connect())
                abort("Failed to establish connection")

            when(val identity = client.user) {
                is LocalClientIdentity -> {
                    connection.sendTextMessage("login/start")
                    connection.sendTextMessage("login/username:" + identity.client)
                    connection.sendTextMessage("login/logintoken:nopenopenopenopenope")
                    connection.sendTextMessage("login/version:" + VersionInfo.networkProtocolVersion)
                    connection.sendTextMessage("login/confirm")

                    this.state = ConnectionState("Offline-mode enabled, skipping login token phase")
                }
                is LoggedInClientIdentity -> {
                    step("Requesting a login token...")
                    val spr = SimplePostRequest("https://chunkstories.xyz/api/serverTokenObtainer.php", "username=" + identity.name + "&sessid=" + identity.sessionKey)
                    val reply = spr.result()

                    if (reply != null && reply.startsWith("ok")) {
                        val loginToken = reply.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]

                        connection.sendTextMessage("login/start")
                        connection.sendTextMessage("login/username:" + identity.name)
                        connection.sendTextMessage("login/logintoken:$loginToken")
                        connection.sendTextMessage("login/version:" + VersionInfo.networkProtocolVersion)
                        connection.sendTextMessage("login/confirm")

                        step("Token obtained, logging in...")
                    } else {
                        abort("Failed to obtain a login token from the servers")
                    }
                }
            }

            if (!authSemaphore.tryAcquire(5, TimeUnit.SECONDS))
                abort("Server login timed out")

            // Obtain the mods list, check if we have them enabled
            step("Asking server required mods...")
            connection.sendTextMessage("mods")
            if (!modsSemaphore.tryAcquire(5, TimeUnit.SECONDS))
                abort("Failed to obtain mods list from server")

            val requiredMd5s = HashSet<String>()

            //TODO drop this nonsense and use JSON
            for (requiredMod in modsString!!.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                // if(requiredMod.startsWith("md5:"))
                // requiredMod = requiredMod.substring(4, requiredMod.length());

                if (!requiredMod.contains(":"))
                    continue

                val properties = requiredMod.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (properties.size < 3)
                    continue

                val modInternalName = properties[0]
                val modMd5Hash = properties[1]
                val modSizeInBytes = java.lang.Long.parseLong(properties[2])

                // String md5Required = requiredMod.contains(":") ? requiredMod.split(":")[0] :
                // requiredMod;
                client.logger().info("Server asks for mod $modInternalName ($modSizeInBytes bytes), md5=$modMd5Hash")

                requiredMd5s.add(modMd5Hash)

                val cached = File("servermods/$modMd5Hash.zip")
                if (!cached.exists()) {
                    // Sequentially download all the mods from the server
                    //connection.obtainModFile(modMd5Hash, cached)

                    val serverAnswer = Semaphore(0)
                    var downloadStatus2: Connection.DownloadStatus? = null

                    step("Asking server for mod: $modInternalName ($modMd5Hash)")

                    connection.registerExpectedFileStreaming("md5:$modMd5Hash", cached) { downloadStatus ->
                        downloadStatus2 = downloadStatus
                        serverAnswer.release()

                        // Create a state telling us the progress
                        this.state = object : ConnectionState("") {
                            override val text: String
                                get() = "${downloadStatus.bytesDownloaded()} bytes out of ${downloadStatus.totalBytes()}"
                        }
                    }
                    connection.sendTextMessage("send-mod/md5:$modMd5Hash")

                    if(!serverAnswer.tryAcquire(5, TimeUnit.SECONDS))
                        abort("Couldn't obtain mod $modInternalName ($modMd5Hash): server didn't reply in time")

                    downloadStatus2!!.waitsUntilDone()
                }

                // Check their size and signature
                if (cached.length() != modSizeInBytes) {
                    client.logger()
                            .info("Invalid filesize for downloaded mod " + modInternalName + " (hash: " + modMd5Hash
                                    + ")" + " expected filesize = " + modSizeInBytes + " != actual filesize = "
                                    + cached.length())
                    cached.delete() // Delete suspicious file
                    abort("Failed to download $modInternalName, wrong file size. You can try again.")
                }

                // Test if the mod loads
                var testHash: ModZip? = null
                try {
                    testHash = ModZip(cached)
                } catch (e: ModLoadFailureException) {
                    e.printStackTrace()

                    client.logger().info("Could not load downloaded mod " + modInternalName + " (hash: "
                            + modMd5Hash + "), see stack trace")
                    cached.delete() // Delete suspicious file
                    abort("Failed to load $modInternalName, check error log.")
                }

                // Test the md5 hash wasn't tampered with
                val actualMd5Hash = testHash!!.mD5Hash
                if (actualMd5Hash != modMd5Hash) {
                    client.logger().info("Invalid md5 hash for mod " + modInternalName
                            + " expected md5 hash = " + modMd5Hash + " != actual md5 hash = " + actualMd5Hash)
                    cached.delete() // Delete suspicious file
                    abort("Mod $modInternalName hash did not match.")
                }
            }

            // Build the string to pass to the modsManager as to ask it to enable said mods
            val requiredMods = requiredMd5s.map { "md5:$it" }
            client.content.modsManager.setEnabledMods(*requiredMods.toTypedArray())

            step("Reloading mods...")
            client.reloadAssets()

            step("Loading ContentTranslator...")
            connection.sendTextMessage("world/translator")
            if (!translatorSemaphore.tryAcquire(5, TimeUnit.SECONDS))
                abort("Timed out waiting for content translator")

            step("Asking for world info...")
            // Ask the server world info and if allowed where to spawn and preload chunks
            connection.sendTextMessage("world/enter")
            if (!worldSemaphore.tryAcquire(15, TimeUnit.SECONDS))
                abort("Timed out waiting for world")

            step("Loading world...")

            // Ask the server to eventually spawn the player entity
            // TODO
            synchronized(this) {
                try {
                    sleep(5000)
                } catch (e: InterruptedException) {
                    // TODO Auto-generated catch block
                    e.printStackTrace()
                }

            }

            // We are good.
            isDone = true
        } catch (e: AbortException) {
            logger.info("Connection sequence aborted.")
        } catch (e1: InterruptedException) {
            e1.printStackTrace()
        }

    }

    private fun step(text: String) {
        this.state = ConnectionState(text)
    }

    @Throws(AbortException::class)
    private fun abort(text: String) {
        this.connection.close("Aborted connection")
        this.state = Failure(text)
        //this.status = ConnectionStep(string)
        //this.aborted = string
        throw AbortException()
    }

    fun abort() {
        stop()
        connection.close("Aborted connection")
    }

    private inner class AbortException : Exception()

    companion object {
        private val logger = LoggerFactory.getLogger("net")
    }
}
