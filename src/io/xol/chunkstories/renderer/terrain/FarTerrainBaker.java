package io.xol.chunkstories.renderer.terrain;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.BufferUtils;

import io.xol.chunkstories.client.RenderingConfig;
import io.xol.chunkstories.renderer.terrain.HeightmapMesher.Surface;
import io.xol.chunkstories.world.WorldClientCommon;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.summary.RegionSummaryImplementation;
import io.xol.engine.graphics.geometry.VerticesObject;

public class FarTerrainBaker
{
	public FarTerrainBaker(ByteBuffer regionMeshBuffer, WorldClientCommon world, int cameraChunkX, int cameraChunkZ)
	{
		super();
		this.regionMeshBuffer = regionMeshBuffer;
		this.world = world;
		this.cameraChunkX = cameraChunkX;
		this.cameraChunkZ = cameraChunkZ;
	}

	ByteBuffer regionMeshBuffer;
	WorldClientCommon world;
	int cameraChunkX, cameraChunkZ;
	
	private static final int[] offsets = { 0, 65536, 81920, 86016, 87040, 87296, 87360, 87376, 87380, 87381 };
	
	public List<RegionMesh> generateArround()
	{
		List<FarTerrainBaker.RegionMesh> regionsToRender_NewList = new ArrayList<FarTerrainBaker.RegionMesh>();
		int summaryDistance = 32;

		System.out.println("----");
			//Iterate over X chunks but skip whole regions
			int currentChunkX = cameraChunkX - summaryDistance;
			while (currentChunkX < cameraChunkX + summaryDistance)
			{
				//Computes where are we
				int currentRegionX = (int) Math.floor(currentChunkX / 8f);
				//if(currentChunkX < 0)
				//	currentRegionX--;
				System.out.println(currentChunkX + " : " + currentRegionX);
				int nextRegionX = currentRegionX + 1;
				int nextChunkX = nextRegionX * 8;

				//Iterate over Z chunks but skip whole regions
				int currentChunkZ = cameraChunkZ - summaryDistance;
				while (currentChunkZ < cameraChunkZ + summaryDistance)
				{
					//Computes where are we
					int currentRegionZ = (int) Math.floor(currentChunkZ / 8f);
					int nextRegionZ = currentRegionZ + 1;
					int nextChunkZ = nextRegionZ * 8;

					//Clear shit
					regionMeshBuffer.clear();

					int rx = currentChunkX / 8;
					int rz = currentChunkZ / 8;
					if (currentChunkZ < 0 && currentChunkZ % 8 != 0)
						rz--;
					if (currentChunkX < 0 && currentChunkX % 8 != 0)
						rx--;

					RegionSummaryImplementation summary = world.getRegionsSummariesHolder().getRegionSummaryWorldCoordinates(currentChunkX * 32, currentChunkZ * 32);

					if (summary == null || !summary.isLoaded())
					{
						currentChunkZ = nextChunkZ;
						continue;
					}

					int rcx = currentChunkX % world.getSizeInChunks();
					if (rcx < 0)
						rcx += world.getSizeInChunks();
					int rcz = currentChunkZ % world.getSizeInChunks();
					if (rcz < 0)
						rcz += world.getSizeInChunks();

					int[] heightMap = summary.getHeightData();
					int[] ids = summary.getVoxelData();

					@SuppressWarnings("unused")
					int vertexCount = 0;

					//Compute the LODs for every subchunk of the region, plus borders
					int[] lodsArray = new int[100];
					for (int scx = -1; scx < 9; scx++)
						for (int scz = -1; scz < 9; scz++)
						{
							int regionMiddleX = currentRegionX * 8 + scx;
							int regionMiddleZ = currentRegionZ * 8 + scz;
							int detail = (int) (Math.sqrt(Math.abs(regionMiddleX - cameraChunkX) * Math.abs(regionMiddleX - cameraChunkX) + Math.abs(regionMiddleZ - cameraChunkZ) * Math.abs(regionMiddleZ - cameraChunkZ))
									/ (RenderingConfig.hqTerrain ? 6f : 4f));

							if (detail > 5)
								detail = 5;

							if (!RenderingConfig.hqTerrain && detail < 2)
								detail = 2;

							lodsArray[(scx + 1) * 10 + (scz + 1)] = detail;
						}

					for (int scx = 0; scx < 8; scx++)
						for (int scz = 0; scz < 8; scz++)
						{
							int chunkLod = lodsArray[(scx + 1) * 10 + (scz + 1)];
							int cellSize = (int) Math.pow(2, chunkLod);

							int x0 = (scx * 32) / cellSize;
							int y0 = (scz * 32) / cellSize;
							HeightmapMesher mesher = new HeightmapMesher(heightMap, ids, offsets[chunkLod], 32 / cellSize, x0, y0, 256 / cellSize);
							int test = 0;
							Surface surf = mesher.nextSurface();
							while (surf != null)
							{
								//Top
								addVertexBytes(regionMeshBuffer, scx * 32 + (surf.getX()) * cellSize, surf.getLevel(), scz * 32 + (surf.getY()) * cellSize, 0, 1, 0, surf.getId());
								addVertexBytes(regionMeshBuffer, scx * 32 + (surf.getX() + surf.getW()) * cellSize, surf.getLevel(), scz * 32 + (surf.getY() + surf.getH()) * cellSize, 0, 1, 0, surf.getId());
								addVertexBytes(regionMeshBuffer, scx * 32 + (surf.getX() + surf.getW()) * cellSize, surf.getLevel(), scz * 32 + (surf.getY()) * cellSize, 0, 1, 0, surf.getId());

								addVertexBytes(regionMeshBuffer, scx * 32 + (surf.getX()) * cellSize, surf.getLevel(), scz * 32 + (surf.getY()) * cellSize, 0, 1, 0, surf.getId());
								addVertexBytes(regionMeshBuffer, scx * 32 + (surf.getX()) * cellSize, surf.getLevel(), scz * 32 + (surf.getY() + surf.getH()) * cellSize, 0, 1, 0, surf.getId());
								addVertexBytes(regionMeshBuffer, scx * 32 + (surf.getX() + surf.getW()) * cellSize, surf.getLevel(), scz * 32 + (surf.getY() + surf.getH()) * cellSize, 0, 1, 0, surf.getId());

								vertexCount += 6;

								//Left side
								int vx = scx * 32 + (surf.getX()) * cellSize;
								int vz = scz * 32 + (surf.getY()) * cellSize;
								int heightCurrent = getHeight(heightMap, world, vx - cellSize, vz, currentRegionX, currentRegionZ, lodsArray[((int) Math.floor((vx - cellSize) / 32f) + 1) * 10 + (scz + 1)]);
								int d = 0;
								for (int i = 1; i < surf.getH() + 1; i++)
								{
									int newHeight = (i < surf.getH()) ? getHeight(heightMap, world, vx - cellSize, vz + i * cellSize, currentRegionX, currentRegionZ,
											lodsArray[((int) Math.floor((vx - cellSize) / 32f) + 1) * 10 + ((int) Math.floor((vz + (i) * cellSize) / 32f) + 1)]) : -1;
									if (newHeight != heightCurrent)
									{
										if (heightCurrent != surf.getLevel())
										{
											int side = heightCurrent > surf.getLevel() ? 1 : -1;
											addVertexBytes(regionMeshBuffer, vx, surf.getLevel(), vz + d * cellSize, side, 0, 0, surf.getId());
											addVertexBytes(regionMeshBuffer, vx, heightCurrent, vz + d * cellSize, side, 0, 0, surf.getId());
											addVertexBytes(regionMeshBuffer, vx, heightCurrent, vz + (i) * cellSize, side, 0, 0, surf.getId());

											addVertexBytes(regionMeshBuffer, vx, surf.getLevel(), vz + d * cellSize, side, 0, 0, surf.getId());
											addVertexBytes(regionMeshBuffer, vx, heightCurrent, vz + (i) * cellSize, side, 0, 0, surf.getId());
											addVertexBytes(regionMeshBuffer, vx, surf.getLevel(), vz + (i) * cellSize, side, 0, 0, surf.getId());
											vertexCount += 6;
										}
										heightCurrent = newHeight;
										d = i;
									}
								}
								//Bot side
								heightCurrent = getHeight(heightMap, world, vx, vz - cellSize, currentRegionX, currentRegionZ, lodsArray[((int) Math.floor((vx) / 32f) + 1) * 10 + ((int) Math.floor((vz - cellSize) / 32f) + 1)]);
								d = 0;
								for (int i = 1; i < surf.getW() + 1; i++)
								{
									int newHeight = (i < surf.getW()) ? getHeight(heightMap, world, vx + i * cellSize, vz - cellSize, currentRegionX, currentRegionZ,
											lodsArray[((int) Math.floor((vx + i * cellSize) / 32f) + 1) * 10 + ((int) Math.floor((vz - cellSize) / 32f) + 1)]) : -1;
									if (newHeight != heightCurrent)
									{
										if (heightCurrent != surf.getLevel())
										{
											int side = heightCurrent > surf.getLevel() ? 1 : -1;
											addVertexBytes(regionMeshBuffer, vx + d * cellSize, surf.getLevel(), vz, 0, 0, side, surf.getId());
											addVertexBytes(regionMeshBuffer, vx + (i) * cellSize, heightCurrent, vz, 0, 0, side, surf.getId());
											addVertexBytes(regionMeshBuffer, vx + d * cellSize, heightCurrent, vz, 0, 0, side, surf.getId());

											addVertexBytes(regionMeshBuffer, vx + (i) * cellSize, heightCurrent, vz, 0, 0, side, surf.getId());
											addVertexBytes(regionMeshBuffer, vx + d * cellSize, surf.getLevel(), vz, 0, 0, side, surf.getId());
											addVertexBytes(regionMeshBuffer, vx + (i) * cellSize, surf.getLevel(), vz, 0, 0, side, surf.getId());
											vertexCount += 6;
										}
										heightCurrent = newHeight;
										d = i;
									}
								}

								//Next
								surf = mesher.nextSurface();
								test++;
							}
							if (test > 32 * 32 / (cellSize * cellSize))
							{
								System.out.println("Meshing made more than reasonnable vertices");
							}
							//If the next side has a coarser resolution we want to fill in the gaps
							//We go alongside the two other sides of the mesh and we add another skirt to match the coarser mesh on the side
							int nextMeshDetailsX = lodsArray[(scx + 2) * 10 + (scz + 1)];
							if (nextMeshDetailsX > chunkLod)
							{
								int vx = scx * 32 + 32;
								for (int vz = scz * 32; vz < scz * 32 + 32; vz += cellSize)
								{

									int height = getHeight(heightMap, world, vx - 1, vz, currentRegionX, currentRegionZ, chunkLod);
									int heightNext = getHeight(heightMap, world, vx + 1, vz, currentRegionX, currentRegionZ, nextMeshDetailsX);

									if (heightNext > height)
									{
										int gapData = getIds(ids, world, vx - 1, vz, currentRegionX, currentRegionZ, chunkLod);

										addVertexBytes(regionMeshBuffer, vx, height, vz, 1, 0, 0, gapData);
										addVertexBytes(regionMeshBuffer, vx, heightNext, vz + cellSize, 1, 0, 0, gapData);
										addVertexBytes(regionMeshBuffer, vx, heightNext, vz, 1, 0, 0, gapData);

										addVertexBytes(regionMeshBuffer, vx, height, vz, 1, 0, 0, gapData);
										addVertexBytes(regionMeshBuffer, vx, height, vz + cellSize, 1, 0, 0, gapData);
										addVertexBytes(regionMeshBuffer, vx, heightNext, vz + cellSize, 1, 0, 0, gapData);
										vertexCount += 6;
									}
									else if (heightNext < height)
									{
										int gapData = getIds(ids, world, vx + 1, vz, currentRegionX, currentRegionZ, chunkLod);

										addVertexBytes(regionMeshBuffer, vx, height, vz, -1, 0, 0, gapData);
										addVertexBytes(regionMeshBuffer, vx, heightNext, vz, -1, 0, 0, gapData);
										addVertexBytes(regionMeshBuffer, vx, heightNext, vz + cellSize, -1, 0, 0, gapData);

										addVertexBytes(regionMeshBuffer, vx, height, vz, -1, 0, 0, gapData);
										addVertexBytes(regionMeshBuffer, vx, heightNext, vz + cellSize, -1, 0, 0, gapData);
										addVertexBytes(regionMeshBuffer, vx, height, vz + cellSize, -1, 0, 0, gapData);
										vertexCount += 6;
									}
								}
							}

							int nextMeshDetailsZ = lodsArray[(scx + 1) * 10 + (scz + 2)];
							if (nextMeshDetailsZ > chunkLod)
							{
								int vz = scz * 32 + 32;
								for (int vx = scx * 32; vx < scx * 32 + 32; vx += cellSize)
								{
									int height = getHeight(heightMap, world, vx, vz - 1, currentRegionX, currentRegionZ, chunkLod);
									int heightNext = getHeight(heightMap, world, vx, vz + 1, currentRegionX, currentRegionZ, nextMeshDetailsZ);

									if (heightNext > height)
									{
										int gapData = getIds(heightMap, world, vx, vz - 1, currentRegionX, currentRegionZ, nextMeshDetailsZ);

										addVertexBytes(regionMeshBuffer, vx, height, vz, 0, 0, 1, gapData);
										addVertexBytes(regionMeshBuffer, vx, heightNext, vz, 0, 0, 1, gapData);
										addVertexBytes(regionMeshBuffer, vx + cellSize, heightNext, vz, 0, 0, 1, gapData);

										addVertexBytes(regionMeshBuffer, vx, height, vz, 0, 0, 1, gapData);
										addVertexBytes(regionMeshBuffer, vx + cellSize, heightNext, vz, 0, 0, 1, gapData);
										addVertexBytes(regionMeshBuffer, vx + cellSize, height, vz, 0, 0, 1, gapData);
										vertexCount += 6;
									}
									else if (heightNext < height)
									{
										int gapData = getIds(heightMap, world, vx, vz + 1, currentRegionX, currentRegionZ, nextMeshDetailsZ);

										addVertexBytes(regionMeshBuffer, vx, height, vz, 0, 0, -1, gapData);
										addVertexBytes(regionMeshBuffer, vx + cellSize, heightNext, vz, 0, 0, -1, gapData);
										addVertexBytes(regionMeshBuffer, vx, heightNext, vz, 0, 0, -1, gapData);

										addVertexBytes(regionMeshBuffer, vx, height, vz, 0, 0, -1, gapData);
										addVertexBytes(regionMeshBuffer, vx + cellSize, height, vz, 0, 0, -1, gapData);
										addVertexBytes(regionMeshBuffer, vx + cellSize, heightNext, vz, 0, 0, -1, gapData);
										vertexCount += 6;
									}
								}
							}

						}


					byte[] vboContent = new byte[regionMeshBuffer.position()];
					regionMeshBuffer.flip();
					regionMeshBuffer.get(vboContent);
					
					FarTerrainBaker.RegionMesh regionMesh = new FarTerrainBaker.RegionMesh(rx, rz, summary, vboContent);

					regionsToRender_NewList.add(regionMesh);

					currentChunkZ = nextChunkZ;
				}

				currentChunkX = nextChunkX;
			}
			
			return  regionsToRender_NewList;
		
	}

