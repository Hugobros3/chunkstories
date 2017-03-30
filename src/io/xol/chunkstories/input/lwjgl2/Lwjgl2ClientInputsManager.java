package io.xol.chunkstories.input.lwjgl2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.lwjgl.input.Mouse;

import io.xol.chunkstories.api.client.ClientInputsManager;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.KeyboardKeyInput;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.net.ClientToServerConnection;
import io.xol.chunkstories.core.events.ClientInputPressedEvent;
import io.xol.chunkstories.core.events.ClientInputReleasedEvent;
import io.xol.chunkstories.core.events.PlayerInputPressedEvent;
import io.xol.chunkstories.core.events.PlayerInputReleasedEvent;
import io.xol.chunkstories.gui.Ingame;
import io.xol.chunkstories.input.KeyBindsLoader;
import io.xol.chunkstories.net.packets.PacketInput;
import io.xol.chunkstories.world.WorldClientRemote;
import io.xol.engine.gui.Scene;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class Lwjgl2ClientInputsManager implements ClientInputsManager
{
	Set<Input> inputs = new HashSet<Input>();
	Set<KeyBindImplementation> keyboardInputs = new HashSet<KeyBindImplementation>();
	Map<Long, Input> inputsMap = new HashMap<Long, Input>();
	
	//private final Ingame scene;

	public Lwjgl2ClientInputsManager()
	{
		reload();
	}
	
	public Iterator<Input> getAllInputs()
	{
		return inputs.iterator();
	}

	/**
	 * Returns null or a KeyBind matching the name
	 * 
	 * @param keyCode
	 * @return
	 */
	public Input getInputByName(String bindName)
	{
		if(bindName.equals("mouse.left"))
			return LEFT;
		if(bindName.equals("mouse.right"))
			return RIGHT;
		if(bindName.equals("mouse.middle"))
			return MIDDLE;
		for (Input keyBind : inputs)
		{
			if (keyBind.getName().equals(bindName))
				return keyBind;
		}
		return null;
	}

	/**
	 * Returns null or a KeyBind matching the pressed key
	 * 
	 * @param keyCode
	 * @return
	 */
	public KeyboardKeyInput getKeyBoundForLWJGL2xKey(int keyCode)
	{
		for (Input keyBind : inputs)
		{
			if (keyBind instanceof KeyBindImplementation && ((KeyBindImplementation) keyBind).getLWJGL2xKey() == keyCode)
				return (KeyboardKeyInput) keyBind;
		}
		return null;
	}
	
	public Input getInputFromHash(long hash)
	{
		if(hash == 0)
			return LEFT;
		else if(hash == 1)
			return RIGHT;
		else if(hash == 2)
			return MIDDLE;
		
		return inputsMap.get(hash);
	}
	
	//Debug
	public void reloadBoundKeysFromConfig()
	{
		for (Input keyBind : inputs)
		{
			if (keyBind instanceof KeyBindImplementation)
				((KeyBindImplementation) keyBind).reload();
		}
	}
	
	public static Lwjgl2MouseButton LEFT = new Lwjgl2MouseButton("mouse.left", 0);
	public static Lwjgl2MouseButton RIGHT = new Lwjgl2MouseButton("mouse.right", 1);
	public static Lwjgl2MouseButton MIDDLE = new Lwjgl2MouseButton("mouse.middle", 2);

	public void reload()
	{
		inputs.clear();
		inputsMap.clear();
		keyboardInputs.clear();
		Iterator<Input> i = KeyBindsLoader.loadKeyBindsIntoManager(this, Client.getInstance().getContent().modsManager());
		while(i.hasNext())
		{
			Input input = i.next();
			inputs.add(input);
			inputsMap.put(input.getHash(), input);
			
			if(input instanceof KeyBindImplementation)
			{
				keyboardInputs.add((KeyBindImplementation) input);
			}
		}

		//Add physical mouse buttons
		inputs.add(LEFT);
		inputsMap.put(LEFT.getHash(), LEFT);
		inputs.add(RIGHT);
		inputsMap.put(RIGHT.getHash(), RIGHT);
		inputs.add(MIDDLE);
		inputsMap.put(MIDDLE.getHash(), MIDDLE);
	}

	public void pollLWJGLInputs()
	{
		for(Input input : this.inputs)
		{
			if(input instanceof LWJGLPollable)
				((LWJGLPollable) input).updateStatus();
		}
	}

	public boolean onInputPressed(Input input)
	{
		//Check we have a relevant scene
		Scene currentScene = Client.getInstance().getGameWindow().getCurrentScene();
		if(!(currentScene instanceof Ingame))
			return false;
		
		Ingame scene = (Ingame)currentScene;
		ClientInputPressedEvent event = new ClientInputPressedEvent(input);

		scene.getPluginManager().fireEvent(event);
		if (event.isCancelled())
			return false;
		
		final EntityControllable entityControlled = Client.getInstance().getPlayer().getControlledEntity();

		//There has to be a controlled entity for sending inputs to make sense.
		if(entityControlled == null)
			return false;
		
		//Send input to server
		if (entityControlled.getWorld() instanceof WorldClientRemote)
		{
			ClientToServerConnection connection = ((WorldClientRemote) entityControlled.getWorld()).getConnection();
			PacketInput packet = new PacketInput();
			packet.input = input;
			connection.sendPacket(packet);
			
			return entityControlled.onControllerInput(input, Client.getInstance().getPlayer());
		}
		else
		{
			PlayerInputPressedEvent event2 = new PlayerInputPressedEvent(Client.getInstance().getPlayer(), input);
			scene.getPluginManager().fireEvent(event2);
			
			if(event2.isCancelled())
				return false;
				//	entity.handleInteraction(input, entity.getControllerComponent().getController());
		}

		//Handle interaction locally
		return entityControlled.onControllerInput(input, Client.getInstance().getPlayer());
	}

	@Override
	public boolean onInputReleased(Input input)
	{
		//Check we have a relevant scene
		Scene currentScene = Client.getInstance().getGameWindow().getCurrentScene();
		if(!(currentScene instanceof Ingame))
			return false;
		Ingame scene = (Ingame)currentScene;
		
		ClientInputReleasedEvent event = new ClientInputReleasedEvent(input);

		scene.getPluginManager().fireEvent(event);
		//if (event.isCancelled())
		//	return false;
		
		final EntityControllable entityControlled = Client.getInstance().getPlayer().getControlledEntity();

		//There has to be a controlled entity for sending inputs to make sense.
		if(entityControlled == null)
			return false;
		
		//Send input to server
		if (entityControlled.getWorld() instanceof WorldClientRemote)
		{
			ClientToServerConnection connection = ((WorldClientRemote) entityControlled.getWorld()).getConnection();
			PacketInput packet = new PacketInput();
			packet.input = input;
			connection.sendPacket(packet);
			return true;
		}
		else 
		{
			PlayerInputReleasedEvent event2 = new PlayerInputReleasedEvent(Client.getInstance().getPlayer(), input);
			scene.getPluginManager().fireEvent(event2);
			return true;
		}
		
	}
	
	public int getMouseCursorX()
	{
		return Mouse.getX();
	}
	
	public int getMouseCursorY()
	{
		return Mouse.getY();
	}
	
	public void setMouseCursorLocation(int x, int y)
	{
		Mouse.setCursorPosition(x, y);
	}
}
