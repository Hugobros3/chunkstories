//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.engine.graphics.fbo;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glClearDepth;
import static org.lwjgl.opengl.GL11.glDepthMask;

import org.joml.Vector4f;
import org.joml.Vector4fc;

import io.xol.chunkstories.api.rendering.target.RenderTarget;
import io.xol.chunkstories.api.rendering.target.RenderTargets;
import io.xol.chunkstories.api.rendering.target.RenderTargetsConfiguration;
import io.xol.engine.graphics.RenderingContext;

public class OpenGLRenderTargetManager implements RenderTargets
{
	final RenderingContext renderingContext;
	RenderTargetsConfiguration fbo = null;

	public OpenGLRenderTargetManager(RenderingContext renderingContext)
	{
		this.renderingContext = renderingContext;
	}

	@Override
	public RenderTargetsConfiguration getCurrentConfiguration()
	{
		return fbo;
	}

	@Override
	public void setConfiguration(RenderTargetsConfiguration fbo)
	{
		if (fbo == null)
			FrameBufferObjectGL.unbind();
		else
			((FrameBufferObjectGL) fbo).bind();

		this.fbo = fbo;
	}

	private boolean depthMask = true;

	@Override
	public void setDepthMask(boolean depthMask)
	{
		if(this.depthMask != depthMask)
			glDepthMask(depthMask);
		this.depthMask = depthMask;
	}

	@Override
	public boolean getDepthMask()
	{
		return depthMask;
	}

	float depthClearDepth = 1.0f;
	Vector4fc colorClearColor = new Vector4f(0);

	private void setClearDepth(float depthClearDepth)
	{
		if (depthClearDepth != depthClearDepth)
			glClearDepth(depthClearDepth);
		this.depthClearDepth = depthClearDepth;
	}

	private void setClearColor(Vector4fc colorClearColor)
	{
		if(colorClearColor == null)
			colorClearColor = new Vector4f(0);
		
		if (!this.colorClearColor.equals(colorClearColor))
			glClearColor(colorClearColor.x(), colorClearColor.y(), colorClearColor.z(), colorClearColor.w());

		this.colorClearColor = colorClearColor;
	}

	@Override
	public void clearBoundRenderTargetAll()
	{	
		//Resets those to default values
		setClearDepth(1);
		setClearColor(new Vector4f(0));
		
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
	}

	@Override
	public void clearBoundRenderTargetZ(float z)
	{
		setClearDepth(z);
		glClear(GL_DEPTH_BUFFER_BIT);
	}

	@Override
	public void clearBoundRenderTargetColor(Vector4fc color)
	{	
		setClearColor(color);
		glClear(GL_COLOR_BUFFER_BIT);
	}

	@Override
	public RenderTargetsConfiguration newConfiguration(RenderTarget depth, RenderTarget... colors) {
		return new FrameBufferObjectGL(depth, colors);
	}

}
