package io.xol.chunkstories.renderer;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL20.*;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.BufferUtils;

import io.xol.engine.graphics.textures.FBOAttachement;
import io.xol.engine.graphics.textures.GBufferTexture;

public class FBO
{
	FBOAttachement[] colorAttachements;
	FBOAttachement depthAttachement;

	int fbo_id;

	public FBO(GBufferTexture depth, FBOAttachement... colors)
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
			for (FBOAttachement texture : colors)
			{
				texture.attachColor(i);
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
			depthAttachement.attachDepth();
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
			depthAttachement.attachDepth();
		if (targets.length == 0)
		{
			// If no arguments set ALL to renderable
			scratchBuffer.clear();
			int i = 0;
			for (FBOAttachement texture : colorAttachements)
			{
				texture.attachColor(i);
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
	
	public void setDepthAttachement(FBOAttachement depthAttachement)
	{
		this.depthAttachement = depthAttachement;
		if(depthAttachement != null)
			depthAttachement.attachDepth();
	}
	
	public void setColorAttachement(int index, FBOAttachement colorAttachement)
	{
		this.colorAttachements[index] = colorAttachement;
		if(colorAttachement != null)
			colorAttachement.attachColor(index);
	}
	
	public void setColorAttachements(FBOAttachement[] colorAttachements)
	{
		this.colorAttachements = colorAttachements;
		
		int i = 0;
		for (FBOAttachement colorAttachement : colorAttachements)
		{
			colorAttachement.attachColor(i);
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
			for (FBOAttachement t : colorAttachements)
			{
				t.resize(w, h);
			}
		}
	}

	public void bind()
	{
		if(fbo_id == bound)
			return;
		glBindFramebuffer(GL_FRAMEBUFFER, fbo_id);
		bound = fbo_id;
	}

	public static void unbind()
	{
		glBindFramebuffer(GL_FRAMEBUFFER, 0);
		bound = 0;
	}
	
	static int bound = 0;

	public void destroy(boolean texturesToo)
	{
		glDeleteFramebuffers(fbo_id);
		if (texturesToo)
		{
			if (depthAttachement != null)
				depthAttachement.free();
			for (FBOAttachement tex : colorAttachements)
			{
				if (tex != null)
					tex.free();
			}
		}
	}
}
