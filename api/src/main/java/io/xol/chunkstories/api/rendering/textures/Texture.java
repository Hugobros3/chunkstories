package io.xol.chunkstories.api.rendering.textures;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** Completely abstracted-out Texture class. Represents some kind of picture on the GPU. */
public interface Texture {

	public TextureFormat getType();

	public void bind();

	/** Unloads the texture from memory and frees the associated memory. Not actually recommanded since GC works on texture objects and will clear linked
	 * GPU memory as well.
	 */
	public boolean destroy();

	/** Returns the VRAM usage, in bytes */
	public long getVramUsage();
}