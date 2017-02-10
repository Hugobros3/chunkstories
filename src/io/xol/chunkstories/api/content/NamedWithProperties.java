package io.xol.chunkstories.api.content;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface NamedWithProperties
{
	public String getName();
	
	/** Resolves a property from the arguments defined in the .items file */
	public String resolveProperty(String propertyName);
	
	/** Do the same as above but provides a default fallback value instead of null, in case said property isn't defined. */
	public String resolveProperty(String propertyName, String defaultValue);
}
