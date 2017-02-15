package io.xol.engine.graphics.textures;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL14.*;

import java.nio.ByteBuffer;

import io.xol.engine.graphics.fbo.RenderTarget;

import static org.lwjgl.opengl.GL30.*;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class Texture2DRenderTarget extends Texture2D
{
	public Texture2DRenderTarget(TextureFormat type, int w, int h)
	{
		super(type);
		
		resize(w, h);
	}
}
