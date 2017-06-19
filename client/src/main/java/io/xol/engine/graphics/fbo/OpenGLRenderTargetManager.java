package io.xol.engine.graphics.fbo;

import static org.lwjgl.opengl.GL11.*;

import io.xol.chunkstories.api.math.vector.sp.Vector4fm;
import io.xol.chunkstories.api.rendering.target.RenderTarget;
import io.xol.chunkstories.api.rendering.target.RenderTargetAttachementsConfiguration;
import io.xol.chunkstories.api.rendering.target.RenderTargetManager;
import io.xol.engine.graphics.RenderingContext;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class OpenGLRenderTargetManager implements RenderTargetManager
{
	final RenderingContext renderingContext;
	RenderTargetAttachementsConfiguration fbo = null;

	public OpenGLRenderTargetManager(RenderingContext renderingContext)
	{
		this.renderingContext = renderingContext;
	}

	@Override
	public RenderTargetAttachementsConfiguration getCurrentConfiguration()
	{
		return fbo;
	}

	@Override
	public void setConfiguration(RenderTargetAttachementsConfiguration fbo)
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
		{
			renderingContext.flush();
			glDepthMask(depthMask);
		}
		this.depthMask = depthMask;
	}

	@Override
	public boolean getDepthMask()
	{
		return depthMask;
	}

	float depthClearDepth = 1.0f;
	Vector4fm colorClearColor = new Vector4fm(0);

	private void setClearDepth(float depthClearDepth)
	{
		if (depthClearDepth != depthClearDepth)
			glClearDepth(depthClearDepth);
		this.depthClearDepth = depthClearDepth;
	}

	private void setClearColor(Vector4fm colorClearColor)
	{
		if(colorClearColor == null)
			colorClearColor = new Vector4fm(0);
		
		if (!this.colorClearColor.equals(colorClearColor))
			glClearColor(colorClearColor.getX(), colorClearColor.getY(), colorClearColor.getZ(), colorClearColor.getW());

		this.colorClearColor = colorClearColor;
	}

	@Override
	public void clearBoundRenderTargetAll()
	{
		//Flushes any command before messing them up
		renderingContext.flush();
		
		//Resets those to default values
		setClearDepth(1);
		setClearColor(new Vector4fm(0));
		
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
	}

	@Override
	public void clearBoundRenderTargetZ(float z)
	{
		//Flushes any command before messing them up
		renderingContext.flush();
		
		setClearDepth(z);
		glClear(GL_DEPTH_BUFFER_BIT);
	}

	@Override
	public void clearBoundRenderTargetColor(Vector4fm color)
	{
		//Flushes any command before messing them up
		renderingContext.flush();
		
		setClearColor(color);
		glClear(GL_COLOR_BUFFER_BIT);
	}

	@Override
	public RenderTargetAttachementsConfiguration newConfiguration(RenderTarget depth, RenderTarget... colors) {
		return new FrameBufferObjectGL(depth, colors);
	}

}
