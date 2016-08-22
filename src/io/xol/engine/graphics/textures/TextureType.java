package io.xol.engine.graphics.textures;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL30.*;

import io.xol.chunkstories.client.RenderingConfig;

public enum TextureType
{
	//For diffuse buffer
	RGBA_8BPP(GL_RGBA, GL_RGBA, GL_UNSIGNED_BYTE, 4),
	//For shaded buffer
	RGB_HDR(RenderingConfig.openGL3Capable ? GL_R11F_G11F_B10F : GL_RGBA16F, GL_RGB, GL_FLOAT, 4),
	//Shadow maps
	DEPTH_SHADOWMAP(GL_DEPTH_COMPONENT24, GL_DEPTH_COMPONENT, GL_FLOAT, 3),
	//Main render depth
	DEPTH_RENDERBUFFER(GL_DEPTH_COMPONENT32, GL_DEPTH_COMPONENT, GL_FLOAT, 4),
	//Summary data
	RED_32F(GL_R32F, GL_RED, GL_FLOAT, 4),
	//
	RGBA_3x10_2(GL_RGB10_A2, GL_RGBA, GL_UNSIGNED_BYTE, 4)
	;
	
	TextureType(int internalFormat, int format, int type, int bytesUsed)
	{
		this.internalFormat = internalFormat;
		this.format = format;
		this.type = type;
		this.bytesUsed = bytesUsed;
	}

	private final int internalFormat;
	private final int format;
	private final int type;
	private final int bytesUsed;

	public int getInternalFormat()
	{
		return internalFormat;
	}
	
	public int getFormat()
	{
		return format;
	}
	
	public int getType()
	{
		return type;
	}

	public int getBytesUsed()
	{
		return bytesUsed;
	}
}