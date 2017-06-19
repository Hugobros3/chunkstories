package io.xol.chunkstories.api.rendering.textures;

import java.nio.ByteBuffer;

public interface Texture1D extends Texture {

	boolean uploadTextureData(int width, ByteBuffer data);

	/**
	 * Determines if a texture will loop arround itself or clamp to it's edges
	 * 
	 * @param on
	 */
	void setTextureWrapping(boolean on);

	void setLinearFiltering(boolean on);

	int getWidth();

	long getVramUsage();

}