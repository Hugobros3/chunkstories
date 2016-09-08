package io.xol.chunkstories.api.rendering;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Abstracts the shaders uniforms ( excluding textures, see TexturingConfiguration )
 */
public interface UniformsConfiguration
{
	/**
	 * Used by RenderingCommands to determine if they can be merged together and instanced
	 */
	public boolean isCompatibleWith(UniformsConfiguration uniformsConfiguration);
	
	/**
	 * Setups the uniforms for the shader
	 */
	public void setup();
}