	private int getHeight(int[] heightMap, WorldImplementation world, int x, int z, int rx, int rz, int level)
	{
		if (x < 0 || z < 0 || x >= 256 || z >= 256)
			return world.getRegionsSummariesHolder().getHeightMipmapped(rx * 256 + x, rz * 256 + z, level);
		else
			return getDataMipmapped(heightMap, x, z, level);
	}

	private int getIds(int[] ids, WorldImplementation world, int x, int z, int rx, int rz, int level)
	{
		if (x < 0 || z < 0 || x >= 256 || z >= 256)
			return world.getRegionsSummariesHolder().getDataMipmapped(rx * 256 + x, rz * 256 + z, level);
		else
			return getDataMipmapped(ids, x, z, level);
	}
	
	private int getDataMipmapped(int[] summaryData, int x, int z, int level)
	{
		if (level > 8)
			return -1;
		int resolution = 256 >> level;
		x >>= level;
		z >>= level;
		int offset = offsets[level];
		//System.out.println(level+"l"+offset+"reso"+resolution+"x:"+x+"z:"+z);
		return summaryData[offset + resolution * x + z];
	}
	
	private void addVertexBytes(ByteBuffer terrain, int x, int y, int z, int nx, int ny, int nz, int voxelData)
	{
		terrain.putShort((short) x);
		terrain.putShort((short) (y + 1));
		terrain.putShort((short) z);
		terrain.putShort((short) 0x00);

		terrain.put((byte) (nx + 1));
		terrain.put((byte) (ny + 1));
		terrain.put((byte) (nz + 1));
		terrain.put((byte) (0x00));
	}
	
	public static class RegionMesh
	{
		int regionDisplayedX, regionDisplayedZ;
		RegionSummaryImplementation regionSummary;
	
		//Mesh (client renderer)
		VerticesObject verticesObject;
	
		public RegionMesh(int rxDisplay, int rzDisplay, RegionSummaryImplementation dataSource, byte[] vboContent)
		{
			this.regionDisplayedX = rxDisplay;
			this.regionDisplayedZ = rzDisplay;
			this.regionSummary = dataSource;
			
			this.verticesObject = new VerticesObject();
			
			ByteBuffer byteBuffer = BufferUtils.createByteBuffer(vboContent.length);
			byteBuffer.put(vboContent);
			byteBuffer.flip();
			verticesObject.uploadData(byteBuffer);
		}
	
		public void delete()
		{
			//glDeleteBuffers(regionSummary.vboId);
			verticesObject.destroy();
		}
	}

}
