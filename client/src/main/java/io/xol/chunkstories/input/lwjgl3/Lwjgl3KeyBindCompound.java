package io.xol.chunkstories.input.lwjgl3;

import io.xol.chunkstories.client.Client;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

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
		String keyNamesString = Client.getInstance().getConfig().getProp("bind.glfw.compound."+name, defaultKeysNames);
		String keyNames[] = keyNamesString.split("\\+");
		
		glfwKeys = new int[keyNames.length];
		for(int i = 0; i < keyNames.length; i++) {
			String keyName = keyNames[i];
			
			int glfwKey = GLFWKeyIndexHelper.getGlfwKeyByName(keyName);
			glfwKeys[i] = glfwKey;
			System.out.println(keyName+":"+glfwKey);
		}
		
		System.out.println("Initialized keyBindCompound "+name+" for "+glfwKeys.length+" keys.");
	}

}
