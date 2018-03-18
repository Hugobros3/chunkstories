//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.renderer.opengl.texture;

import static org.lwjgl.opengl.GL11.GL_LEQUAL;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_LINEAR_MIPMAP_LINEAR;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_NEAREST_MIPMAP_NEAREST;
import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_BASE_LEVEL;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_MAX_LEVEL;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL14.GL_COMPARE_R_TO_TEXTURE;
import static org.lwjgl.opengl.GL14.GL_TEXTURE_COMPARE_FUNC;
import static org.lwjgl.opengl.GL14.GL_TEXTURE_COMPARE_MODE;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_DEPTH_ATTACHMENT;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.GL30;

import io.xol.chunkstories.api.exceptions.rendering.IllegalRenderingThreadException;
import io.xol.chunkstories.api.rendering.target.RenderTarget;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.api.rendering.textures.TextureFormat;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.RenderingConfig;
import io.xol.chunkstories.renderer.opengl.OpenGLDebugOutputCallback;

public abstract class Texture2DGL extends TextureGL implements RenderTarget, Texture2D {
	protected int width;
	protected int height;

	// Filtering parameters
	boolean wrapping = true;
	boolean mipmapping = false;
	boolean linearFiltering = true;
	int baseMipmapLevel = 0;
	int maxMipmapLevel = 1000;

	protected boolean mipmapsUpToDate = false;

	static int currentlyBoundId = 0;

	protected Texture2DGL(TextureFormat type) {
		super(type);
	}
	
	@Override
	public boolean uploadTextureData(int width, int height, ByteBuffer data) {
		return uploadTextureData(width, height, 0, data);
	}
	
	@Override
	public boolean uploadTextureData(int width, int height, int level, ByteBuffer data) {
		if (Client.getInstance().getGameWindow().isMainGLWindow())
			return uploadTextureDataActual(width, height, level, data);
		else {
			return TextureGL.scheduled.add(new TextureRunnable() {
				@Override
				public void run() {
					uploadTextureData(width, height, level, data);
				}

				@Override
				public TextureGL getTexture() {
					return Texture2DGL.this;
				}

			});
		}
	}

	private boolean uploadTextureDataActual(int width, int height, int level, ByteBuffer data) {
		int k = currentlyBoundId;

		glActiveTexture(GL_TEXTURE0 + 15);
		bind();
		this.width = width;
		this.height = height;

		glTexImage2D(GL_TEXTURE_2D, 0, type.getInternalFormat(), width, height, 0, type.getFormat(), type.getType(), (ByteBuffer) data);

		computeMipmaps();
		applyTextureWrapping();
		applyFiltering();

		if (k > 0)
			glBindTexture(GL_TEXTURE_2D, currentlyBoundId);

		return true;
	}

	/**
	 * @return the OpenGL GL_TEXTURE id of this object
	 */
	public int getId() {
		return glId;
	}

	@Override
	public void bind() {
		if (!Client.getInstance().getGameWindow().isMainGLWindow())
			throw new IllegalRenderingThreadException();

		// Don't bother
		if (glId == -2) {
			logger().error("Critical mess-up: Tried to bind a destroyed Texture2D " + this + ". Terminating process immediately.");
			Thread.dumpStack();
			System.exit(-801);
			// throw new RuntimeException("Tryed to bind a destroyed VerticesBuffer");
		}

		// Allow creation only in intial state
		if (glId == -1)
			aquireID();

		glBindTexture(GL_TEXTURE_2D, glId);

		if (RenderingConfig.DEBUG_OPENGL)
			checkForErrors();
		currentlyBoundId = glId;
	}

	private void checkForErrors() {
		if (OpenGLDebugOutputCallback.didErrorHappen())
			System.out.println("Something went wrong with " + this);
	}

