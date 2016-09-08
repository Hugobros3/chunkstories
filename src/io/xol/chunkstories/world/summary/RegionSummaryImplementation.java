package io.xol.chunkstories.world.summary;

import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.api.world.chunk.WorldUser;
import io.xol.chunkstories.api.world.heightmap.RegionSummary;
import io.xol.chunkstories.voxel.Voxels;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.engine.graphics.geometry.VerticesObject;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TextureFormat;

import java.io.File;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.lwjgl.BufferUtils;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

/**
 * A region summary contains metadata about an 8x8 chunks ( or 256x256 blocks ) vertical slice of the world
 */
public class RegionSummaryImplementation implements RegionSummary
{
	final WorldRegionSummariesHolder worldSummariesHolder;
	public WorldImplementation world;
	private final int regionX;
	private final int regionZ;

	private Set<WeakReference<WorldUser>> users = new HashSet<WeakReference<WorldUser>>();
	
	// LZ4 compressors & decompressors
	static LZ4Factory factory = LZ4Factory.fastestInstance();
	public static LZ4Compressor compressor = factory.highCompressor(10);
	public static LZ4FastDecompressor decompressor = factory.fastDecompressor();

	public final File handler;
	public AtomicBoolean summaryLoaded = new AtomicBoolean(false);

	public int[] heights;
	public int[] ids;

	//Textures (client renderer)
	public AtomicBoolean texturesUpToDate = new AtomicBoolean(false);
	
	public Texture2D heightsTexture;
	public Texture2D voxelTypesTexture;

	//Mesh (client renderer)
	public VerticesObject verticesObject;

	private byte[] vboDataToUpload = null;

	RegionSummaryImplementation(WorldRegionSummariesHolder worldSummariesHolder, int rx, int rz)
	{
		this.worldSummariesHolder = worldSummariesHolder;
		this.world = worldSummariesHolder.getWorld();
		this.regionX = rx;
		this.regionZ = rz;

		// 512kb per summary, use of max mipmaps for heights
		heights = new int[(int) Math.ceil(256 * 256 * (1 + 1 / 3D))];
		ids = new int[(int) Math.ceil(256 * 256 * (1 + 1 / 3D))];

		if (world instanceof WorldMaster)
			handler = new File(world.getFolderPath() + "/summaries/" + rx + "." + rz + ".sum");
		else
			handler = null;

		//Create rendering stuff only if we're a client world
		if(world instanceof WorldClient)
		{
			heightsTexture = new Texture2D(TextureFormat.RED_32F);
			voxelTypesTexture = new Texture2D(TextureFormat.RED_32F);
			verticesObject = new VerticesObject();
		}
		
		loadSummary();
	}

	@Override
	public int getRegionX()
	{
		return regionX;
	}

	@Override
	public int getRegionZ()
	{
		return regionZ;
	}
	
	@Override
	public Iterator<WorldUser> getSummaryUsers()
	{
		return new Iterator<WorldUser>()
		{
			Iterator<WeakReference<WorldUser>> i = users.iterator();
			WorldUser user;

			@Override
			public boolean hasNext()
			{
				while(user == null && i.hasNext())
				{
					user = i.next().get();
				}
				return user != null;
			}

			@Override
			public WorldUser next()
			{
				hasNext();
				WorldUser u = user;
				user = null;
				return u;
			}

		};
	}

	@Override
	public boolean registerUser(WorldUser user)
	{
		Iterator<WeakReference<WorldUser>> i = users.iterator();
		while (i.hasNext())
		{
			WeakReference<WorldUser> w = i.next();
			WorldUser u = w.get();
			if (u == null)
				i.remove();
			else if (u != null && u.equals(user))
				return false;
		}
		
		users.add(new WeakReference<WorldUser>(user));
		
		//if(chunk == null)
		//	loadChunk();
		
		return true;
	}

	@Override
	/**
	 * Unregisters user and if there is no remaining user, unloads the chunk
	 */
	public boolean unregisterUser(WorldUser user)
	{
		Iterator<WeakReference<WorldUser>> i = users.iterator();
		while (i.hasNext())
		{
			WeakReference<WorldUser> w = i.next();
			WorldUser u = w.get();
			if (u == null)
				i.remove();
			else if (u != null && u.equals(user))
				i.remove();
		}
		
		if(users.isEmpty())
		{
			unloadSummary();
			return true;
		}
		
		return false;
	}

