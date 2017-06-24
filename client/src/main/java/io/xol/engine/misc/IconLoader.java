package io.xol.engine.misc;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;

import org.lwjgl.glfw.GLFWImage;
import static org.lwjgl.glfw.GLFW.*;

import de.matthiasmann.twl.utils.PNGDecoder;
import de.matthiasmann.twl.utils.PNGDecoder.Format;
import io.xol.engine.base.GameWindowOpenGL_LWJGL3;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class IconLoader
{

	public static void load(GameWindowOpenGL_LWJGL3 gameWindow)
	{
		//glfwSetWindowIcon(gameWindow.glfwWindowHandle, getIconsData());
		
		try (GLFWImage.Buffer icons = GLFWImage.malloc(2)) {
            ByteBuffer pixels16 = getByteBufferData("./res/textures/icon16.png");//stbi_load_from_memory(icon16, w, h, comp, 4);
            icons
                .position(0)
                .width(16)
                .height(16)
                .pixels(pixels16);

            ByteBuffer pixels32 = getByteBufferData("./res/textures/icon.png");//stbi_load_from_memory(icon32, w, h, comp, 4);
            icons
                .position(1)
                .width(32)
                .height(32)
                .pixels(pixels32);

            icons.position(0);
            glfwSetWindowIcon(gameWindow.glfwWindowHandle, icons);

            //stbi_image_free(pixels32);
            //stbi_image_free(pixels16);
		}
		
		//Display.setIcon(getIconsData());
	}

	public static ByteBuffer getByteBufferData(String name)
	{
		File file = new File(name);
		if (file.exists())
		{
			try
			{
				PNGDecoder decoder = new PNGDecoder(new FileInputStream(file));
				int width = decoder.getWidth();
				int height = decoder.getHeight();
				ByteBuffer temp = ByteBuffer.allocateDirect(4 * width * height);
				decoder.decode(temp, width * 4, Format.RGBA);
				temp.flip();
				return temp;
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		return null;
	}

	public static ByteBuffer[] getIconsData()
	{

		ByteBuffer[] returnme;
		if (OSHelper.isWindows())
		{
			returnme = new ByteBuffer[2];
			returnme[0] = getByteBufferData("./res/textures/icon16.png");
			returnme[1] = getByteBufferData("./res/textures/icon.png");
		}
		else
		{
			returnme = new ByteBuffer[1];
			returnme[0] = getByteBufferData("./res/textures/icon.png");
		}
		return returnme;
	}
}
