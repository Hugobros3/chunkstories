package io.xol.chunkstories.api.rendering.textures;

import static io.xol.chunkstories.api.rendering.textures.TextureFormat.GL_Abstraction.*;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public enum TextureFormat
{
	//For diffuse buffer
	RGBA_8BPP(GL_RGBA, GL_RGBA, GL_UNSIGNED_BYTE, 4),
	//For shaded buffer
	RGB_HDR(GL_R11F_G11F_B10F, GL_RGB, GL_FLOAT, 4),
	//Shadow maps
	DEPTH_SHADOWMAP(GL_DEPTH_COMPONENT24, GL_DEPTH_COMPONENT, GL_FLOAT, 3),
	//Main render depth
	DEPTH_RENDERBUFFER(GL_DEPTH_COMPONENT32, GL_DEPTH_COMPONENT, GL_FLOAT, 4),
	//Summary data
	RED_32F(GL_R32F, GL_RED, GL_FLOAT, 4),
	//
	RGBA_3x10_2(GL_RGB10_A2, GL_RGBA, GL_UNSIGNED_BYTE, 4),
	//
	RGBA_32F(GL_RGBA32F, GL_RGBA, GL_FLOAT, 16),
	;

	//Removes LWJGL dependency
	interface GL_Abstraction {
		public static final int GL_RGBA = 0x1908;
		public static final int GL_UNSIGNED_BYTE = 0x1401;
		public static final int GL_R11F_G11F_B10F = 0x8c3a;
		public static final int GL_RGB = 0x1907;
		public static final int GL_FLOAT = 0x1406;
		public static final int GL_DEPTH_COMPONENT = 0x1902;
		public static final int GL_DEPTH_COMPONENT24 = 0x81a6;
		public static final int GL_DEPTH_COMPONENT32 = 0x81a7;
		public static final int GL_R32F = 0x822e;
		public static final int GL_RED = 0x1903;
		public static final int GL_RGB10_A2 = 0x8059;
		public static final int GL_RGBA32F = 0x8814;
	}
	
	TextureFormat(int internalFormat, int format, int type, int bytesUsed)
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

	public int getBytesPerTexel()
	{
		return bytesUsed;
	}
}