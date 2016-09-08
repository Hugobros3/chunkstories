package io.xol.chunkstories.api.rendering;

import java.util.Map;

//(c) 2015-2016 XolioWare Interactive
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
