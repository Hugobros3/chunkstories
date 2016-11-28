package io.xol.chunkstories.api.rendering;

import io.xol.engine.graphics.fbo.FrameBufferObject;
import io.xol.engine.math.lalgb.Vector4f;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface RenderTargetManager
{
	/**
	 * Get the current FBO bound
	 */
	public FrameBufferObject getFramebufferWritingTo();
	
	/**
	 * Binds a new FBO to render to
	 */
	public void setCurrentRenderTarget(FrameBufferObject fbo);
	
	/**
	 * Clears both types of bound rendertargets
	 * Equivalent to calling clearBoundRenderTargetZ(1.0f); and clearBoundRenderTargetColor(null);
	 */
	public void clearBoundRenderTargetAll();
	
	/**
	 * Clears the depth render target (if bound) to the level specified
	 */
	public void clearBoundRenderTargetZ(float z);
	
	/**
	 * Clears the color render target (if bound) to the color specified
	 * Giving a null Vector4f is assummed to mean Vector4f(0, 0, 0, 0);
	 */
	public void clearBoundRenderTargetColor(Vector4f color);
	
	/**
	 * Enables or disable the depth mask ( wether or not depth information is written )
	 */
	public void setDepthMask(boolean on);
	
	/**
	 * @return Wether the depth mask is active or not
	 */
	public boolean getDepthMask();
}
