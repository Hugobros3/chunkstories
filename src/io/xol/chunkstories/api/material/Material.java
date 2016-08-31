package io.xol.chunkstories.api.material;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface Material
{
	public String getName();

	public String resolveProperty(String propertyName);
}