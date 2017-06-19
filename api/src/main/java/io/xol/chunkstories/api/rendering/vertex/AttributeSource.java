package io.xol.chunkstories.api.rendering.vertex;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * A data source for vertex shaders attribute inputs
 */
public interface AttributeSource
{
	/**
	 * Setups this attributeSource to it's slot, the enabling/allocation/disabling of vertex attributes is up to the engine
	 * This isn't really meant to be a part of the specification, but is for simplicity reasons. Sorry for platform-agnosticity fans :/
	 */
	public void setup(int gl_AttributeLocation);
}
