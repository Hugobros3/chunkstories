//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.input.lwjgl3;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwGetKey;

import io.xol.chunkstories.api.input.KeyboardKeyInput;
import io.xol.chunkstories.client.Client;

/**
 * Describes a key assignated to some action
 */
public class Lwjgl3KeyBind extends Lwjgl3Input implements KeyboardKeyInput, LWJGLPollable
{
	int GLFW_key;
	
	boolean isDown;
	
	boolean editable = true;
	boolean repeat = false;
	
	public Lwjgl3KeyBind(Lwjgl3ClientInputsManager im, String name, String defaultKeyName)
	{
		super(im, name);
		this.GLFW_key = Client.getInstance().getConfig().getInteger("bind."+name, GLFWKeyIndexHelper.getGlfwKeyByName(defaultKeyName));
	}
	
	/**
	 * Internal to the engine, should not be interfered with by external mods
	 * @return
	 */
	public int getLWJGL2xKey()
	{
		return GLFW_key;
	}
	
	@Override
	public boolean isPressed()
	{
		return isDown;
	}

	/**
	 * When reloading from the config file (options changed)
	 */
	public void reload()
	{
		this.GLFW_key = Client.getInstance().getConfig().getInteger("bind."+name, -1);
	}
	
	@Override
	public void updateStatus()
	{
		isDown = glfwGetKey(im.gameWindow.glfwWindowHandle, GLFW_key) == GLFW_PRESS;//Keyboard.isKeyDown(LWJGL2_key);
	}
	
	/**
	 * Is this key bind editable in the controls
	 */
	public boolean isEditable()
	{
		return editable;
	}

	public void setEditable(boolean editable)
	{
		this.editable = editable;
	}
}
