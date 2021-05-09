//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.net

import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException

import xyz.chunkstories.api.content.json.Json
import xyz.chunkstories.api.content.json.asBoolean
import xyz.chunkstories.api.content.json.asInt
import xyz.chunkstories.api.content.json.asString
import xyz.chunkstories.api.net.Packet
import xyz.chunkstories.api.world.GameInstance
import xyz.chunkstories.api.world.World
import xyz.chunkstories.content.GameContentStore

class PacketDefinition constructor(store: GameContentStore, val name: String, val properties: Json.Dict)  {

    val allowedFrom: AllowedFrom

    enum class AllowedFrom {
        ALL, CLIENT, SERVER
    }

    val isStreamed: Boolean
    val fixedId: Int
    internal val clientClass: Class<out Packet>?
    internal val serverClass: Class<out Packet>?
    internal val commonClass: Class<out Packet>?

    internal val clientClassConstructor: Constructor<out Packet>?
    internal val serverClassConstructor: Constructor<out Packet>?
    internal val commonClassConstructor: Constructor<out Packet>?

    var constructorTakesWorld = false // True if the Packet constructor takes a World parameter
        private set

    init {
        isStreamed = properties["streamed"].asBoolean ?: false
        fixedId = properties["fixedId"].asInt ?: -1

        allowedFrom = when(properties["allowedFrom"].asString ?: "all") {
            "all" -> AllowedFrom.ALL
            "client" -> AllowedFrom.CLIENT
            "server" -> AllowedFrom.SERVER
            else -> throw Exception("allowedFrom can only take one of {all, client, server}.")
        }

        clientClass = properties["clientClass"].asString?.let { resolveClass(store, it) }
        serverClass = properties["serverClass"].asString?.let { resolveClass(store, it) }
        commonClass = properties["commonClass"].asString?.let { resolveClass(store, it) }

        // Security trips in case someone forgets to set up a handler
        if (this.commonClass == null) {
            when {
                allowedFrom == AllowedFrom.ALL && (this.clientClass == null || this.serverClass == null) -> {
                    throw Exception("Packet can be received from both client and servers, but isn't provided with a way to handle both." + "\nEither commonClass must be set, or both clientClass and serverClass")
                }
                allowedFrom == AllowedFrom.SERVER && this.clientClass == null -> {
                    throw Exception("This packet lacks a handler class, please set either commonClass or clientClass")
                }
                allowedFrom == AllowedFrom.CLIENT && this.serverClass == null -> {
                    throw Exception("This packet lacks a handler class, please set either commonClass or serverClass")
                }
            }
        }

        // Grabs the constructors
        clientClassConstructor = extractConstructor(this.clientClass)
        serverClassConstructor = extractConstructor(this.serverClass)
        commonClassConstructor = extractConstructor(this.commonClass)
    }

    private fun resolveClass(store: GameContentStore, className: String): Class<out Packet>? {

        val rawClass = store.modsManager.getClassByName(className)
        if (rawClass == null) {
            return null
            // throw new IllegalPacketDeclarationException("Packet class " + this.getName()
            // + " does not exist in codebase.");
        } else if (!Packet::class.java.isAssignableFrom(rawClass)) {
            return null
            // throw new IllegalPacketDeclarationException("Class " + this.getName() + " is
            // not extending the Packet class.");
        }

        return rawClass as Class<out Packet>?
    }

    private fun extractConstructor(packetClass: Class<out Packet>?): Constructor<out Packet>? {
        // Null leads to null.
        if (packetClass == null)
            return null

        val types = if (constructorTakesWorld) arrayOf<Class<*>>(World::class.java) else arrayOf<Class<*>>(GameInstance::class.java)
        var constructor: Constructor<out Packet>?
        try {
            constructor = packetClass.getConstructor(*types)
        } catch (e: NoSuchMethodException) {
            constructor = null
        } catch (e: SecurityException) {
            constructor = null
        }

        if (constructor == null) {
            throw Exception("Packet " + this.name + " does not provide a valid constructor.")
        }

        return constructor
    }

    fun createNewWithInstance(client: Boolean, gameInstance: GameInstance): Packet? {
        assert(!constructorTakesWorld)
        val parameters = arrayOf<Any>(gameInstance)

        return if (client && clientClass != null)
            clientClassConstructor!!.newInstance(*parameters)
        else if (!client && serverClass != null)
            serverClassConstructor!!.newInstance(*parameters)
        else
            commonClassConstructor!!.newInstance(*parameters)
    }

    fun createNewWithWorld(client: Boolean, world: World?): Packet? {
        assert(constructorTakesWorld)
        val parameters = arrayOf<Any>(world!!)

        return if (client && clientClass != null)
            clientClassConstructor!!.newInstance(*parameters)
        else if (!client && serverClass != null)
            serverClassConstructor!!.newInstance(*parameters)
        else
            commonClassConstructor!!.newInstance(*parameters)
    }

    override fun toString(): String {
        return "PacketDefinition(name=$name, allowedFrom=$allowedFrom, isStreamed=$isStreamed, fixedId=$fixedId, clientClass=$clientClass, serverClass=$serverClass, commonClass=$commonClass, clientClassConstructor=$clientClassConstructor, serverClassConstructor=$serverClassConstructor, commonClassConstructor=$commonClassConstructor, constructorTakesWorld=$constructorTakesWorld)"
    }
}