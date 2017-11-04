package io.xol.chunkstories.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import io.xol.chunkstories.api.content.Content.PacketTypes.PacketType;
import io.xol.chunkstories.api.exceptions.content.IllegalPacketDeclarationException;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.content.GameContentStore;
import io.xol.chunkstories.materials.GenericNamedConfigurable;

public class PacketTypeDeclared extends GenericNamedConfigurable implements PacketType {

	final int id;
	final AllowedFrom allowedFrom;

	final Class<? extends Packet> clientClass;
	final Class<? extends Packet> serverClass;
	final Class<? extends Packet> commonClass;

	final Constructor<? extends Packet> clientClassConstructor;
	final Constructor<? extends Packet> serverClassConstructor;
	final Constructor<? extends Packet> commonClassConstructor;

	public PacketTypeDeclared(GameContentStore store, String name, int ID, BufferedReader reader)
			throws IllegalPacketDeclarationException, IOException {
		super(name, reader);
		this.id = ID;

		String afs = this.resolveProperty("allowedFrom", "all");
		if (afs.equals("all"))
			allowedFrom = AllowedFrom.ALL;
		else if (afs.equals("client"))
			allowedFrom = AllowedFrom.CLIENT;
		else if (afs.equals("server"))
			allowedFrom = AllowedFrom.SERVER;
		else
			throw new IllegalPacketDeclarationException("allowedFrom can only take one of {all, client, server}.");

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
			//throw new IllegalPacketDeclarationException("Packet class " + this.getName() + " does not exist in codebase.");
		} else if (!(Packet.class.isAssignableFrom(rawClass))) {
			return null;
			//throw new IllegalPacketDeclarationException("Class " + this.getName() + " is not extending the Packet class.");
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

		Class<?>[] types = {};
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

	public Packet createNew(boolean client) {
		try {
			Object[] parameters = {};
			if (client && clientClass != null)
				return clientClassConstructor.newInstance(parameters);
			else if (!client && serverClass != null)
				return serverClassConstructor.newInstance(parameters);
			else
				return commonClassConstructor.newInstance(parameters);
			
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public int getID() {
		return id;
	}

	@Override
	public AllowedFrom allowedFrom() {
		return allowedFrom;
	}
}