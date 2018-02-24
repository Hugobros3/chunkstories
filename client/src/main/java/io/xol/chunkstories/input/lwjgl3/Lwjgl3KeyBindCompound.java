//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.input.lwjgl3;

import io.xol.chunkstories.client.Client;

public class Lwjgl3KeyBindCompound extends Lwjgl3Input {

	final String defaultKeysNames;
	int[] glfwKeys;
	
	public Lwjgl3KeyBindCompound(Lwjgl3ClientInputsManager im, String name, String defaultKeysNames) {
		super(im, name);
		
		this.defaultKeysNames = defaultKeysNames;
		
		reload();
	}

	@Override
	public boolean isPressed() {
		return false;
	}

	@Override
	public void reload() {
		String keyNamesString = Client.getInstance().getConfig().getString("bind.glfw.compound."+name, defaultKeysNames);
		String keyNames[] = keyNamesString.split("\\+");
		
		glfwKeys = new int[keyNames.length];
		for(int i = 0; i < keyNames.length; i++) {
			String keyName = keyNames[i];
			
			int glfwKey = GLFWKeyIndexHelper.getGlfwKeyByName(keyName);
			glfwKeys[i] = glfwKey;
			//System.out.println(keyName+":"+glfwKey);
		}
		
		this.im.logger().debug("Initialized keyBindCompound "+name+" for "+glfwKeys.length+" keys.");
	}

}
