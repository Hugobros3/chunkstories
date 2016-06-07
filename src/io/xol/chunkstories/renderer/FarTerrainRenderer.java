package io.xol.chunkstories.renderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.xol.engine.math.lalgb.Vector3f;
import io.xol.engine.math.lalgb.Vector4f;

import org.lwjgl.BufferUtils;

import io.xol.chunkstories.client.FastConfig;
import io.xol.chunkstories.renderer.HeightmapMeshSummarizer.Surface;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.voxel.VoxelTextures;
import io.xol.chunkstories.voxel.VoxelTypes;
import io.xol.chunkstories.world.World;
import io.xol.chunkstories.world.summary.RegionSummary;
import io.xol.engine.model.RenderingContext;
import io.xol.engine.shaders.ShaderProgram;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class FarTerrainRenderer
{
	private static final int TRIANGLES_PER_FACE = 2; // 2 triangles per face
	private static final int TRIANGLE_SIZE = 3; // 3 vertex per triangles
	private static final int VERTEX_SIZE = 3; // A vertex is 3 coordinates : xyz

	World world;

	int maxLodLevels = 6;
	List<RegionMesh> regionsToRender = new ArrayList<RegionMesh>();

	//FloatBufferPool fbPool = new FloatBufferPool(96, 25000 * VERTEX_SIZE * TRIANGLE_SIZE * TRIANGLES_PER_FACE);

	public FarTerrainRenderer(World world)
	{
		this.world = world;
	}

	boolean blocksTexturesSummaryDone = false;
	int blocksTexturesSummaryId = -1;

	public void redoBlockTexturesSummary()
	{
		blocksTexturesSummaryDone = false;
	}

	public int getBlocksTexturesSummaryId()
	{
		if (!blocksTexturesSummaryDone)
		{
			if (blocksTexturesSummaryId == -1)
				blocksTexturesSummaryId = glGenTextures();

			glBindTexture(GL_TEXTURE_1D, blocksTexturesSummaryId);

			int size = 512;
			ByteBuffer bb = ByteBuffer.allocateDirect(size * 4);
			bb.order(ByteOrder.LITTLE_ENDIAN);
			Voxel vox;
			BlockRenderInfo temp = new BlockRenderInfo(0);
			for (int i = 0; i < size; i++)
			{
				vox = VoxelTypes.get(i);
				temp.data = i;
				Vector4f colorAndAlpha = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
				if (vox != null)
					colorAndAlpha = VoxelTextures.getTextureColorAlphaAVG(vox.getVoxelTexture(0, 0, temp).name);

				// colorAndAlpha = new Vector4f(1f, 0.5f, 1f, 1f);
				bb.put((byte) (colorAndAlpha.x * 255));
				bb.put((byte) (colorAndAlpha.y * 255));
				bb.put((byte) (colorAndAlpha.z * 255));
				bb.put((byte) (colorAndAlpha.w * 255));
			}
			bb.flip();
			glTexImage1D(GL_TEXTURE_1D, 0, GL_RGBA, size, 0, GL_RGBA, GL_UNSIGNED_BYTE, bb);

			glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

			blocksTexturesSummaryDone = true;
		}
		return blocksTexturesSummaryId;
	}

	int cameraChunkX, cameraChunkZ;

	public int draw(RenderingContext renderingContext, ShaderProgram terrain)
	{
		int elements = 0;
		glDisable(GL_CULL_FACE); // culling for our glorious terrain
		glLineWidth(1.0f);
		
		int vertexIn = terrain.getVertexAttributeLocation("vertexIn");
		int normalIn = terrain.getVertexAttributeLocation("normalIn");
		renderingContext.enableVertexAttribute(vertexIn);	
		renderingContext.enableVertexAttribute(normalIn);	
		
		//Sort to draw near first
		List<RegionMesh> regionsToRenderSorted = new ArrayList<RegionMesh>(regionsToRender);
		//renderingContext.getCamera().getLocation;
		Camera camera = renderingContext.getCamera();
		int camRX = (int) (-camera.pos.x / 256);
		int camRZ = (int) (-camera.pos.z / 256);
		
		regionsToRenderSorted.sort(new Comparator<RegionMesh>() {

			@Override
			public int compare(RegionMesh a, RegionMesh b)
			{
				int distanceA = Math.abs(a.rxDisplay - camRX) + Math.abs(a.rzDisplay - camRZ);
				//System.out.println(camRX + " : " + distanceA);
				int distanceB = Math.abs(b.rxDisplay - camRX) + Math.abs(b.rzDisplay - camRZ);
				return distanceA - distanceB;
			}
			
		});
		
		for (RegionMesh rs : regionsToRenderSorted)
		{
			float height = 1024f;
			if(!renderingContext.getCamera().isBoxInFrustrum(new Vector3f(rs.rxDisplay * 256 + 128, height / 2, rs.rzDisplay * 256 + 128), new Vector3f(256, height, 256)))
				continue;
			
			terrain.setUniformSampler(1, "groundTexture", rs.regionSummary.tId);

			glBindTexture(GL_TEXTURE_2D, rs.regionSummary.tId);
			//glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER,  GL_NEAREST_MIPMAP_NEAREST);
			//glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER,  GL_NEAREST_MIPMAP_NEAREST);
			
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			
			terrain.setUniformSampler(0, "heightMap", rs.regionSummary.hId);
			glBindTexture(GL_TEXTURE_2D, rs.regionSummary.hId);
			if(FastConfig.hqTerrain)
			{
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER,  GL_LINEAR);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER,  GL_LINEAR);
			}
			else
			{
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER,  GL_NEAREST_MIPMAP_NEAREST);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER,  GL_NEAREST_MIPMAP_NEAREST);
			}
			
			terrain.setUniformSampler1D(2, "blocksTexturesSummary", getBlocksTexturesSummaryId());
			terrain.setUniformFloat2("regionPosition", rs.regionSummary.rx, rs.regionSummary.rz);

			// terrain.setUniformFloat2("chunkPositionActual", cs.dekalX,
			// cs.dekalZ);
			terrain.setUniformFloat2("chunkPosition", rs.rxDisplay * 256, rs.rzDisplay * 256);

			glBindBuffer(GL_ARRAY_BUFFER, rs.vbo);
			//glVertexPointer(3, GL_FLOAT, 0, 0L);
			//glVertexAttribPointer(vertexIn, 3, GL_FLOAT, false, 0, 0L);
			glVertexAttribPointer(vertexIn, 3, GL_SHORT, false, 12, 0L);
			glVertexAttribPointer(normalIn, 4, GL_UNSIGNED_BYTE, false, 12, 8L);
			
			elements += rs.vboSize;

			if (rs.vboSize > 0 && rs.regionSummary.hId >= 0)
				//glDrawArrays(GL_QUADS, 0, rs.vboSize/6*4);
				glDrawArrays(GL_TRIANGLES, 0, rs.vboSize);
		}
		renderingContext.disableVertexAttribute(vertexIn);
		renderingContext.disableVertexAttribute(normalIn);
		return elements;
	}

	int lastRegionX = -1;
	int lastRegionZ = -1;

	int lastLevelDetail = -1;

	long lastGen = 0;
	int totalSize = 0;

	/**
	 * Regenerates the RegionSummaryMeshes 
	 * @param camPosX
	 * @param camPosZ
	 */
	//Single 6Mb Buffer
	ByteBuffer regionMeshBuffer = BufferUtils.createByteBuffer(256 * 256 * 5 * TRIANGLE_SIZE * VERTEX_SIZE * TRIANGLES_PER_FACE * ( 8 + 4));
	
	public void generateArround(double camPosX, double camPosZ)
	{
		long time = System.currentTimeMillis();
		
		cameraChunkX = (int) (camPosX / 32);
		cameraChunkZ = (int) (camPosZ / 32);
		
		totalSize = 0;

		for (RegionMesh rs : regionsToRender)
		{
			rs.delete();
		}
		regionsToRender.clear();

		int summaryDistance = 32;
		
		//Iterate over X chunks but skip whole regions
		int currentChunkX = cameraChunkX - summaryDistance;
		while (currentChunkX < cameraChunkX + summaryDistance)
		{
			//Computes where are we
			int currentRegionX = (int)Math.floor(currentChunkX / 8);
			//if(currentChunkX < 0)
			//	currentRegionX--;
			int nextRegionX = currentRegionX+1;
			int nextChunkX = nextRegionX * 8;

			//System.out.println("cx:" + currentChunkX + "crx: "+ currentRegionX + "nrx: " + nextRegionX + "ncx: " + nextChunkX);
			
			//Iterate over Z chunks but skip whole regions
			int currentChunkZ = cameraChunkZ - summaryDistance; 
			while(currentChunkZ < cameraChunkZ + summaryDistance)
			{
				//Computes where are we
				int currentRegionZ = (int)Math.floor(currentChunkZ / 8);
				int nextRegionZ = currentRegionZ+1;
				//if(currentChunkZ < 0)
				//	currentRegionZ--;
				int nextChunkZ = nextRegionZ * 8;
				
				//System.out.println("cz:" + currentChunkZ + "crz: "+ currentRegionZ + "nrz: " + nextRegionZ + "ncz: " + nextChunkZ);

				//Clear shit
				regionMeshBuffer.clear();
				
				int rx = currentChunkX / 8;
				int rz = currentChunkZ / 8;
				if (currentChunkZ < 0 && currentChunkZ % 8 != 0)
					rz--;
				if (currentChunkX < 0 && currentChunkX % 8 != 0)
					rx--;
				RegionMesh regionMesh = new RegionMesh(rx, rz, world.regionSummaries.get(currentChunkX * 32, currentChunkZ * 32));
				
				int rcx = currentChunkX % world.getSizeInChunks();
				if (rcx < 0)
					rcx += world.getSizeInChunks();
				int rcz = currentChunkZ % world.getSizeInChunks();
				if (rcz < 0)
					rcz += world.getSizeInChunks();
				
				int[] heightMap = regionMesh.regionSummary.heights;
				
				int vertexCount = 0;

				//Details cache array
				int[] details2use = new int[100];
				for(int scx = -1; scx < 9; scx++)
					for(int scz = -1; scz < 9; scz++)
					{
						int regionMiddleX = currentRegionX * 8 + scx;
						int regionMiddleZ = currentRegionZ * 8 + scz;
						int detail = (int) 
								(Math.sqrt(Math.abs(regionMiddleX - cameraChunkX)*Math.abs(regionMiddleX - cameraChunkX) 
										+ Math.abs(regionMiddleZ - cameraChunkZ)*Math.abs(regionMiddleZ - cameraChunkZ)) / (FastConfig.hqTerrain ? 6f : 4f));
						
						if(detail > 5)
							detail = 5;

						if(!FastConfig.hqTerrain && detail < 2)
							detail = 2;
						
						details2use[(scx+1)*10+(scz+1)] = detail;
					}
				
				for(int scx = 0; scx < 8; scx++)
					for(int scz = 0; scz < 8; scz++)
					{
						int details = details2use[(scx+1)*10+(scz+1)];
						int cellSize = (int) Math.pow(2, details);
						
						int x0 = (scx * 32) / cellSize;
						int y0 = (scz * 32) / cellSize;
						HeightmapMeshSummarizer mesher = new HeightmapMeshSummarizer(heightMap, offsets[details], 32 / cellSize,
								x0, y0, 256 / cellSize);
						int test = 0;
						Surface surf = mesher.nextSurface();
						while(surf != null)
						{
							//Top
							addVertexBytes(regionMeshBuffer, scx*32+(surf.getX()              ) * cellSize, surf.getLevel(), scz*32+(surf.getY()              ) * cellSize, 0, 1, 0);
							addVertexBytes(regionMeshBuffer, scx*32+(surf.getX() + surf.getW()) * cellSize, surf.getLevel(), scz*32+(surf.getY() + surf.getH()) * cellSize, 0, 1, 0);
							addVertexBytes(regionMeshBuffer, scx*32+(surf.getX() + surf.getW()) * cellSize, surf.getLevel(), scz*32+(surf.getY()              ) * cellSize, 0, 1, 0);
							
							addVertexBytes(regionMeshBuffer, scx*32+(surf.getX()              ) * cellSize, surf.getLevel(), scz*32+(surf.getY()              ) * cellSize, 0, 1, 0);
							addVertexBytes(regionMeshBuffer, scx*32+(surf.getX()              ) * cellSize, surf.getLevel(), scz*32+(surf.getY() + surf.getH()) * cellSize, 0, 1, 0);
							addVertexBytes(regionMeshBuffer, scx*32+(surf.getX() + surf.getW()) * cellSize, surf.getLevel(), scz*32+(surf.getY() + surf.getH()) * cellSize, 0, 1, 0);
							
							vertexCount+=6;
							
							//Left side
							int vx = scx*32+(surf.getX()              ) * cellSize;
							int vz = scz*32+(surf.getY()              ) * cellSize;
							int heightCurrent = getHeight(heightMap, world, vx-cellSize, vz, currentRegionX, currentRegionZ, details2use[((int)Math.floor((vx-cellSize)/32f)+1)*10+(scz+1)]);
							int d = 0;
							for(int i = 1; i < surf.getH()+1; i++)
							{
								int newHeight = (i < surf.getH()) ? getHeight(heightMap, world, vx-cellSize, vz + i*cellSize, currentRegionX, currentRegionZ, details2use[((int)Math.floor((vx-cellSize)/32f)+1)*10+((int)Math.floor((vz+(i)*cellSize)/32f)+1)]) : -1;
								if(newHeight != heightCurrent)
								{
									if(heightCurrent != surf.getLevel())
									{
										int s = heightCurrent > surf.getLevel() ? 1 : -1;
										addVertexBytes(regionMeshBuffer, vx, surf.getLevel(), vz + d * cellSize, s, 0, 0);
										addVertexBytes(regionMeshBuffer, vx, heightCurrent, vz + d * cellSize, s, 0, 0);
										addVertexBytes(regionMeshBuffer, vx, heightCurrent, vz + (i) * cellSize, s, 0, 0);
										
										addVertexBytes(regionMeshBuffer, vx, surf.getLevel(), vz + d * cellSize, s, 0, 0);
										addVertexBytes(regionMeshBuffer, vx, heightCurrent, vz + (i) * cellSize, s, 0, 0);
										addVertexBytes(regionMeshBuffer, vx, surf.getLevel(), vz + (i) *cellSize, s, 0, 0);
										vertexCount+=6;
									}
									heightCurrent = newHeight;
									d = i;
								}
							}
							//Bot side
							heightCurrent = getHeight(heightMap, world, vx, vz-cellSize, currentRegionX, currentRegionZ, details2use[((int)Math.floor((vx)/32f)+1)*10+((int)Math.floor((vz-cellSize)/32f)+1)]);
							d = 0;
							for(int i = 1; i < surf.getW()+1; i++)
							{
								int newHeight = (i < surf.getW()) ? getHeight(heightMap, world, vx + i*cellSize, vz - cellSize, currentRegionX, currentRegionZ, details2use[((int)Math.floor((vx+i*cellSize)/32f)+1)*10+((int)Math.floor((vz-cellSize)/32f)+1)]) : -1;
								if(newHeight != heightCurrent)
								{
									if(heightCurrent != surf.getLevel())
									{
										int s = heightCurrent > surf.getLevel() ? 1 : -1;
										addVertexBytes(regionMeshBuffer, vx + d * cellSize, surf.getLevel(), vz, 0, 0, s);
										addVertexBytes(regionMeshBuffer, vx + (i) * cellSize, heightCurrent, vz, 0, 0, s);
										addVertexBytes(regionMeshBuffer, vx + d * cellSize, heightCurrent, vz, 0, 0, s);

										addVertexBytes(regionMeshBuffer, vx + (i) * cellSize, heightCurrent, vz, 0, 0, s);
										addVertexBytes(regionMeshBuffer, vx + d * cellSize, surf.getLevel(), vz, 0, 0, s);
										addVertexBytes(regionMeshBuffer, vx + (i) * cellSize, surf.getLevel(), vz, 0, 0, s);
										vertexCount+=6;
									}
									heightCurrent = newHeight;
									d = i;
								}
							}
							
							//Next
							surf = mesher.nextSurface();
							test++;
						}
						if(test > 32* 32 / (cellSize * cellSize))
						{
							System.out.println("Meshing made more than reasonnable vertices");
						}
						
						/*for(int vx = scx*32; vx < scx*32+32; vx += cellSize)
							for(int vz = scz*32; vz < scz*32+32; vz += cellSize)
							{
								int height = getHeight(heightMap, world, vx, vz, currentRegionX, currentRegionZ, details);
								int heightXM = getHeight(heightMap, world, vx-cellSize, vz, currentRegionX, currentRegionZ, details2use[((int)Math.floor((vx-cellSize)/32f)+1)*10+(scz+1)]);
								int heightZM = getHeight(heightMap, world, vx, vz-cellSize, currentRegionX, currentRegionZ, details2use[(scx+1)*10+((int)Math.floor((vz-cellSize)/32f)+1)]);
								
								//Unused
								//int heightZP = getHeight(heightMap, world, vx, vz+cellSize, currentRegionX, currentRegionZ, detail);
								//int heightXP = getHeight(heightMap, world, vx+cellSize, vz, currentRegionX, currentRegionZ, detail);
								
								addVertexShort(regionMeshBuffer, vx, height, vz, 0, 1, 0);
								addVertexShort(regionMeshBuffer, vx + cellSize, height, vz, 0, 1, 0);
								addVertexShort(regionMeshBuffer, vx + cellSize, height, vz + cellSize, 0, 1, 0);

								addVertexShort(regionMeshBuffer, vx, height, vz, 0, 1, 0);
								addVertexShort(regionMeshBuffer, vx + cellSize, height, vz + cellSize, 0, 1, 0);
								addVertexShort(regionMeshBuffer, vx, height, vz + cellSize, 0, 1, 0);
								vertexCount+=6;
								
								if(heightXM != height)
								{
									addVertexBytes(regionMeshBuffer, vx, height, vz, 1, 0, 0);
									addVertexBytes(regionMeshBuffer, vx, heightXM, vz, 1, 0, 0);
									addVertexBytes(regionMeshBuffer, vx, heightXM, vz + cellSize, 1, 0, 0);
									
									addVertexBytes(regionMeshBuffer, vx, height, vz, 1, 0, 0);
									addVertexBytes(regionMeshBuffer, vx, heightXM, vz + cellSize, 1, 0, 0);
									addVertexBytes(regionMeshBuffer, vx, height, vz + cellSize, 1, 0, 0);
									vertexCount+=6;
								}
								
								if(heightZM != height)
								{
									addVertexBytes(regionMeshBuffer, vx, height, vz, 0, 0, 1);
									addVertexBytes(regionMeshBuffer, vx + cellSize, heightZM, vz, 0, 0, 1);
									addVertexBytes(regionMeshBuffer, vx, heightZM, vz, 0, 0, 1);
									
									addVertexBytes(regionMeshBuffer, vx, height, vz, 0, 0, 1);
									addVertexBytes(regionMeshBuffer, vx + cellSize, height, vz, 0, 0, 1);
									addVertexBytes(regionMeshBuffer, vx + cellSize, heightZM, vz, 0, 0, 1);
									vertexCount+=6;
								}
							}
						*/
						//If the next side has a coarser resolution we want to fill in the gaps
						//We go alongside the two other sides of the mesh and we add another skirt to match the coarser mesh on the side
						int nextMeshDetailsX = details2use[(scx+2)*10+(scz+1)];
						if(nextMeshDetailsX > details)
						{
							int vx = scx*32 + 32;
							for(int vz = scz*32; vz < scz*32+32; vz += cellSize)
							{

								int height = getHeight(heightMap, world, vx-1, vz, currentRegionX, currentRegionZ, details);
								int heightNext = getHeight(heightMap, world, vx+1, vz, currentRegionX, currentRegionZ, nextMeshDetailsX);
								
								if(heightNext > height)
								{
									addVertexBytes(regionMeshBuffer, vx, height, vz, 1, 0, 0);
									addVertexBytes(regionMeshBuffer, vx, heightNext, vz + cellSize, 1, 0, 0);
									addVertexBytes(regionMeshBuffer, vx, heightNext, vz, 1, 0, 0);
									
									addVertexBytes(regionMeshBuffer, vx, height, vz, 1, 0, 0);
									addVertexBytes(regionMeshBuffer, vx, height, vz + cellSize, 1, 0, 0);
									addVertexBytes(regionMeshBuffer, vx, heightNext, vz + cellSize, 1, 0, 0);
									vertexCount+=6;
								}
								else if(heightNext < height)
								{
									addVertexBytes(regionMeshBuffer, vx, height, vz, -1, 0, 0);
									addVertexBytes(regionMeshBuffer, vx, heightNext, vz, -1, 0, 0);
									addVertexBytes(regionMeshBuffer, vx, heightNext, vz + cellSize, -1, 0, 0);
									
									addVertexBytes(regionMeshBuffer, vx, height, vz, -1, 0, 0);
									addVertexBytes(regionMeshBuffer, vx, heightNext, vz + cellSize, -1, 0, 0);
									addVertexBytes(regionMeshBuffer, vx, height, vz + cellSize, -1, 0, 0);
									vertexCount+=6;
								}
							}
						}
						
						int nextMeshDetailsZ = details2use[(scx+1)*10+(scz+2)];
						if(nextMeshDetailsZ > details)
						{
							int vz = scz*32 + 32;
							for(int vx = scx*32; vx < scx*32+32; vx += cellSize)
							{
								int height = getHeight(heightMap, world, vx, vz-1, currentRegionX, currentRegionZ, details);
								int heightNext = getHeight(heightMap, world, vx, vz+1, currentRegionX, currentRegionZ, nextMeshDetailsZ);
								
								if(heightNext > height)
								{
									addVertexBytes(regionMeshBuffer, vx, height, vz, 0, 0, 1);
									addVertexBytes(regionMeshBuffer, vx, heightNext, vz, 0, 0, 1);
									addVertexBytes(regionMeshBuffer, vx + cellSize, heightNext, vz, 0, 0, 1);
									
									addVertexBytes(regionMeshBuffer, vx, height, vz, 0, 0, 1);
									addVertexBytes(regionMeshBuffer, vx + cellSize, heightNext, vz, 0, 0, 1);
									addVertexBytes(regionMeshBuffer, vx + cellSize, height, vz, 0, 0, 1);
									vertexCount+=6;
								}
								else if(heightNext < height)
								{
									addVertexBytes(regionMeshBuffer, vx, height, vz, 0, 0, -1);
									addVertexBytes(regionMeshBuffer, vx + cellSize, heightNext, vz, 0, 0, -1);
									addVertexBytes(regionMeshBuffer, vx, heightNext, vz, 0, 0, -1);
									
									addVertexBytes(regionMeshBuffer, vx, height, vz, 0, 0, -1);
									addVertexBytes(regionMeshBuffer, vx + cellSize, height, vz, 0, 0, -1);
									addVertexBytes(regionMeshBuffer, vx + cellSize, heightNext, vz, 0, 0, -1);
									vertexCount+=6;
								}
							}
						}
						
					}
				
				//System.out.println("vc:" + vertexCount);
				
				glBindBuffer(GL_ARRAY_BUFFER, regionMesh.vbo);
				regionMeshBuffer.flip();
				glBufferData(GL_ARRAY_BUFFER, regionMeshBuffer, GL_DYNAMIC_DRAW);
				regionMesh.vboSize = vertexCount;
				
				lastRegionX = rcx / 8;
				lastRegionZ = rcz / 8;
				
				regionsToRender.add(regionMesh);
				
				currentChunkZ = nextChunkZ;
			}
			
			currentChunkX = nextChunkX;
		}

		lastLevelDetail = -1;
		lastRegionX = -1;
		lastRegionZ = -1;

		lastGen = time;
	}
	
	private int getHeight(int[] heightMap, World world, int x, int z, int rx, int rz, int level)
	{
		if(x < 0 || z < 0 || x >= 256 || z >= 256)
			return world.regionSummaries.getHeightMipmapped(rx*256+x, rz*256+z, level);
		else
			return getHeightMipmapped(heightMap, x, z, level);
	}

	static int[] offsets = {0, 65536, 81920, 86016, 87040, 87296, 87360, 87376, 87380, 87381};
	
	public int getHeightMipmapped(int[] heightMap, int x, int z, int level)
	{
		if(level > 8)
			return -1;
		int resolution = 256 >> level;
		x >>= level;
		z >>= level;
		int offset = offsets[level];
		//System.out.println(level+"l"+offset+"reso"+resolution+"x:"+x+"z:"+z);
		return heightMap[offset + resolution * x + z];
	}
	
	public void updateData()
	{
		for(RegionMesh rs : regionsToRender)
		{
			boolean generated = rs.regionSummary.uploadTextures();
			if(generated)
			{
				//System.out.println("generated RS texture "+ rs.dataSource.hId);
			}
			//System.out.println(rs.dataSource.loaded.get());
			if(!rs.regionSummary.loaded.get())
				rs.regionSummary = world.regionSummaries.get(rs.regionSummary.rx * 256, rs.regionSummary.rz * 256);
		}
	}

	class RegionMesh
	{
		public RegionMesh(int rxDisplay, int rzDisplay, RegionSummary dataSource)
		{
			
			this.rxDisplay = rxDisplay;
			this.rzDisplay = rzDisplay;
			this.regionSummary = dataSource;
			//fbId = fbPool.requestFloatBuffer();
			vbo = glGenBuffers();
		}

		int rxDisplay, rzDisplay;
		RegionSummary regionSummary;
		//int fbId;
		int vbo, vboSize;

		public void delete()
		{
			//fbPool.releaseFloatBuffer(fbId);
			glDeleteBuffers(vbo);
		}
	}
	
	int kek = 0;
	
	private void addVertexBytes(ByteBuffer terrain, int x, int y, int z, int nx, int ny, int nz)
	{
			terrain.putShort((short) x);
			terrain.putShort((short) y);
			terrain.putShort((short) z);
			terrain.putShort((short) 0x00);
	
			terrain.put((byte)(nx+1));
			terrain.put((byte)(ny+1));
			terrain.put((byte)(nz+1));
			terrain.put((byte)(0x00));
	}

	public void destroy()
	{
		glDeleteTextures(blocksTexturesSummaryId);
	}
}
