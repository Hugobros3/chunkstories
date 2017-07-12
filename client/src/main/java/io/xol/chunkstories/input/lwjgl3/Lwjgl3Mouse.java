package io.xol.chunkstories.input.lwjgl3;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import static org.lwjgl.glfw.GLFW.*;

import java.nio.DoubleBuffer;

import org.lwjgl.system.MemoryUtil;

import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.Mouse;

import org.joml.Vector2d;
import org.joml.Vector2f;
import io.xol.chunkstories.client.Client;

public class Lwjgl3Mouse implements Mouse {

	Lwjgl3ClientInputsManager im;
	
	public Lwjgl3Mouse(Lwjgl3ClientInputsManager lwjgl2ClientInputsManager) {
		this.im = lwjgl2ClientInputsManager;
	}

	@Override
	public MouseButton getMainButton() {
		return im.LEFT;
	}

	@Override
	public MouseButton getSecondaryButton() {
		return im.RIGHT;
	}

	@Override
	public MouseButton getMiddleButton() {
		return im.MIDDLE;
	}
	
	public Vector2d getMousePosition()
	{
		DoubleBuffer b1 = MemoryUtil.memAllocDouble(1);
		DoubleBuffer b2 = MemoryUtil.memAllocDouble(1);
		glfwGetCursorPos(im.gameWindow.glfwWindowHandle, b1, b2);
		Vector2d vec2 = new Vector2d(b1.get(), im.gameWindow.getHeight() - b2.get());
		MemoryUtil.memFree(b1);
		MemoryUtil.memFree(b2);

		return vec2;
	}

	@Override
	public double getCursorX() {
		return getMousePosition().x();
	}

	@Override
	public double getCursorY() {
		return getMousePosition().y();
	}

	@Override
	public void setMouseCursorLocation(double x, double y) {
		glfwSetCursorPos(im.gameWindow.glfwWindowHandle, x, y);
	}

	@Override
	public boolean isGrabbed() {
		return glfwGetInputMode(im.gameWindow.glfwWindowHandle, GLFW_CURSOR) == GLFW_CURSOR_DISABLED;
	}

	@Override
	public void setGrabbed(boolean grabbed) {
		glfwSetInputMode(this.im.gameWindow.glfwWindowHandle, GLFW_CURSOR, grabbed ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
	}

	public MouseScroll scroll(double yoffset) {
		return new MouseScroll() {

			@Override
			public ClientInterface getClient() {
				return Client.getInstance();
			}

			@Override
			public String getName() {
				return "mouse.scroll";
			}

			@Override
			public boolean isPressed() {
				return false;
			}

			@Override
			public long getHash() {
				throw new UnsupportedOperationException("MouseScroll.getHash()");
			}

			@Override
			public int amount() {
				return (int)yoffset;
			}
			

			@Override
			public boolean equals(Object o)
			{
				if(o == null)
					return false;
				else if(o instanceof Input) {
					return ((Input)o).getName().equals(getName());
				}
				else if(o instanceof String) {
					return ((String)o).equals(this.getName());
				}
				return false;
			}
			
			@Override
			public int hashCode()
			{
				return getName().hashCode();
			}
			
		};
	}

}
