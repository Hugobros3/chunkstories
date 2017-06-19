package io.xol.chunkstories.api.rendering.pipeline;

import java.util.Map;

import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.vertex.AttributeSource;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Abstracts the vertex shader inputs
 */
public interface AttributesConfiguration
{
	/**
	 * Returns the currently bound attributes
	 * @return
	 */
	public Map<String, AttributeSource> getBoundAttributes();
	
	/**
	 * Used by RenderingCommands to determine if they can be merged together and instanced
	 */
	public boolean isCompatibleWith(AttributesConfiguration attributesConfiguration);

	public void setup(RenderingInterface renderingInterface);
}
