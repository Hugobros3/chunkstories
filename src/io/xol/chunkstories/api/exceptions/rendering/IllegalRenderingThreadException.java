package io.xol.chunkstories.api.exceptions.rendering;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Rendering functions should only be called inside the main rendering thread.
 * To check if you are in the proper thread, please use GameWindowOpenGL.isMainGLWindow()
 */
public class IllegalRenderingThreadException extends RuntimeException
{

	/**
	 * 
	 */
	private static final long serialVersionUID = -8543094072345220060L;

}
