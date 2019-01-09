//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import xyz.chunkstories.api.content.Definition;
import xyz.chunkstories.api.exceptions.content.IllegalPacketDeclarationException;
import xyz.chunkstories.api.net.Packet;
import xyz.chunkstories.api.net.PacketDefinition;
import xyz.chunkstories.api.world.World;
import xyz.chunkstories.content.GameContentStore;

public class PacketDefinitionImplementation extends Definition implements PacketDefinition {

	final AllowedFrom allowedFrom;
	final PacketGenre genre;

	final boolean streamed;
	final int fixedId;
	final Class<? extends Packet> clientClass;
	final Class<? extends Packet> serverClass;
	final Class<? extends Packet> commonClass;

	final Constructor<? extends Packet> clientClassConstructor;
	final Constructor<? extends Packet> serverClassConstructor;
	final Constructor<? extends Packet> commonClassConstructor;

	private boolean constructorTakesWorld = false; // True if the Packet constructor takes a World parameter

	public PacketDefinitionImplementation(GameContentStore store, String name, Map<String, String> properties)
			throws IllegalPacketDeclarationException, IOException {
		super(name, properties);

		streamed = Boolean.parseBoolean(this.resolveProperty("streamed", "false"));
		fixedId = Integer.parseInt(this.resolveProperty("fixedId", "-1"));

		String afs = this.resolveProperty("allowedFrom", "all");
		if (afs.equals("all"))
			allowedFrom = AllowedFrom.ALL;
		else if (afs.equals("client"))
			allowedFrom = AllowedFrom.CLIENT;
		else if (afs.equals("server"))
			allowedFrom = AllowedFrom.SERVER;
		else
			throw new IllegalPacketDeclarationException("allowedFrom can only take one of {all, client, server}.");

		String tys = this.resolveProperty("type", "general");
		if (tys.equals("general"))
			genre = PacketGenre.GENERAL_PURPOSE;
		else if (tys.equals("system"))
			genre = PacketGenre.SYSTEM;
		else if (tys.equals("world")) {
			genre = PacketGenre.WORLD;
			constructorTakesWorld = true;
		} else if (tys.equals("world_streaming")) {
			genre = PacketGenre.WORLD_STREAMING;
			constructorTakesWorld = true;
		} else
			throw new IllegalPacketDeclarationException(
					"type can only take one of {general, systme, world, world_streaming}.");

		// First obtain the classes dedicated to a specific side
		String clientClass = this.resolveProperty("clientClass");
		if (clientClass != null)
			this.clientClass = resolveClass(store, clientClass);
		else
			this.clientClass = null;

		String serverClass = this.resolveProperty("serverClass");
		if (serverClass != null)
			this.serverClass = resolveClass(store, serverClass);
		else
			this.serverClass = null;

		// Then, if necessary we lookup the common class
		String commonClass = this.resolveProperty("commonClass");
		if (commonClass != null)
			this.commonClass = resolveClass(store, commonClass);
		else
			this.commonClass = null;

		// Security trips in case someone forgets to set up a handler
		if (commonClass == null) {
			if (allowedFrom == AllowedFrom.ALL && (this.clientClass == null || this.serverClass == null)) {
				throw new IllegalPacketDeclarationException(
						"Packet can be received from both client and servers, but isn't provided with a way to handle both."
								+ "\nEither commonClass must be set, or both clientClass and serverClass");
			} else if (allowedFrom == AllowedFrom.SERVER && (this.clientClass == null)) {
				throw new IllegalPacketDeclarationException(
						"This packet lacks a handler class, please set either commonClass or clientClass");
			} else if (allowedFrom == AllowedFrom.CLIENT && (this.serverClass == null)) {
				throw new IllegalPacketDeclarationException(
						"This packet lacks a handler class, please set either commonClass or serverClass");
			}
		}

		// Grabs the constructors
		clientClassConstructor = extractConstructor(this.clientClass);
		serverClassConstructor = extractConstructor(this.serverClass);
		commonClassConstructor = extractConstructor(this.commonClass);
	}

	private Class<? extends Packet> resolveClass(GameContentStore store, String className)
			throws IllegalPacketDeclarationException {

		Class<?> rawClass = store.modsManager().getClassByName(className);
		if (rawClass == null) {
			return null;
			// throw new IllegalPacketDeclarationException("Packet class " + this.getName()
			// + " does not exist in codebase.");
		} else if (!(Packet.class.isAssignableFrom(rawClass))) {
			return null;
			// throw new IllegalPacketDeclarationException("Class " + this.getName() + " is
			// not extending the Packet class.");
		}

		@SuppressWarnings("unchecked")
		Class<? extends Packet> packetClass = (Class<? extends Packet>) rawClass;

		return packetClass;
	}

	private Constructor<? extends Packet> extractConstructor(Class<? extends Packet> packetClass)
			throws IllegalPacketDeclarationException {
		// Null leads to null.
		if (packetClass == null)
			return null;

		Class<?>[] types = constructorTakesWorld ? new Class[] { World.class } : new Class[] {};
		Constructor<? extends Packet> constructor;
		try {
			constructor = packetClass.getConstructor(types);
		} catch (NoSuchMethodException | SecurityException e) {
			constructor = null;
		}

		if (constructor == null) {
			throw new IllegalPacketDeclarationException(
					"Packet " + this.getName() + " does not provide a valid constructor.");
		}

		return constructor;
	}

	public int getFixedId() {
		return fixedId;
	}

	public Packet createNew(boolean client, World world) {
		try {
			Object[] parameters = constructorTakesWorld ? new Object[] { world } : new Object[] {};

			if (client && clientClass != null)
				return clientClassConstructor.newInstance(parameters);
			else if (!client && serverClass != null)
				return serverClassConstructor.newInstance(parameters);
			else
				return commonClassConstructor.newInstance(parameters);

		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public AllowedFrom allowedFrom() {
		return allowedFrom;
	}

	@Override
	public PacketGenre getGenre() {
		return genre;
	}

	public boolean isStreamed() {
		return streamed;
	}
}