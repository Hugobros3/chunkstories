package io.xol.chunkstories.api.rendering.pipeline;

import java.util.Map;

import io.xol.chunkstories.api.exceptions.rendering.NotEnoughtTextureUnitsException;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.engine.graphics.textures.Texture2D;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Abstracts the configuration of the texturing units when drawing
 */
public interface TexturingConfiguration
{
	/**
	 * Returns the currently bound 2D textures
	 * @return
	 */
	public Map<String, Texture2D> getBoundTextures2D();
	
	/**
	 * Used by RenderingCommands to determine if they can be merged together and instanced
	 */
	public boolean isCompatibleWith(TexturingConfiguration boundTextures);
	
	public void setup(RenderingInterface renderingInterface) throws NotEnoughtTextureUnitsException;
}
