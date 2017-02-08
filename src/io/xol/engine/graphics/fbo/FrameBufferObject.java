package io.xol.engine.graphics.fbo;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL20.*;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.BufferUtils;

import io.xol.chunkstories.client.Client;
import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.graphics.textures.Texture2DRenderTarget;

public class FrameBufferObject
{
	RenderTarget[] colorAttachements;
	RenderTarget depthAttachement;

	int fbo_id;

	public FrameBufferObject(Texture2DRenderTarget depth, RenderTarget... colors)
	{
		fbo_id = glGenFramebuffers();
		glBindFramebuffer(GL_FRAMEBUFFER, fbo_id);

		depthAttachement = depth;
		colorAttachements = colors;

		// Initialize color output buffers
		if (colors != null && colors.length > 0)
		{
			scratchBuffer = BufferUtils.createIntBuffer(colors.length);
			int i = 0;
			for (RenderTarget texture : colors)
			{
				texture.attachAsColor(i);
				//glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + i, GL_TEXTURE_2D, texture.getID(), 0);
				scratchBuffer.put(i, GL_COLOR_ATTACHMENT0 + i);
				i++;
			}
			glDrawBuffers(scratchBuffer);
		}
		else
		{
			glDrawBuffers(GL_NONE);
		}
		// Initialize depth output buffer
		if (depthAttachement != null)
			depthAttachement.attacAshDepth();
			//glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthAttachement.getID(), 0);

		glBindFramebuffer(GL_FRAMEBUFFER, 0);
	}

	IntBuffer scratchBuffer;
	List<Integer> drawBuffers = new ArrayList<Integer>();
	
	public void setEnabledRenderTargets(boolean... targets)
	{
		bind();
		// ???
		if (depthAttachement != null)
			depthAttachement.attacAshDepth();
		if (targets.length == 0)
		{
			// If no arguments set ALL to renderable
			scratchBuffer.clear();
			int i = 0;
			for (RenderTarget texture : colorAttachements)
			{
				texture.attachAsColor(i);
				//glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + i, GL_TEXTURE_2D, texture.getID(), 0);
				scratchBuffer.put(i, GL_COLOR_ATTACHMENT0 + i);
				i++;
			}
			glDrawBuffers(scratchBuffer);
		}
		else
		{
			drawBuffers.clear();
			int i = 0;
			for (boolean b : targets)
			{
				if (b)
					drawBuffers.add(GL_COLOR_ATTACHMENT0 + i);
				i++;
			}
			if (drawBuffers.size() > 0)
			{
				IntBuffer scratchBuffer = BufferUtils.createIntBuffer(drawBuffers.size());
				i = 0;
				for (int b : drawBuffers)
				{
					scratchBuffer.put(i, b);
					i++;
				}
				glDrawBuffers(scratchBuffer);
			}
			else
				glDrawBuffers(GL_NONE);
		}
	}
	
	public void setDepthAttachement(RenderTarget depthAttachement)
	{
		this.depthAttachement = depthAttachement;
		if(depthAttachement != null)
			depthAttachement.attacAshDepth();
	}
	
	public void setColorAttachement(int index, RenderTarget colorAttachement)
	{
		this.colorAttachements[index] = colorAttachement;
		if(colorAttachement != null)
			colorAttachement.attachAsColor(index);
	}
	
	public void setColorAttachements(RenderTarget[] colorAttachements)
	{
		this.colorAttachements = colorAttachements;
		
		int i = 0;
		for (RenderTarget colorAttachement : colorAttachements)
		{
			colorAttachement.attachAsColor(i);
			//glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + i, GL_TEXTURE_2D, texture.getID(), 0);
			scratchBuffer.put(i, GL_COLOR_ATTACHMENT0 + i);
			i++;
		}
	}

	public void resizeFBO(int w, int h)
	{
		if (depthAttachement != null)
		{
			depthAttachement.resize(w, h);
		}
		if (colorAttachements != null)
		{
			for (RenderTarget t : colorAttachements)
			{
				t.resize(w, h);
			}
		}
	}

	void bind()
	{
		//Don't rebind twice
		if(fbo_id == bound)
			return;
		GameWindowOpenGL.getInstance().renderingContext.flush();
		glBindFramebuffer(GL_FRAMEBUFFER, fbo_id);
		RenderTarget ok = this.depthAttachement != null ? depthAttachement : this.colorAttachements[0];
		glViewport(0, 0, ok.getWidth(), ok.getHeight());
		bound = fbo_id;
	}

	static void unbind()
	{
		GameWindowOpenGL.getInstance().renderingContext.flush();
		glBindFramebuffer(GL_FRAMEBUFFER, 0);
		glViewport(0, 0, Client.getInstance().getWindows().getWidth(), Client.getInstance().getWindows().getHeight());
		bound = 0;
	}
	
	static int bound = 0;

	public void destroy(boolean texturesToo)
	{
		glDeleteFramebuffers(fbo_id);
		if (texturesToo)
		{
			if (depthAttachement != null)
				depthAttachement.destroy();
			for (RenderTarget tex : colorAttachements)
			{
				if (tex != null)
					tex.destroy();
			}
		}
	}
}
