//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.input.lwjgl3;

import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL;
import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwGetInputMode;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwSetInputMode;

import java.nio.DoubleBuffer;

import io.xol.chunkstories.client.ClientImplementation;
import org.joml.Vector2d;
import org.lwjgl.system.MemoryUtil;

import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.Mouse;

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

	public Vector2d getMousePosition() {
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
		glfwSetInputMode(this.im.gameWindow.glfwWindowHandle, GLFW_CURSOR,
				grabbed ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
	}

	public MouseScroll scroll(double yoffset) {
		return new MouseScroll() {

			@Override
			public ClientInterface getClient() {
				return ClientImplementation.getInstance();
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
				return (int) yoffset;
			}

			@Override
			public boolean equals(Object o) {
				if (o == null)
					return false;
				else if (o instanceof Input) {
					return ((Input) o).getName().equals(getName());
				} else if (o instanceof String) {
					return ((String) o).equals(this.getName());
				}
				return false;
			}

			@Override
			public int hashCode() {
				return getName().hashCode();
			}

		};
	}

}
