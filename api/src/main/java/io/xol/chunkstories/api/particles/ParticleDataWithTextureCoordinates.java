package io.xol.chunkstories.api.particles;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface ParticleDataWithTextureCoordinates {
	
	public float getTextureCoordinateXTopLeft();
	public float getTextureCoordinateXTopRight();
	public float getTextureCoordinateXBottomLeft();
	public float getTextureCoordinateXBottomRight();
	
	public float getTextureCoordinateYTopLeft();
	public float getTextureCoordinateYTopRight();
	public float getTextureCoordinateYBottomLeft();
	public float getTextureCoordinateYBottomRight();
}
