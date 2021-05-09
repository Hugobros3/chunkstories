//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.net

import com.google.gson.Gson
import org.hjson.JsonValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.content.json.asDict
import xyz.chunkstories.api.net.Packet
import xyz.chunkstories.content.GameContentStore
import xyz.chunkstories.content.eat
import java.io.Reader
import java.util.*

class PacketsStore(val parent: GameContentStore) {
    private val byNames = HashMap<String, PacketDefinition>()
    private val byClasses = HashMap<Class<out Packet>, PacketDefinition>()

    fun reload() {
        byNames.clear()
        byClasses.clear()

        val gson = Gson()

        fun readDefinitions(r: Reader) {
            val json = JsonValue.readHjson(r).eat().asDict ?: throw Exception("This json isn't a dict")
            val dict = json["packets"].asDict ?: throw Exception("This json doesn't contain an 'packets' dict")

            for (element in dict.elements) {
                val name = element.key
                val properties = element.value.asDict ?: throw Exception("Definitions have to be dicts")

                val packetDefinition = PacketDefinition(parent, name, properties)
                byNames[name] = packetDefinition

                packetDefinition.commonClass?.let { byClasses[it] = packetDefinition }
                packetDefinition.serverClass?.let { byClasses[it] = packetDefinition }
                packetDefinition.clientClass?.let { byClasses[it] = packetDefinition }

                logger.debug("Loaded packet definition $packetDefinition")
            }
        }

        // Load system.packets
        readDefinitions(javaClass.getResourceAsStream("/packets/systemPackets.hjson").reader())

        // Load the rest
        for (asset in parent.modsManager.allAssets.filter { it.name.startsWith("packets/") && it.name.endsWith(".hjson") }) {
            logger.debug("Reading packets definitions in : $asset")
            readDefinitions(asset.reader())
        }
    }

    fun getPacketByName(name: String): PacketDefinition? {
        return byNames[name]
    }

    fun getPacketFromInstance(packet: Packet): PacketDefinition? {
        val pclass = packet.javaClass

        val ptd = this.byClasses[pclass]
        if (ptd != null)
            return ptd

        return null
    }

    val all: Collection<PacketDefinition>
        get() {
            return byNames.values
        }

    companion object {
        private val logger = LoggerFactory.getLogger("content.packets")
    }

    val logger: Logger
        get() = Companion.logger
}
