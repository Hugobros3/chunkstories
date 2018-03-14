//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.input.lwjgl3;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwGetKey;

import java.io.IOException;

import io.xol.chunkstories.api.input.KeyboardKeyInput;
import io.xol.chunkstories.api.util.Configuration.KeyBindOption;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.materials.GenericNamedConfigurable;
import io.xol.chunkstories.util.config.ConfigurationImplementation;
import io.xol.chunkstories.util.config.OptionIntImplementation;
import io.xol.chunkstories.util.config.OptionUntyped;

/**
 * Describes a key assignated to some action
 */
public class Lwjgl3KeyBind extends Lwjgl3Input implements KeyboardKeyInput, LWJGLPollable
{
	int GLFW_key;
	
	boolean isDown;
	
	boolean editable = true;
	boolean repeat = false;
	
	int defaultKey;
	
	Lwjgl3KeyBindOption option;
	
	public Lwjgl3KeyBind(Lwjgl3ClientInputsManager im, String name, String defaultKeyName)
	{
		super(im, name);
		this.defaultKey = GLFWKeyIndexHelper.getGlfwKeyByName(defaultKeyName);
		this.GLFW_key = defaultKey;
		
		option = new Lwjgl3KeyBindOption("client.input.bind." + name);
		Client.getInstance().getConfiguration().addOption(option);
	}
	
	public Lwjgl3KeyBindOption getOption() {
		return option;
	}
	
	public class Lwjgl3KeyBindOption extends GenericNamedConfigurable implements KeyBindOption {

		public Lwjgl3KeyBindOption(String name) {
			super(name);
		}

		@Override
		public int getIntValue() {
			return GLFW_key;
		}

		@Override
		public String getValue() {
			return GLFW_key + "";
		}

		@Override
		public String getDefaultValue() {
			return defaultKey + "";
		}

		@Override
		public void trySetting(String value) {
			GLFW_key = parse(value);
		}

		@Override
		public Lwjgl3KeyBind getInput() {
			return Lwjgl3KeyBind.this;
		}
	}
	
	private int parse(String s) {
		try {
			return Integer.parseInt(s);
		} catch(NumberFormatException e) {
			return 0;
		}
	}
	
	/**
	 * Internal to the engine, should not be interfered with by external mods
	 * @return
	 */
	public int getLWJGL3xKey()
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
		//this.GLFW_key = Client.getInstance().getConfig().getInteger("bind."+name, -1);
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
