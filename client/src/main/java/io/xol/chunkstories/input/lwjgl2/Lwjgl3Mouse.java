package io.xol.chunkstories.input.lwjgl2;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import static org.lwjgl.glfw.GLFW.*;

import java.nio.DoubleBuffer;

import org.lwjgl.system.MemoryUtil;

import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.Mouse;
import io.xol.chunkstories.api.math.vector.sp.Vector2fm;
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
	
	public Vector2fm getMousePosition()
	{
		DoubleBuffer b1 = MemoryUtil.memAllocDouble(1);
		DoubleBuffer b2 = MemoryUtil.memAllocDouble(1);
		glfwGetCursorPos(im.gameWindow.glfwWindowHandle, b1, b2);
		Vector2fm vec2 = new Vector2fm(b1.get(), im.gameWindow.getHeight() - b2.get());
		MemoryUtil.memFree(b1);
		MemoryUtil.memFree(b2);

		return vec2;
	}

	@Override
	public float getCursorX() {
		return getMousePosition().getX();
	}

	@Override
	public float getCursorY() {
		return getMousePosition().getY();
	}

	@Override
	public void setMouseCursorLocation(float x, float y) {
		glfwSetCursorPos(im.gameWindow.glfwWindowHandle, x, y);
		//throw new UnsupportedOperationException("Mouse.setMouseCursorLocation()");
	}

	@Override
	public boolean isGrabbed() {
		return glfwGetInputMode(im.gameWindow.glfwWindowHandle, GLFW_CURSOR) == GLFW_CURSOR_DISABLED;
		//throw new UnsupportedOperationException("Mouse.isGrabbed()");
	}

	@Override
	public void setGrabbed(boolean grabbed) {
		glfwSetInputMode(this.im.gameWindow.glfwWindowHandle, GLFW_CURSOR, grabbed ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
		//throw new UnsupportedOperationException("Mouse.setGrabbed()");
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
