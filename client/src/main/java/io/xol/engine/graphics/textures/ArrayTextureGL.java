package io.xol.engine.graphics.textures;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.ByteBuffer;

import io.xol.chunkstories.api.rendering.textures.ArrayTexture;
import io.xol.chunkstories.api.rendering.textures.TextureFormat;

public class ArrayTextureGL extends TextureGL implements ArrayTexture {
	
	final int layers;
	final int size;
	
	public ArrayTextureGL(TextureFormat type, int size, int layers) {
		super(type);
		
		this.aquireID();
		this.size = size;
		this.layers = layers;

		bind();
		
		glTexImage3D(GL_TEXTURE_2D_ARRAY, 0, type.getInternalFormat(), size, size, layers, 0, type.getFormat(), type.getType(), (ByteBuffer)null);

		glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
	}

	@Override
	public void bind() {
		glBindTexture(GL_TEXTURE_2D_ARRAY, glId);
	}
	
	/** MUST BE CALLED FROM MAIN THREAD, NO FAILSAFES 
	 * WILL ALSO FUCK UP BIND POINTS, DO YOU MATH KIDS */
	public void uploadTextureData(int layer, int level, ByteBuffer data) {
		bind();
		glTexSubImage3D(GL_TEXTURE_2D_ARRAY, level, 0, 0, layer, size, size, 1, type.getFormat(), type.getType(), data);
	}

	@Override
	public long getVramUsage() {
		return type.getBytesPerTexel() * size * size * layers;
	}

	@Override
	public int getSize() {
		return size;
	}

	@Override
	public int getLayers() {
		return layers;
	}
}
