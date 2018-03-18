//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.input.lwjgl3;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_REPEAT;
import static org.lwjgl.glfw.GLFW.glfwGetKey;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetCharCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;
import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.client.ClientInputsManager;
import io.xol.chunkstories.api.client.LocalPlayer;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.events.client.ClientInputPressedEvent;
import io.xol.chunkstories.api.events.client.ClientInputReleasedEvent;
import io.xol.chunkstories.api.events.player.PlayerInputPressedEvent;
import io.xol.chunkstories.api.events.player.PlayerInputReleasedEvent;
import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.Mouse;
import io.xol.chunkstories.api.input.Mouse.MouseButton;
import io.xol.chunkstories.api.input.Mouse.MouseScroll;
import io.xol.chunkstories.api.plugin.ClientPluginManager;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.net.ServerConnection;
import io.xol.chunkstories.gui.layer.config.KeyBindSelectionOverlay;
import io.xol.chunkstories.input.InputVirtual;
import io.xol.chunkstories.input.InputsLoaderHelper;
import io.xol.chunkstories.input.InputsManagerLoader;
import io.xol.chunkstories.input.Pollable;
import io.xol.chunkstories.net.packets.PacketInput;
import io.xol.chunkstories.renderer.opengl.GLFWGameWindow;
import io.xol.chunkstories.world.WorldClientRemote;

public class Lwjgl3ClientInputsManager implements ClientInputsManager, InputsManagerLoader
{
	protected final GLFWGameWindow gameWindow;
	
	Collection<Input> inputs = new ArrayList<Input>();
	//Set<Lwjgl3KeyBind> keyboardInputs = new HashSet<Lwjgl3KeyBind>();
	Map<Long, Input> inputsMap = new HashMap<Long, Input>();
	
	public Lwjgl3Mouse mouse;// = new Lwjgl3Mouse(this);
	public Lwjgl3MouseButton LEFT;// = new Lwjgl3MouseButton(MOUSE, "mouse.left", 0);
	public Lwjgl3MouseButton RIGHT;// = new Lwjgl3MouseButton(MOUSE, "mouse.right", 1);
	public Lwjgl3MouseButton MIDDLE;// = new Lwjgl3MouseButton(MOUSE, "mouse.middle", 2);
	
	private final GLFWKeyCallback keyCallback;
	private final GLFWMouseButtonCallback mouseButtonCallback;
	private final GLFWScrollCallback scrollCallback;
	private final GLFWCharCallback characterCallback;
	
	private static final Logger logger = LoggerFactory.getLogger("client.workers");
	