	@Override
	public String toString() {
		return "[" + getClass().getSimpleName() + " id: " + glId + " size:" + width + "x" + height + " format:" + type.name() + "]";
	}

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
		applyTextureWrapping();
	}
	
	protected void applyTextureWrapping() {
		if (!wrapping) {
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		} else {
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
		}
	}
	
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
		applyFiltering();
	}

	protected void applyFiltering() {
		if (type == TextureFormat.DEPTH_SHADOWMAP) {
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_R_TO_TEXTURE);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
		}
		
		if (mipmapping) {
			if (linearFiltering) {
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			} else {
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			}

			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, baseMipmapLevel);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, maxMipmapLevel);
		} else {
			if (linearFiltering) {
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			} else {
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			}
		}
	}

	@Override
	public void setMipMapping(boolean on) {
		if (glId < 0) // Don't bother with invalid textures
			return;
		boolean applyParameters = false;

		if (mipmapping != on) // We changed something so we redo them
			applyParameters = true;

		mipmapping = on;

		if (!applyParameters)
			return;
		bind();
		
		applyFiltering();
		computeMipmaps();
	}

	@Override
	public void computeMipmaps() {
		if (mipmapping && !mipmapsUpToDate) {
			bind();
	
			// Regenerate the mipmaps only when necessary
			if (RenderingConfig.gl_openGL3Capable)
				GL30.glGenerateMipmap(GL_TEXTURE_2D);
			else if (RenderingConfig.gl_fbExtCapable)
				ARBFramebufferObject.glGenerateMipmap(GL_TEXTURE_2D);
	
			mipmapsUpToDate = true;
		}
	}
	
	@Override
	public void setMipmapLevelsRange(int baseLevel, int maxLevel) {
		if (glId < 0) // Don't bother with invalid textures
			return;
		boolean applyParameters = false;

		int actualMaxMipLevelPossible = getMaxMipmapLevel();
		if (maxLevel > actualMaxMipLevelPossible) {
			logger().warn("Warning, tried setting mipLevel > max permitted by texture size. Correcting.");
			Thread.dumpStack();
			maxLevel = actualMaxMipLevelPossible;
		}

		if (this.baseMipmapLevel != baseLevel || this.maxMipmapLevel != maxLevel) // We changed something so we redo them
			applyParameters = true;

		baseMipmapLevel = baseLevel;
		maxMipmapLevel = maxLevel;

		if (!applyParameters)
			return;
		bind();
		applyFiltering();
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public long getVramUsage() {
		int surface = getWidth() * getHeight();
		return surface * type.getBytesPerTexel();
	}

	@Override
	public int getMaxMipmapLevel() {
		int width = this.width;
		int height = this.height;

		int level = 0;
		while (width != 1 && height != 1) {
			if (width == 0 || height == 0)
				break;
			width /= 2;
			height /= 2;

			level++;
		}

		return level;
	}
	
	@Override
	public void resize(int w, int h) {
		bind();

		if (this.width == w && this.height == h && !(type == TextureFormat.RGB_HDR))
			return;

		if (w < 0 || h < 0) {
			logger().warn("Tried to resize texture with negative size!");
			return;
		}
		
		this.width = w;
		this.height = h;
		
		glTexImage2D(GL_TEXTURE_2D, 0, type.getInternalFormat(), w, h, 0, type.getFormat(), type.getType(), (ByteBuffer) null);

		applyFiltering();
		applyTextureWrapping();
		computeMipmaps();
	}

	@Override
	public void attachAsDepth() {
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, this.getId(), 0);
	}

	@Override
	public void attachAsColor(int colorAttachement) {
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + colorAttachement, GL_TEXTURE_2D, this.getId(), 0);
	}

	@Override
	public RenderTarget getMipLevelAsRenderTarget(int mipLevel) {
		assert mipLevel <= this.getMaxMipmapLevel();

		return new RenderTarget() {

			int divisor = 1 << mipLevel;
			int mipWidth = getWidth() / divisor;
			int mipHeight = getHeight() / divisor;

			@Override
			public void attachAsDepth() {
				glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, getId(), mipLevel);
			}

			@Override
			public void attachAsColor(int colorAttachement) {
				glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + colorAttachement, GL_TEXTURE_2D, getId(), mipLevel);
			}

			@Override
			public void resize(int width, int height) {
				throw new UnsupportedOperationException("No.");
			}

			@Override
			public int getWidth() {
				return mipWidth;
			}

			@Override
			public int getHeight() {
				return mipHeight;
			}

			@Override
			public boolean destroy() {
				throw new UnsupportedOperationException("Neither.");
			}

		};
	}

}
