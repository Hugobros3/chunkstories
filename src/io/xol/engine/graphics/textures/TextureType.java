package io.xol.engine.graphics.textures;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL30.*;

import io.xol.chunkstories.client.FastConfig;

public enum TextureType
{
	//For diffuse buffer
	RGBA_8BPP(GL_RGBA, GL_RGBA, GL_UNSIGNED_BYTE),
	//For shaded buffer
	RGB_HDR(FastConfig.openGL3Capable ? GL_R11F_G11F_B10F : GL_RGBA16F, GL_RGB, GL_FLOAT),
	//Shadow maps
	DEPTH_SHADOWMAP(GL_DEPTH_COMPONENT24, GL_DEPTH_COMPONENT, GL_FLOAT),
	//Main render depth
	DEPTH_RENDERBUFFER(GL_DEPTH_COMPONENT32, GL_DEPTH_COMPONENT, GL_FLOAT),
	//Summary data
	RED_32F(GL_R32F, GL_RED, GL_FLOAT),
	;

	TextureType(int internalFormat, int format, int type)
	{
		this.internalFormat = internalFormat;
		this.format = format;
		this.type = type;
	}

	private final int internalFormat;
	private final int format;
	private final int type;

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
}