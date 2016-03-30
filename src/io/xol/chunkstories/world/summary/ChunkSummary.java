package io.xol.chunkstories.world.summary;

import io.xol.chunkstories.client.FastConfig;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.voxel.VoxelTypes;
import io.xol.chunkstories.world.World;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.GL30;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.ARBTextureRg.*;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ChunkSummary
{
	// LZ4 compressors & decompressors
	static LZ4Factory factory = LZ4Factory.fastestInstance();
	public static LZ4Compressor compressor = factory.highCompressor(10);
	public static LZ4FastDecompressor decompressor = factory.fastDecompressor();

	public int hId = -1;
	public int tId = -1;

	public int vboId = -1;
	public int vboSize = 0;
	public int vboSizeTemp = 0;

	public FloatBuffer vboBuffer;

	public int[] heights;
	public int[] ids;

	public AtomicBoolean loaded = new AtomicBoolean(false);

	public AtomicBoolean uploadUpToDate = new AtomicBoolean(false);
	public File handler;

	public int rx, rz;

	public World world;

	public ChunkSummary(World world, int rx, int rz)
	{
		this.world = world;
		this.rx = rx;
		this.rz = rz;

		// 512kb per summary.
		heights = new int[256 * 256];
		ids = new int[256 * 256];
		
		//System.out.println("New chunk summary made");
		//Thread.currentThread().dumpStack();
	}

	public void load(File handler)
	{
		this.handler = handler;
		this.world.ioHandler.requestChunkSummaryLoad(this);
	}

	public void save(File handler)
	{
		this.handler = handler;
		this.world.ioHandler.requestChunkSummarySave(this);
	}

	int index(int x, int z)
	{
		return x * 256 + z;
	}

	public void set(int x, int y, int z, int t)
	{
		Voxel voxel = VoxelTypes.get(t);
		int h = getHeight(x, z);
		//If we place something solid over the last solid thing
		if ((voxel.isVoxelSolid() || voxel.isVoxelLiquid()) && y >= h)
		{
			if (y >= h)
			{
				heights[index(x, z)] = y;
				ids[index(x, z)] = t;
			}
		}
		else
		{
			// If removing the top block, start a loop to find bottom.
			if (y == h)
			{
				boolean loaded = false;
				boolean solid = false;
				boolean liquid = false;
				do
				{
					y--;
					loaded = world.isChunkLoaded(x / 32, y / 32, z / 32);
					solid = VoxelTypes.get(world.getDataAt(x, y, z, false)).isVoxelSolid();
					liquid = VoxelTypes.get(world.getDataAt(x, y, z, false)).isVoxelLiquid();
				}
				while (y >= 0 && loaded && !solid && !liquid);

				heights[index(x, z)] = y;
				ids[index(x, z)] = t;
			}
		}
	}

	public int getHeight(int x, int z)
	{
		return heights[index(x, z)];
	}

	public int getID(int x, int z)
	{
		return ids[index(x, z)];
	}

	public void forceSet(int x, int y, int z, int t)
	{
		heights[index(x, z)] = y;
		ids[index(x, z)] = t;
	}

	public float dekalX;
	public float dekalZ;

	public boolean glGen()
	{
		if (!loaded.get())
		{
			return false;
		}
		if (tId == -1)
		{
			//System.out.println("Textures created");
			hId = glGenTextures();
			tId = glGenTextures();
		}
		if (uploadUpToDate.get())
			return false;
		//System.out.println("Uploading chunk summary...");
		
		glBindTexture(GL_TEXTURE_2D, hId);
		ByteBuffer bb = ByteBuffer.allocateDirect(4 * 256 * 256);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		// IntBuffer ib = bb.asIntBuffer();
		for (int i = 0; i < 256 * 256; i++)
		{
			bb.putFloat(heights[i]);
		}
		// ib.put(heights);
		/*
		 * bb.rewind(); while(bb.hasRemaining()) { byte lel = bb.get(); if(lel
		 * != 0) System.out.println(lel); }
		 */
		// ib.flip();
		bb.flip(); // 0x822e is GL_R32F, 0x8235 is GL_R32I
		glTexImage2D(GL_TEXTURE_2D, 0, GL_R32F /* GL_R32F */, 256, 256, 0, GL_RED/* GL_RED_INTEGER */, GL_FLOAT, bb);

		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

		glHint(GL_GENERATE_MIPMAP_HINT, GL_FASTEST);

		if (FastConfig.openGL3Capable)
			GL30.glGenerateMipmap(GL_TEXTURE_2D);
		else if (FastConfig.fbExtCapable)
			ARBFramebufferObject.glGenerateMipmap(GL_TEXTURE_2D);

		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

		bb = ByteBuffer.allocateDirect(4 * 256 * 256);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < 256 * 256; i++)
		{
			int id = ids[i];
			// bb.putInt(id & 0x0000FFFF);
			bb.putFloat(id & 0x0000FFFF);
			// Voxel v = VoxelTypes.get(id);
			// Vector3f color = v.getVoxelColor();
			// bb.putFloat(color.x);
			// bb.putFloat(color.y);
			// bb.putFloat(color.z);
		}
		bb.rewind();
		glBindTexture(GL_TEXTURE_2D, tId);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_R32F /* GL_R32F */, 256, 256, 0, GL_RED/* GL_RED_INTEGER */, GL_FLOAT, bb);
		//glTexImage2D(GL_TEXTURE_2D, 0, GL_R32I, 256, 256, 0, GL_RED_INTEGER_EXT, GL_INT, bb);

		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		
		glHint(GL_GENERATE_MIPMAP_HINT, GL_FASTEST);

		if (FastConfig.openGL3Capable)
			GL30.glGenerateMipmap(GL_TEXTURE_2D);
		else if (FastConfig.fbExtCapable)
			ARBFramebufferObject.glGenerateMipmap(GL_TEXTURE_2D);
		
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		uploadUpToDate.set(true);
		return true;
	}

	public void free()
	{
		if (tId != -1)
		{
			glDeleteTextures(hId);
			glDeleteTextures(tId);
		}
		if (vboId != -1)
			glDeleteBuffers(vboId);
		//System.out.println("Freeing cs"+rx+rz);
	}

	public boolean isLoaded()
	{
		return loaded.get();
	}
}
