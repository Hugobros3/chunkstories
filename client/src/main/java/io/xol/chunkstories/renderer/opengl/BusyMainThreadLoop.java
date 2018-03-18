//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.renderer.opengl;

import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_FRONT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glDrawBuffer;
import static org.lwjgl.opengl.GL11.glViewport;

import java.util.concurrent.atomic.AtomicBoolean;

import org.lwjgl.opengl.GL;

import io.xol.chunkstories.util.concurrency.SimpleFence;

public class BusyMainThreadLoop extends Thread {
	final SimpleFence fence = new SimpleFence();
	final GameWindowOpenGL_LWJGL3 window;
	final AtomicBoolean controlRequested = new AtomicBoolean();

	BusyMainThreadLoop(GameWindowOpenGL_LWJGL3 window) {
		this.window = window;
		
		this.start();
	}
	
	@Override
	public void run() {
		glfwMakeContextCurrent(window.glfwWindowHandle);
		//GL.createCapabilities();
		GL.setCapabilities(window.capabilities);
		
		while(!controlRequested.get() && !glfwWindowShouldClose(window.glfwWindowHandle)) {

			glDrawBuffer(GL_FRONT);
			glViewport(0, 0, 1024, 768);
			glClearColor(2, 1, 1, 0.5f);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			glfwSwapBuffers(window.glfwWindowHandle);
			glfwPollEvents();
			
			System.out.println("Waity waity");
			
			//30fps
			try {
				Thread.sleep(303L);
			} catch (InterruptedException e) {
			}
		}
		
		glClearColor((float)Math.random(), 0, 0, 1.0f);
		glClear(GL_COLOR_BUFFER_BIT);
		
		fence.signal();
	}
	
	public void takeControl() {
		controlRequested.set(true);
		fence.traverse();
		glfwMakeContextCurrent(window.glfwWindowHandle);
	}
}