	/**
	 * Iterates over users references, cleans null ones and if the result is an empty list it promptly unloads the chunk.
	 */
	public boolean unloadsIfUnused()
	{
		Iterator<WeakReference<WorldUser>> i = users.iterator();
		while (i.hasNext())
		{
			WeakReference<WorldUser> w = i.next();
			WorldUser u = w.get();
			if (u == null)
				i.remove();
		}
		
		if(users.isEmpty())
		{
			unloadSummary();
			return true;
		}
		
		return false;
	}

	public int countUsers()
	{
		int c = 0;
		
		Iterator<WeakReference<WorldUser>> i = users.iterator();
		while (i.hasNext())
		{
			WeakReference<WorldUser> w = i.next();
			WorldUser u = w.get();
			if (u == null)
				i.remove();
			else
				c++;
		}
		
		return c;
	}
	
	private void loadSummary()
	{
		this.world.ioHandler.requestRegionSummaryLoad(this);
	}

	public void saveSummary()
	{
		this.world.ioHandler.requestRegionSummarySave(this);
	}

	private int index(int x, int z)
	{
		return x * 256 + z;
	}

	@Override
	public void updateOnBlockModification(int worldX, int height, int worldZ, int voxelData)
	{
		Voxel voxel = Voxels.get(voxelData);
		int h = getHeight(worldX, worldZ);
		//If we place something solid over the last solid thing
		if ((voxel.isVoxelSolid() || voxel.isVoxelLiquid()) && height >= h)
		{
			if (height >= h)
			{
				heights[index(worldX, worldZ)] = height;
				ids[index(worldX, worldZ)] = voxelData;
			}
		}
		else
		{
			// If removing the top block, start a loop to find bottom.
			if (height == h)
			{
				boolean loaded = false;
				boolean solid = false;
				boolean liquid = false;
				do
				{
					height--;
					loaded = world.isChunkLoaded(worldX / 32, height / 32, worldZ / 32);
					
					voxelData = world.getVoxelData(worldX, height, worldZ);
					solid = Voxels.get(voxelData).isVoxelSolid();
					liquid = Voxels.get(voxelData).isVoxelLiquid();
				}
				while (height >= 0 && loaded && !solid && !liquid);

				heights[index(worldX, worldZ)] = height;
				ids[index(worldX, worldZ)] = voxelData;
			}
		}
	}

	@Override
	public void setHeightAndId(int worldX, int height, int worldZ, int voxelData)
	{
		heights[index(worldX, worldZ)] = height;
		ids[index(worldX, worldZ)] = voxelData;
	}

	@Override
	public int getHeight(int x, int z)
	{
		return heights[index(x, z)];
	}

	@Override
	public int getVoxelData(int x, int z)
	{
		return ids[index(x, z)];
	}

	public boolean uploadNeededData()
	{
		return uploadTextures() || uploadModel();
	}

	private boolean uploadModel()
	{
		synchronized (this)
		{
			if (vboDataToUpload == null)
				return false;
			
			//if (vboId == -1)
			//	vboId = glGenBuffers();

			byte[] keep = vboDataToUpload;
			
			//vboSize = 0;
			ByteBuffer byteBuffer = BufferUtils.createByteBuffer(keep.length);
			byteBuffer.put(keep);
			byteBuffer.flip();
			
			verticesObject.uploadData(byteBuffer);

			//glBindBuffer(GL_ARRAY_BUFFER, vboId);
			//glBufferData(GL_ARRAY_BUFFER, byteBuffer, GL_DYNAMIC_DRAW);
			//vboSize = vboDataToUpload.length / 12;

			vboDataToUpload = null;
		}
		return true;
	}

	private boolean uploadTextures()
	{
		if (!summaryLoaded.get())
			return false;
		
		/*if (voxelTypesTextureId == -1)
		{
			heightsTextureId = glGenTextures();
			voxelTypesTextureId = glGenTextures();
		}*/
		if (texturesUpToDate.get())
			return false;

		//Upload stuff
		//glBindTexture(GL_TEXTURE_2D, heightsTextureId);
		ByteBuffer bb = ByteBuffer.allocateDirect(4 * 256 * 256);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < 256 * 256; i++)
		{
			bb.putFloat(heights[i]);
		}

		bb.flip(); // 0x822e is GL_R32F, 0x8235 is GL_R32I
		
		heightsTexture.uploadTextureData(256, 256, bb);
		//glTexImage2D(GL_TEXTURE_2D, 0, GL_R32F /* GL_R32F */, 256, 256, 0, GL_RED/* GL_RED_INTEGER */, GL_FLOAT, bb);

		//Setup texture filtering
		/*glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);*/
		
		heightsTexture.setTextureWrapping(false);
		heightsTexture.setLinearFiltering(false);
		

