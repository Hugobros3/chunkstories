//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.net

import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import org.hjson.JsonValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.content.Asset
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.entity.EntityDefinition
import xyz.chunkstories.api.exceptions.net.UnknowPacketException
import xyz.chunkstories.api.net.Packet
import xyz.chunkstories.api.net.PacketDefinition
import xyz.chunkstories.content.GameContentStore
import xyz.chunkstories.content.extractProperties

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.Reader
import java.util.HashMap

class PacketsStore(private val content: GameContentStore) : Content.PacketDefinitions {
    private val byNames = HashMap<String, PacketDefinitionImplementation>()
    private val byClasses = HashMap<Class<out Packet>, PacketDefinitionImplementation>()

    override fun logger(): Logger {
        return logger
    }

    fun reload() {
        byNames.clear()
        byClasses.clear()

        val gson = Gson()

        fun readDefinitions(r: Reader) {
            val json = JsonValue.readHjson(r).toString()
            val map = gson.fromJson(json, LinkedTreeMap::class.java)

            val materialsTreeMap = map["packets"] as LinkedTreeMap<*, *>

            for (definition in materialsTreeMap.entries) {
                val name = definition.key as String
                val properties = (definition.value as LinkedTreeMap<String, *>).extractProperties()

                properties["name"] = name

                val packetDefinition = PacketDefinitionImplementation(content, name, properties)
                byNames[name] = packetDefinition

                packetDefinition.commonClass?.let { byClasses[it] = packetDefinition }
                packetDefinition.serverClass?.let { byClasses[it] = packetDefinition }
                packetDefinition.clientClass?.let { byClasses[it] = packetDefinition }

                logger().debug("Loaded packet definition $packetDefinition")
            }
        }

        // Load system.packets
        readDefinitions(javaClass.getResourceAsStream("/packets/systemPackets.hjson").reader())

        // Load the rest
        for (asset in content.modsManager().allAssets.filter { it.name.startsWith("packets/") && it.name.endsWith(".hjson") }) {
            logger().debug("Reading packets definitions in : $asset")
            readDefinitions(asset.reader())
        }
    }

    override fun getPacketByName(name: String): PacketDefinition? {
        return byNames[name]
    }

    @Throws(UnknowPacketException::class)
    override fun getPacketFromInstance(packet: Packet): PacketDefinition {
        val pclass = packet.javaClass

        val ptd = this.byClasses[pclass]
        if (ptd != null)
            return ptd

        throw UnknowPacketException(packet)
    }

    override fun parent(): Content {
        return this.content
    }

    override fun all(): Iterator<PacketDefinition> {
        val i = byNames.values.iterator()
        return object : Iterator<PacketDefinition> {

            override fun hasNext(): Boolean {
                return i.hasNext()
            }

            override fun next(): PacketDefinition {
                return i.next()
            }

        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger("content.packets")
    }
}
