//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.client.util;

import static org.lwjgl.glfw.GLFW.glfwSetWindowIcon;

import java.nio.ByteBuffer;

import org.lwjgl.glfw.GLFWImage;

import de.matthiasmann.twl.utils.PNGDecoder;
import de.matthiasmann.twl.utils.PNGDecoder.Format;
import io.xol.chunkstories.renderer.opengl.GLFWGameWindow;

public class IconLoader {
	public IconLoader(GLFWGameWindow gameWindow) {
		try (GLFWImage.Buffer icons = GLFWImage.malloc(2)) {
			ByteBuffer pixels16 = getByteBufferData("/textures/icon16.png");
			icons.position(0).width(16).height(16).pixels(pixels16);

			ByteBuffer pixels32 = getByteBufferData("/textures/icon.png");
			icons.position(1).width(32).height(32).pixels(pixels32);

			icons.position(0);
			glfwSetWindowIcon(gameWindow.glfwWindowHandle, icons);

			// stbi_image_free(pixels32);
			// stbi_image_free(pixels16);
		}

		// Display.setIcon(getIconsData());
	}

	ByteBuffer getByteBufferData(String name) {
		try {
			PNGDecoder decoder = new PNGDecoder(getClass().getResourceAsStream(name));
			int width = decoder.getWidth();
			int height = decoder.getHeight();
			ByteBuffer temp = ByteBuffer.allocateDirect(4 * width * height);
			decoder.decode(temp, width * 4, Format.RGBA);
			temp.flip();
			return temp;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
