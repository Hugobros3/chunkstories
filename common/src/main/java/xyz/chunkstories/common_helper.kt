package xyz.chunkstories

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.net.Packet
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.workers.Tasks
import xyz.chunkstories.content.mods.ModsManagerImplementation
import xyz.chunkstories.util.LogbackSetupHelper
import java.text.SimpleDateFormat
import java.util.*

// TODO move
interface RemotePlayer : Player {
    fun pushPacket(packet: Packet)
}

// TODO move
interface Engine {
    val modsManager: ModsManagerImplementation
    val tasks: Tasks
}

fun setupLogFile(logDirectory: String): Logger {
    val cal = Calendar.getInstance()
    val sdf = SimpleDateFormat("YYYY.MM.dd HH.mm.ss")
    val time = sdf.format(cal.time)

    val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)

    val loggingFilename = "$logDirectory/$time.log"
    LogbackSetupHelper(loggingFilename)

    return logger
}