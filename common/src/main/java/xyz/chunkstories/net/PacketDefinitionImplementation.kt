//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.net

import java.io.BufferedReader
import java.io.IOException
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException

import xyz.chunkstories.api.content.Definition
import xyz.chunkstories.api.content.json.Json
import xyz.chunkstories.api.content.json.asBoolean
import xyz.chunkstories.api.content.json.asInt
import xyz.chunkstories.api.content.json.asString
import xyz.chunkstories.api.exceptions.content.IllegalPacketDeclarationException
import xyz.chunkstories.api.net.Packet
import xyz.chunkstories.api.net.PacketDefinition
import xyz.chunkstories.api.world.World
import xyz.chunkstories.content.GameContentStore

class PacketDefinitionImplementation @Throws(IllegalPacketDeclarationException::class, IOException::class)
constructor(store: GameContentStore, name: String, properties: Json.Dict) : Definition(name, properties), PacketDefinition {

    override val allowedFrom: PacketDefinition.AllowedFrom
    override val genre: PacketDefinition.PacketGenre

    val isStreamed: Boolean
    val fixedId: Int
    internal val clientClass: Class<out Packet>?
    internal val serverClass: Class<out Packet>?
    internal val commonClass: Class<out Packet>?

    internal val clientClassConstructor: Constructor<out Packet>?
    internal val serverClassConstructor: Constructor<out Packet>?
    internal val commonClassConstructor: Constructor<out Packet>?

    private var constructorTakesWorld = false // True if the Packet constructor takes a World parameter

    init {
        isStreamed = this["streamed"].asBoolean ?: false
        fixedId = this["fixedId"].asInt ?: -1

        allowedFrom = when(this["allowedFrom"].asString ?: "all") {
            "all" -> PacketDefinition.AllowedFrom.ALL
            "client" -> PacketDefinition.AllowedFrom.CLIENT
            "server" -> PacketDefinition.AllowedFrom.SERVER
            else -> throw IllegalPacketDeclarationException("allowedFrom can only take one of {all, client, server}.")
        }

        genre = when(this["type"].asString ?: "general") {
            "general" -> PacketDefinition.PacketGenre.GENERAL_PURPOSE
            "system" -> PacketDefinition.PacketGenre.SYSTEM
            "world" -> {
                constructorTakesWorld = true
                PacketDefinition.PacketGenre.WORLD
            }
            "world_streaming" -> {
                constructorTakesWorld = true
                PacketDefinition.PacketGenre.WORLD_STREAMING
            }
            else -> throw IllegalPacketDeclarationException("type can only take one of {general, system, world, world_streaming}.")
        }

        clientClass = this["clientClass"].asString?.let { resolveClass(store, it) }
        serverClass = this["serverClass"].asString?.let { resolveClass(store, it) }
        commonClass = this["commonClass"].asString?.let { resolveClass(store, it) }

        // First obtain the classes dedicated to a specific side
        /*val clientClass = this.resolveProperty("clientClass")
        if (clientClass != null)
            this.clientClass = resolveClass(store, clientClass)
        else
            this.clientClass = null

        val serverClass = this.resolveProperty("serverClass")
        if (serverClass != null)
            this.serverClass = resolveClass(store, serverClass)
        else
            this.serverClass = null

        // Then, if necessary we lookup the common class
        val commonClass = this.resolveProperty("commonClass")
        if (commonClass != null)
            this.commonClass = resolveClass(store, commonClass)
        else
            this.commonClass = null*/

        // Security trips in case someone forgets to set up a handler
        if (this.commonClass == null) {
            if (allowedFrom == PacketDefinition.AllowedFrom.ALL && (this.clientClass == null || this.serverClass == null)) {
                throw IllegalPacketDeclarationException(
                        "Packet can be received from both client and servers, but isn't provided with a way to handle both." + "\nEither commonClass must be set, or both clientClass and serverClass")
            } else if (allowedFrom == PacketDefinition.AllowedFrom.SERVER && this.clientClass == null) {
                throw IllegalPacketDeclarationException(
                        "This packet lacks a handler class, please set either commonClass or clientClass")
            } else if (allowedFrom == PacketDefinition.AllowedFrom.CLIENT && this.serverClass == null) {
                throw IllegalPacketDeclarationException(
                        "This packet lacks a handler class, please set either commonClass or serverClass")
            }
        }

        // Grabs the constructors
        clientClassConstructor = extractConstructor(this.clientClass)
        serverClassConstructor = extractConstructor(this.serverClass)
        commonClassConstructor = extractConstructor(this.commonClass)
    }

    @Throws(IllegalPacketDeclarationException::class)
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

    @Throws(IllegalPacketDeclarationException::class)
    private fun extractConstructor(packetClass: Class<out Packet>?): Constructor<out Packet>? {
        // Null leads to null.
        if (packetClass == null)
            return null

        val types = if (constructorTakesWorld) arrayOf<Class<*>>(World::class.java) else arrayOf()
        var constructor: Constructor<out Packet>?
        try {
            constructor = packetClass.getConstructor(*types)
        } catch (e: NoSuchMethodException) {
            constructor = null
        } catch (e: SecurityException) {
            constructor = null
        }

        if (constructor == null) {
            throw IllegalPacketDeclarationException(
                    "Packet " + this.name + " does not provide a valid constructor.")
        }

        return constructor
    }

    fun createNew(client: Boolean, world: World?): Packet? {
        try {
            val parameters = if (constructorTakesWorld) arrayOf<Any>(world!!) else arrayOf()

            return if (client && clientClass != null)
                clientClassConstructor!!.newInstance(*parameters)
            else if (!client && serverClass != null)
                serverClassConstructor!!.newInstance(*parameters)
            else
                commonClassConstructor!!.newInstance(*parameters)

        } catch (e: InstantiationException) {
            e.printStackTrace()
            return null
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
            return null
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            return null
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
            return null
        }

    }

    override fun toString(): String {
        return "PacketDefinitionImplementation(name=$name, allowedFrom=$allowedFrom, genre=$genre, isStreamed=$isStreamed, fixedId=$fixedId, clientClass=$clientClass, serverClass=$serverClass, commonClass=$commonClass, clientClassConstructor=$clientClassConstructor, serverClassConstructor=$serverClassConstructor, commonClassConstructor=$commonClassConstructor, constructorTakesWorld=$constructorTakesWorld)"
    }
}