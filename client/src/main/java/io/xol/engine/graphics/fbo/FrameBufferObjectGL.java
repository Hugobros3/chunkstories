//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.engine.graphics.fbo;

import static org.lwjgl.opengl.GL11.GL_NONE;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL20.glDrawBuffers;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glDeleteFramebuffers;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;

import java.lang.ref.WeakReference;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.lwjgl.BufferUtils;

import io.xol.chunkstories.api.rendering.target.RenderTarget;
import io.xol.chunkstories.api.rendering.target.RenderTargetsConfiguration;
import io.xol.chunkstories.client.Client;

public class FrameBufferObjectGL implements RenderTargetsConfiguration
{
	RenderTarget[] colorAttachements;
	RenderTarget depthAttachement;

	int glId;

	public FrameBufferObjectGL(RenderTarget depth, RenderTarget... colors)
	{
		glId = glGenFramebuffers();
		glBindFramebuffer(GL_FRAMEBUFFER, glId);
		allocatedIds.put(glId, new WeakReference<>(this));

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
			depthAttachement.attachAsDepth();
			//glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthAttachement.getID(), 0);

		glBindFramebuffer(GL_FRAMEBUFFER, 0);
	}

	IntBuffer scratchBuffer;
	List<Integer> drawBuffers = new ArrayList<Integer>();
	
	@Override
	public void setEnabledRenderTargets(boolean... targets)
	{
		bind();
		// ???
		if (depthAttachement != null)
			depthAttachement.attachAsDepth();
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

	@Override
	public void setDepthAttachement(RenderTarget depthAttachement)
	{
		this.depthAttachement = depthAttachement;
		if(depthAttachement != null)
			depthAttachement.attachAsDepth();
	}

	@Override
	public void setColorAttachement(int index, RenderTarget colorAttachement)
	{
		this.colorAttachements[index] = colorAttachement;
		if(colorAttachement != null)
			colorAttachement.attachAsColor(index);
	}

	@Override
	public void setColorAttachements(RenderTarget... colorAttachements)
	{
		scratchBuffer = BufferUtils.createIntBuffer(colorAttachements.length);
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

	@Override
	public void resize(int w, int h)
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
		if(glId == bound)
			return;
		glBindFramebuffer(GL_FRAMEBUFFER, glId);
		RenderTarget ok = this.depthAttachement != null ? depthAttachement : (this.colorAttachements != null && this.colorAttachements.length > 0 ? this.colorAttachements[0] : null);
		if(ok != null)
			glViewport(0, 0, ok.getWidth(), ok.getHeight());
		else
			System.out.println("fck off");
		bound = glId;
	}

	static void unbind()
	{
		glBindFramebuffer(GL_FRAMEBUFFER, 0);
		glViewport(0, 0, Client.getInstance().getGameWindow().getWidth(), Client.getInstance().getGameWindow().getHeight());
		bound = 0;
	}
	
	static int bound = 0;

	@Override
	public void destroy()
	{
		allocatedIds.remove(glId);
		glDeleteFramebuffers(glId);
		/*if (texturesToo)
		{
			if (depthAttachement != null)
				depthAttachement.destroy();
			for (RenderTarget tex : colorAttachements)
			{
				if (tex != null)
					tex.destroy();
			}
		}*/
	}

	public static void updateFrameBufferObjects() {
		
		Iterator<Entry<Integer, WeakReference<FrameBufferObjectGL>>> i = allocatedIds.entrySet().iterator();
		while (i.hasNext())
		{
			Entry<Integer, WeakReference<FrameBufferObjectGL>> entry = i.next();
			int glId = entry.getKey();
			WeakReference<FrameBufferObjectGL> weakReference = entry.getValue();
			FrameBufferObjectGL fbo = weakReference.get();
	
			if (fbo == null)
			{
				//Gives back orphan objects
				glDeleteFramebuffers(glId);
				i.remove();
			}
		}
	}
	
	protected static Map<Integer, WeakReference<FrameBufferObjectGL>> allocatedIds = new ConcurrentHashMap<>();
}
