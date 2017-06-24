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
