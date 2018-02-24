//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.engine.base;

import org.lwjgl.glfw.GLFWVidMode;

public class VideoMode {
	
	final int monitorId;
	final GLFWVidMode videoMode;
	
	public VideoMode(int monitorId, GLFWVidMode videoMode) {
		super();
		this.monitorId = monitorId;
		this.videoMode = videoMode;
	}

	public String toString() {
		return monitorId+":"+videoMode.width()+":"+videoMode.height()+":"+videoMode.refreshRate();
	}
}
