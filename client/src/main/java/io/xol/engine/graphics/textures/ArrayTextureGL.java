package io.xol.engine.graphics.textures;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.GL30;

import io.xol.chunkstories.api.rendering.textures.ArrayTexture;
import io.xol.chunkstories.api.rendering.textures.TextureFormat;
import io.xol.chunkstories.api.util.ChunkStoriesLogger.LogLevel;
import io.xol.chunkstories.api.util.ChunkStoriesLogger.LogType;
import io.xol.chunkstories.client.RenderingConfig;
import io.xol.chunkstories.tools.ChunkStoriesLoggerImplementation;

public class ArrayTextureGL extends TextureGL implements ArrayTexture {
	
	final int layers;
	final int size;
	
	boolean wrapping = false;
	boolean mipmapping = false;
	boolean linearFiltering = false;
	int baseMipmapLevel = 0;
	int maxMipmapLevel = 1000;
	
	private boolean mipmapsUpToDate = true;
	
	public ArrayTextureGL(TextureFormat type, int size, int layers) {
		super(type);
		
		this.aquireID();
		this.size = size;
		this.layers = layers;

		bind();
		
		//glTexImage3D(GL_TEXTURE_2D_ARRAY, 0, type.getInternalFormat(), size, size, layers, 0, type.getFormat(), type.getType(), (ByteBuffer)null);
		int a = size;
		int b = 0;
		while(true) {
			
			glTexImage3D(GL_TEXTURE_2D_ARRAY, b, type.getInternalFormat(), a, a, layers, 0, type.getFormat(), type.getType(), (ByteBuffer)null);
			
			if(a == 1)
				break;
			
			a /= 2;
			b++;
		}

		glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
	}
	
	public void setTextureWrapping(boolean on)
	{
		if (glId < 0) // Don't bother with invalid textures
			return;
		boolean applyParameters = false;
	
		if (wrapping != on) // We changed something so we redo them
			applyParameters = true;
	
		wrapping = on;
	
		if (!applyParameters)
			return;
		bind();
		if (!on)
		{
			glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		}
		else
		{
			glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_REPEAT);
			glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_REPEAT);
		}
	}
	
	public void setMipMapping(boolean on)
	{
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
		/*if (mipmapping && !mipmapsUpToDate)
			computeMipmaps();*/
	}

	public void computeMipmaps()
	{
		//System.out.println("Computing mipmap for "+glId);
		bind();
		
		//Regenerate the mipmaps only when necessary
		if (RenderingConfig.gl_openGL3Capable)
			GL30.glGenerateMipmap(GL_TEXTURE_2D_ARRAY);
		else if (RenderingConfig.gl_fbExtCapable)
			ARBFramebufferObject.glGenerateMipmap(GL_TEXTURE_2D_ARRAY);
	
		mipmapsUpToDate = true;
	}

	public void setLinearFiltering(boolean on)
	{
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
	
	protected void applyTextureParameters()
	{
		//Generate mipmaps
		if (mipmapping)
		{
			/*if (RenderingConfig.gl_openGL3Capable)
				GL30.glGenerateMipmap(GL_TEXTURE_2D);
			else if (RenderingConfig.gl_fbExtCapable)
				ARBFramebufferObject.glGenerateMipmap(GL_TEXTURE_2D);
			 */
			mipmapsUpToDate = true;
		}
		if (!wrapping)
		{
			glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		}
		else
		{
			glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_REPEAT);
			glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_REPEAT);
		}
		applyFiltering();
	}
	
	private void applyFiltering()
	{
		//System.out.println("Set filtering called for "+name+" "+linearFiltering);
		if (mipmapping)
		{
			if (linearFiltering)
			{
				glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
				glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			}
			else
			{
				glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST);
				glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			}
	
			glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_BASE_LEVEL, baseMipmapLevel);
			glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAX_LEVEL, maxMipmapLevel);
		}
		else
		{
			if (linearFiltering)
			{
				glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
				glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			}
			else
			{
				glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
				glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			}
		}
	}
	
	public void setMipmapLevelsRange(int baseLevel, int maxLevel)
	{
		if (glId < 0) // Don't bother with invalid textures
			return;
		boolean applyParameters = false;
	
		int actualMaxMipLevelPossible = getMaxMipmapLevel();
		if(maxLevel > actualMaxMipLevelPossible)
		{
			ChunkStoriesLoggerImplementation.getInstance().log("Warning, tried setting mipLevel > max permitted by texture size. Correcting.", LogType.RENDERING, LogLevel.WARN);
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
	
	public int getMaxMipmapLevel()
	{
		int width = size;
		int height = size;
		
		int level = 0;
		while(width != 1 && height != 1)
		{
			if(width == 0 || height == 0)
				break;
			width /= 2;
			height /=2;
			
			level++;
		}
		
		return level;
	}

	@Override
	public void bind() {
		glBindTexture(GL_TEXTURE_2D_ARRAY, glId);
	}
	
	/** MUST BE CALLED FROM MAIN THREAD, NO FAILSAFES 
	 * WILL ALSO FUCK UP BIND POINTS, DO YOU MATH KIDS */
	public void uploadTextureData(int layer, int level, ByteBuffer data) {
		bind();
		int sizeLod = size;
		for(int i = 0; i < level; i++)
			sizeLod /= 2;
		glTexSubImage3D(GL_TEXTURE_2D_ARRAY, level, 0, 0, layer, sizeLod, sizeLod, 1, type.getFormat(), type.getType(), data);
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
