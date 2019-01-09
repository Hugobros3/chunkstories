//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.scrap;

import org.lwjgl.glfw.GLFWVidMode;

public class DisplayMode {

	final int monitorId;
	final GLFWVidMode videoMode;

	public DisplayMode(int monitorId, GLFWVidMode videoMode) {
		super();
		this.monitorId = monitorId;
		this.videoMode = videoMode;
	}

	public String toString() {
		return monitorId + ":" + videoMode.width() + ":" + videoMode.height() + ":" + videoMode.refreshRate();
	}
}
