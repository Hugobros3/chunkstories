//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.input.lwjgl3;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwGetKey;

import io.xol.chunkstories.api.client.Client;
import io.xol.chunkstories.api.client.IngameClient;
import io.xol.chunkstories.api.input.KeyboardKeyInput;
import io.xol.chunkstories.api.util.Configuration;
import io.xol.chunkstories.input.Pollable;

/**
 * Describes a key assignated to some action
 */
public class Lwjgl3KeyBind extends Lwjgl3Input implements KeyboardKeyInput, Pollable {
	private Lwjgl3ClientInputsManager inputsManager;

	private int GLFW_key;
	private int defaultKey;

	private boolean isDown = false;
	boolean editable = true;
	boolean repeat = false;

	private Configuration.OptionInt option;

	Lwjgl3KeyBind(Lwjgl3ClientInputsManager inputsManager, String name, String defaultKeyName) {
		super(inputsManager, name);
		this.inputsManager = inputsManager;
		this.defaultKey = GLFWKeyIndexHelper.getGlfwKeyByName(defaultKeyName);
		this.GLFW_key = defaultKey;

		Client client = inputsManager.gameWindow.getClient();
		Configuration clientConfiguration = client.getConfiguration();

		option = clientConfiguration.new OptionInt("client.input.bind." + name, defaultKey);
		option.addHook(o -> {GLFW_key = o.getValue();});
		clientConfiguration.registerOption(option);
	}

	private int parse(String s) {
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/**
	 * Internal to the engine, should not be interfered with by external mods
	 * 
	 * @return
	 */
	int getLWJGL3xKey() {
		return GLFW_key;
	}

	/** Returns true if the key is pressed and we're either not ingame or there is no GUI overlay blocking gameplay input */
	@Override
	public boolean isPressed() {
		IngameClient ingameClient = inputsManager.gameWindow.getClient().getIngame();
		if (ingameClient != null)
			return isDown && ingameClient.getPlayer().hasFocus();
		return isDown;
	}

	/**
	 * When reloading from the config file (options changed)
	 */
	public void reload() {
		// doesn't do stuff, we have a hook on the option directly
	}

	@Override
	public void updateStatus() {
		isDown = glfwGetKey(im.gameWindow.getGlfwWindowHandle(), GLFW_key) == GLFW_PRESS;
	}

	/**
	 * Is this key bind editable in the controls
	 */
	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}
}
