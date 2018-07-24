//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.renderer.opengl.texture;

import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_1D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glTexImage1D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

import java.nio.ByteBuffer;

import io.xol.chunkstories.api.exceptions.rendering.IllegalRenderingThreadException;
import io.xol.chunkstories.api.rendering.textures.Texture1D;
import io.xol.chunkstories.api.rendering.textures.TextureFormat;
import io.xol.chunkstories.client.Client;

public class Texture1DGL extends TextureGL implements Texture1D {
	String name;
	int width;
	boolean wrapping = true;
	boolean linearFiltering = true;

	public Texture1DGL(TextureFormat type) {
		super(type);
		// allTextureObjects.add(new WeakReference<Texture1D>(this));
	}

	public TextureFormat getType() {
		return type;
	}

	/**
	 * Returns the OpenGL GL_TEXTURE id of this object
	 * 
	 * @return
	 */
	public int getID() {
		return glId;
	}

	public void bind() {
		if (!Client.getInstance().getGameWindow().isMainGLWindow())
			throw new IllegalRenderingThreadException();

		// Don't bother
		if (glId == -2) {
			logger().error("Critical mess-up: Tried to bind a destroyed Texture1D " + this
					+ ". Terminating process immediately.");
			Thread.dumpStack();
			System.exit(-802);
			// throw new RuntimeException("Tryed to bind a destroyed VerticesBuffer");
		}

		if (glId == -1)
			aquireID();

		glBindTexture(GL_TEXTURE_1D, glId);
	}

	/*
	 * public synchronized boolean destroy() { if (glId >= 0) {
	 * glDeleteTextures(glId); totalTextureObjects--; } glId = -1; }
	 */

	// Texture modifications

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.xol.engine.graphics.textures.Texture1D#uploadTextureData(int,
	 * java.nio.ByteBuffer)
	 */
	@Override
	public boolean uploadTextureData(int width, ByteBuffer data) {
		bind();
		this.width = width;
		// glTexImage1D(GL_TEXTURE_1D, 0, GL_RGBA, width, 0, GL_RGBA, GL_UNSIGNED_BYTE,
		// data);
		glTexImage1D(GL_TEXTURE_1D, 0, type.getInternalFormat(), width, 0, type.getFormat(), type.getType(),
				(ByteBuffer) data);

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.xol.engine.graphics.textures.Texture1D#setTextureWrapping(boolean)
	 */
	@Override
	public void setTextureWrapping(boolean on) {
		if (glId < 0) // Don't bother with invalid textures
			return;
		boolean applyParameters = false;

		if (wrapping != on) // We changed something so we redo them
			applyParameters = true;

		wrapping = on;

		if (!applyParameters)
			return;
		bind();
		if (!on) {
			glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		} else {
			glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_WRAP_S, GL_REPEAT);
			glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_WRAP_T, GL_REPEAT);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.xol.engine.graphics.textures.Texture1D#setLinearFiltering(boolean)
	 */
	@Override
	public void setLinearFiltering(boolean on) {
		if (glId < 0) // Don't bother with invalid textures
			return;
		boolean applyParameters = false;

		if (linearFiltering != on) // We changed something so we redo them
			applyParameters = true;

		linearFiltering = on;

		if (!applyParameters)
			return;
		bind();
		setFiltering();
	}

	// Private function that sets both filering scheme and mipmap usage.
	private void setFiltering() {
		if (linearFiltering) {
			glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		} else {
			glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.xol.engine.graphics.textures.Texture1D#getWidth()
	 */
	@Override
	public int getWidth() {
		return width;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.xol.engine.graphics.textures.Texture1D#getVramUsage()
	 */
	@Override
	public long getVramUsage() {
		int surface = getWidth();
		return surface * type.getBytesPerTexel();
	}

	/*
	 * public static long getTotalVramUsage() { long vram = 0;
	 * 
	 * //Iterates over every instance reference, removes null ones and add up valid
	 * ones Iterator<WeakReference<Texture1D>> i = allTextureObjects.iterator();
	 * while (i.hasNext()) { WeakReference<Texture1D> reference = i.next();
	 * 
	 * Texture1D object = reference.get(); if (object != null) vram +=
	 * object.getVramUsage(); else i.remove(); }
	 * 
	 * return vram; }
	 * 
	 * private static int totalTextureObjects = 0; private static
	 * BlockingQueue<WeakReference<Texture1D>> allTextureObjects = new
	 * LinkedBlockingQueue<WeakReference<Texture1D>>();
	 */
}