	//private final Ingame scene;
	public Lwjgl3ClientInputsManager(GLFWGameWindow gameWindow)
	{
		this.gameWindow = gameWindow;
		
		/*ConfigDeprecated clientConfig = gameWindow.getClient().configDeprecated();
		if(clientConfig.getString("lwjgl-version", "lwjgl2").equals("lwjgl2")) {
			
			gameWindow.getClient().logger().warn("Game was last ran on LWJGL2.x, input codes are messed up, resetting all of them.");
			
			//Removes ALL inputs declared using the old inputs codes of LWJGL2
			Iterator<String> i = clientConfig.getFieldsSet();
			
			while(i.hasNext()) {
				String field = i.next();
				
				if(field.startsWith("bind.")) {
					clientConfig.removeFieldValue(field);
				}
			}
		}
		clientConfig.setString("lwjgl-version", "lwjgl3");
		clientConfig.save();*/
		
		mouse = new Lwjgl3Mouse(this);
		LEFT = new Lwjgl3MouseButton(mouse, "mouse.left", 0);
		RIGHT = new Lwjgl3MouseButton(mouse, "mouse.right", 1);
		MIDDLE = new Lwjgl3MouseButton(mouse, "mouse.middle", 2);
		
		glfwSetKeyCallback(gameWindow.glfwWindowHandle, (keyCallback = new GLFWKeyCallback()
		{
			@Override
			public void invoke(long window, int key, int scancode, int action, int mods)
			{
				if(gameWindow.getLayer() instanceof KeyBindSelectionOverlay)
				{
					KeyBindSelectionOverlay kbs = (KeyBindSelectionOverlay)gameWindow.getLayer();
					kbs.setKeyTo(key);
				}
				
				//Try first the compound shortcuts
				Lwjgl3KeyBindCompound keyBindCompound = getKeyCompoundFulForLWJGL3xKey(key);
				if(keyBindCompound != null) {
					if (action == GLFW_PRESS)
						if(onInputPressed(keyBindCompound))
							return;
				}
				
				//If unsuccessfull pass to normal keyboard input
				Lwjgl3KeyBind keyboardInput = getKeyBoundForLWJGL3xKey(key);
				
				if(keyboardInput != null) {
					if (action == GLFW_PRESS)
						onInputPressed(keyboardInput);
					else if(action == GLFW_REPEAT && keyboardInput.repeat)
						onInputPressed(keyboardInput);
					else if (action == GLFW_RELEASE)
						onInputReleased(keyboardInput);
				}
				
				//Unhandled character
			}
		}));

		glfwSetMouseButtonCallback(gameWindow.glfwWindowHandle, (mouseButtonCallback = new GLFWMouseButtonCallback()
		{
			@Override
			public void invoke(long window, int button, int action, int mods)
			{
				MouseButton mButton = null;
				switch (button)
				{
				case 0:
					mButton = LEFT;
					break;
				case 1:
					mButton = RIGHT;
					break;
				case 2:
					mButton = MIDDLE;
					break;
				}
				
				if (mButton != null)
				{
					if (action == GLFW_PRESS)
						onInputPressed(mButton);
					else if (action == GLFW_RELEASE)
						onInputReleased(mButton);
				}
			}

		}));
		
		glfwSetScrollCallback(gameWindow.glfwWindowHandle, scrollCallback = new GLFWScrollCallback() {
		    @Override
		    public void invoke(long window, double xoffset, double yoffset) {
		    	
		    	MouseScroll ms = mouse.scroll(yoffset);
		    	onInputPressed(ms);
		    	
		    	//gameWindow.getCurrentScene().onScroll((int)yoffset);
		    }
		});
		
		glfwSetCharCallback(gameWindow.glfwWindowHandle, characterCallback = new GLFWCharCallback() {

			@Override
			public void invoke(long window, int codepoint) {
				char[] chars = Character.toChars(codepoint);
				for(char c : chars)
				{
					//Try the GUI handling
					Layer layer = gameWindow.getLayer();
					if(layer.handleTextInput(c))
						return;
					//else
					//	System.out.println("Unhandled character:" + c);
				}
			}
			
		});
		
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
	protected Lwjgl3KeyBind getKeyBoundForLWJGL3xKey(int keyCode)
	{
		for (Input keyBind : inputs)
		{
			if (keyBind instanceof Lwjgl3KeyBind && ((Lwjgl3KeyBind) keyBind).getLWJGL3xKey() == keyCode)
				return (Lwjgl3KeyBind) keyBind;
		}
		return null;
	}
	
	protected Lwjgl3KeyBindCompound getKeyCompoundFulForLWJGL3xKey(int key) {
		inputs:
		for (Input keyBind : inputs)
		{
			if (keyBind instanceof Lwjgl3KeyBindCompound) {
				Lwjgl3KeyBindCompound keyCombinaison = (Lwjgl3KeyBindCompound)keyBind;
				
				//Check all other keys were pressed
				for(int glfwKey : keyCombinaison.glfwKeys) {
					if(glfwGetKey(gameWindow.glfwWindowHandle, glfwKey) != GLFW_PRESS)
					continue inputs;
				}
				
				return keyCombinaison;
			}
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

	public void reload()
	{
		inputs.clear();
		inputsMap.clear();
		
		InputsLoaderHelper.loadKeyBindsIntoManager(this, Client.getInstance().getContent().modsManager());

		//Add physical mouse buttons
		inputs.add(LEFT);
		inputsMap.put(LEFT.getHash(), LEFT);
		inputs.add(RIGHT);
		inputsMap.put(RIGHT.getHash(), RIGHT);
		inputs.add(MIDDLE);
		inputsMap.put(MIDDLE.getHash(), MIDDLE);
	}

	public void insertInput(String type, String name, String value, Collection<String> arguments) {
		Input input;
		if (type.equals("keyBind")) {
			Lwjgl3KeyBind key = new Lwjgl3KeyBind(this, name, value);
			input = key;
			if(arguments.contains("hidden"))
				key.editable = false;
			if(arguments.contains("repeat"))
				key.repeat = true;
			//keyboardInputs.add(key);
		}
		else if(type.equals("virtual"))	{
			input = new InputVirtual(name);
		}
		else if(type.equals("keyBindCompound")) {
			Lwjgl3KeyBindCompound keyCompound = new Lwjgl3KeyBindCompound(this, name, value);
			input = keyCompound;
		}
		else
			return;
		
		inputs.add(input);
		inputsMap.put(input.getHash(), input);
	}
	
	public void pollLWJGLInputs()
	{
		glfwPollEvents();
		
		for(Input input : this.inputs)
		{
			if(input instanceof Pollable)
				((Pollable) input).updateStatus();
		}
	}

	public boolean onInputPressed(Input input)
	{
		if(input.equals("fullscreen")) {
			gameWindow.toggleFullscreen();
			return true;
		}
		
		//System.out.println("Input pressed "+input.getName());
		
		//Try the client-side event press
		ClientInputPressedEvent event = new ClientInputPressedEvent(gameWindow.getClient(), input);
		
		ClientPluginManager cpm = gameWindow.getClient().getPluginManager();
		if(cpm != null) {
			cpm.fireEvent(event);
			if (event.isCancelled())
				return false;
		}
		
		//Try the GUI handling
		Layer layer = gameWindow.getLayer();
		if(layer.handleInput(input))
			return true;
		
		//System.out.println("wasn't handled");
		
		final LocalPlayer player = Client.getInstance().getPlayer();
		if(player == null)
			return false;
		
		final EntityControllable entityControlled = player.getControlledEntity();

		//There has to be a controlled entity for sending inputs to make sense.
		if(entityControlled == null)
			return false;
		
		//Send input to server
		World world = entityControlled.getWorld();
		if (world instanceof WorldClientRemote) {
			// MouseScroll inputs are strictly client-side
			if (!(input instanceof MouseScroll)) {
				ServerConnection connection = ((WorldClientRemote) entityControlled.getWorld()).getConnection();
				PacketInput packet = new PacketInput(world);
				packet.input = input;
				packet.isPressed = true;
				connection.pushPacket(packet);
			}
			
			return entityControlled.onControllerInput(input, Client.getInstance().getPlayer());
		} else {
			PlayerInputPressedEvent event2 = new PlayerInputPressedEvent(Client.getInstance().getPlayer(), input);
			cpm.fireEvent(event2);
			
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
		ClientInputReleasedEvent event = new ClientInputReleasedEvent(gameWindow.getClient(), input);
		ClientPluginManager cpm = gameWindow.getClient().getPluginManager();
		if(cpm != null) {
			cpm.fireEvent(event);
		}
		
		final LocalPlayer player = Client.getInstance().getPlayer();
		if(player == null)
			return false;
		
		final EntityControllable entityControlled = player.getControlledEntity();

		//There has to be a controlled entity for sending inputs to make sense.
		if(entityControlled == null)
			return false;
		
		//Send input to server
		World world = entityControlled.getWorld();
		if (world instanceof WorldClientRemote) {
			ServerConnection connection = ((WorldClientRemote) entityControlled.getWorld()).getConnection();
			PacketInput packet = new PacketInput(world);
			packet.input = input;
			packet.isPressed = false;
			connection.pushPacket(packet);
			return true;
		} else {
			PlayerInputReleasedEvent event2 = new PlayerInputReleasedEvent(Client.getInstance().getPlayer(), input);
			cpm.fireEvent(event2);
			return true;
		}
	}

	@Override
	public Mouse getMouse() {
		return mouse;
	}
	
	public void destroy() {
		this.keyCallback.free();
		this.mouseButtonCallback.free();
		this.scrollCallback.free();
		this.characterCallback.free();
	}
	
	public Logger logger() {
		return logger;
	}
}
