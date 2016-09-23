package io.xol.engine.graphics.textures;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL14.*;

import java.nio.ByteBuffer;

import io.xol.engine.graphics.fbo.RenderTarget;

import static org.lwjgl.opengl.GL30.*;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class GBufferTexture extends Texture2D implements RenderTarget
{
	public GBufferTexture(TextureFormat type, int w, int h)
	{
		super(type);
		
		resize(w, h);
	}

	public void resize(int w, int h)
	{
		bind();

		if (this.width == w && this.height == h && !(type == TextureFormat.RGB_HDR))
			return;

		this.width = w;
		this.height = h;
		
		glTexImage2D(GL_TEXTURE_2D, 0, type.getInternalFormat(), w, h, 0, type.getFormat(), type.getType(), (ByteBuffer) null);
		
		if (type != TextureFormat.DEPTH_SHADOWMAP)
		{
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		}
		else
		{
			// For proper hardware linear filtering of textures :o
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_R_TO_TEXTURE);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
		}
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
	}

	@Override
	public void attacAshDepth()
	{
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, this.getId(), 0);
	}

	@Override
	public void attachAsColor(int colorAttachement)
	{
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + colorAttachement, GL_TEXTURE_2D, this.getId(), 0);
	}
}