		//Generate mipmaps
		/*if (FastConfig.openGL3Capable)
			GL30.glGenerateMipmap(GL_TEXTURE_2D);
		else if (FastConfig.fbExtCapable)
			ARBFramebufferObject.glGenerateMipmap(GL_TEXTURE_2D);*/

		//Upload stuff
		bb = ByteBuffer.allocateDirect(4 * 256 * 256);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < 256 * 256; i++)
		{
			int id = ids[i];
			Voxel v = Voxels.get(id);
			if(v.isVoxelLiquid())
				bb.putFloat(512f);
			else
				bb.putFloat(v.getVoxelTexture(id, VoxelSides.TOP, null).positionInColorIndex);
		}
		bb.rewind();
		//glBindTexture(GL_TEXTURE_2D, voxelTypesTextureId);
		//glTexImage2D(GL_TEXTURE_2D, 0, GL_R32F /* GL_R32F */, 256, 256, 0, GL_RED/* GL_RED_INTEGER */, GL_FLOAT, bb);

		voxelTypesTexture.uploadTextureData(256, 256, bb);
		
		//Setup texture filtering
		/*glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);*/
		
		voxelTypesTexture.setTextureWrapping(false);
		voxelTypesTexture.setLinearFiltering(false);

		//Generate mipmaps
		/*if (FastConfig.openGL3Capable)
			GL30.glGenerateMipmap(GL_TEXTURE_2D);
		else if (FastConfig.fbExtCapable)
			ARBFramebufferObject.glGenerateMipmap(GL_TEXTURE_2D);*/

		//Tell world renderer
		((WorldClient) this.world).getWorldRenderer().getFarTerrainRenderer().markFarTerrainMeshDirty();
		
		//Flag it
		texturesUpToDate.set(true);
		return true;
	}

	void unloadSummary()
	{
		if(world instanceof WorldClient)
		{
			heightsTexture.destroy();
			voxelTypesTexture.destroy();
			
			verticesObject.destroy();
		}
		
		if(!worldSummariesHolder.removeSummary(this))
		{
			System.out.println("Someone tryed to remove a summary twice !!!");
		}
	}

	public synchronized void sendNewModel(byte[] newModelData)
	{
		vboDataToUpload = newModelData;
	}
		
	public boolean isLoaded()
	{
		return summaryLoaded.get();
	}

	public void computeHeightMetadata()
	{
		//Max mipmaps
		int resolution = 128;
		int offset = 0;
		while (resolution > 1)
		{
			for (int x = 0; x < resolution; x++)
				for (int z = 0; z < resolution; z++)
				{
					//Fetch from the current resolution
					//int v00 = heights[offset + (resolution * 2) * (x * 2) + (z * 2)];
					//int v01 = heights[offset + (resolution * 2) * (x * 2) + (z * 2 + 1)];
					//int v10 = heights[offset + (resolution * 2) * (x * 2 + 1) + (z * 2)];
					//int v11 = heights[offset + (resolution * 2) * (x * 2 + 1) + (z * 2) + 1];
					
					int maxIndex = 0;
					int maxHeight = 0;
					for(int i = 0; i <= 1; i++)
						for(int j = 0; j <= 1; j++)
						{
							int locationThere = offset + (resolution * 2) * (x * 2 + i) + (z * 2) + j;
							int heightThere = heights[locationThere];
							
							if(heightThere >= maxHeight)
							{
								maxIndex = locationThere;
								maxHeight = heightThere;
							}
						}
					
					//int maxHeight = max(max(v00, v01), max(v10, v11));

					//Skip the already passed steps and the current resolution being sampled data to go write the next one
					heights[offset + (resolution * 2) * (resolution * 2) + resolution * x + z] = maxHeight;
					ids[offset + (resolution * 2) * (resolution * 2) + resolution * x + z] = ids[maxIndex];
				}

			offset += resolution * 2 * resolution * 2;
			resolution /= 2;
		}
	}

	static int[] offsets = { 0, 65536, 81920, 86016, 87040, 87296, 87360, 87376, 87380, 87381 };

	public int getHeightMipmapped(int x, int z, int level)
	{
		if (level > 8)
			return -1;
		int resolution = 256 >> level;
		x >>= level;
		z >>= level;
		int offset = offsets[level];
		return heights[offset + resolution * x + z];
	}
	
	public int getDataMipmapped(int x, int z, int level)
	{
		if (level > 8)
			return -1;
		int resolution = 256 >> level;
		x >>= level;
		z >>= level;
		int offset = offsets[level];
		return ids[offset + resolution * x + z];
	}
}
