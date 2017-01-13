package io.xol.engine.misc;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;

import org.lwjgl.opengl.Display;

import de.matthiasmann.twl.utils.PNGDecoder;
import de.matthiasmann.twl.utils.PNGDecoder.Format;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class IconLoader
{

	public static void load()
	{
		Display.setIcon(getIconsData());
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
