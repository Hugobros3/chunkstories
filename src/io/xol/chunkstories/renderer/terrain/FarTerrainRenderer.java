package io.xol.chunkstories.renderer.terrain;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.geometry.VertexFormat;
import io.xol.engine.graphics.textures.Texture1D;
import io.xol.engine.graphics.textures.TextureFormat;
import io.xol.engine.math.lalgb.Vector3f;

import org.lwjgl.BufferUtils;

import io.xol.chunkstories.Constants;
import io.xol.chunkstories.api.rendering.Primitive;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.CullingMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.DepthTestMode;
import io.xol.chunkstories.client.RenderingConfig;
import io.xol.chunkstories.renderer.Camera;
import io.xol.chunkstories.renderer.terrain.HeightmapMeshSummarizer.Surface;
import io.xol.chunkstories.voxel.VoxelTexture;
import io.xol.chunkstories.voxel.VoxelTextures;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.summary.RegionSummaryImplementation;
import static org.lwjgl.opengl.GL11.*;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class FarTerrainRenderer
{
	private static final int TRIANGLES_PER_FACE = 2; // 2 triangles per face
	private static final int TRIANGLE_SIZE = 3; // 3 vertex per triangles
	private static final int VERTEX_SIZE = 3; // A vertex is 3 coordinates : xyz

	private static int[] offsets = { 0, 65536, 81920, 86016, 87040, 87296, 87360, 87376, 87380, 87381 };

	//Single 6Mb Buffer
	private ByteBuffer regionMeshBuffer = BufferUtils.createByteBuffer(256 * 256 * 5 * TRIANGLE_SIZE * VERTEX_SIZE * TRIANGLES_PER_FACE * (8 + 4));

	private WorldImplementation world;

	private List<RegionMesh> regionsToRender = new ArrayList<RegionMesh>();

	//TODO use a texture
	private boolean blocksTexturesSummaryDone = false;
	private Texture1D blockTexturesSummary = new Texture1D(TextureFormat.RGBA_8BPP);
	//private int blocksTexturesSummaryId = -1;

	@SuppressWarnings("unused")
	private int lastRegionX = -1;
	@SuppressWarnings("unused")
	private int lastRegionZ = -1;

	@SuppressWarnings("unused")
	private int lastLevelDetail = -1;

	private int cameraChunkX, cameraChunkZ;

	private AtomicBoolean isTerrainUpdateRunning = new AtomicBoolean();
	private AtomicInteger farTerrainUpdatesToTakeIntoAccount = new AtomicInteger();
	private long lastTerrainUpdateTiming;
	private long timeToWaitBetweenTerrainUpdates = 2500;

	public FarTerrainRenderer(WorldImplementation world)
	{
		this.world = world;
		getBlocksTexturesSummary();
	}

	public void markFarTerrainMeshDirty()
	{
		farTerrainUpdatesToTakeIntoAccount.incrementAndGet();
		//terrainDirty = true;
	}

	public void markVoxelTexturesSummaryDirty()
	{
		blocksTexturesSummaryDone = false;
	}

	public Texture1D getBlocksTexturesSummary()
	{
		if (!blocksTexturesSummaryDone)
		{
			int size = 512;
			ByteBuffer bb = ByteBuffer.allocateDirect(size * 4);
			bb.order(ByteOrder.LITTLE_ENDIAN);

			int counter = 0;
			Iterator<VoxelTexture> i = VoxelTextures.getAllVoxelTextures();
			while (i.hasNext() && counter < size)
			{
				VoxelTexture voxelTexture = i.next();

				bb.put((byte) (voxelTexture.color.x * 255));
				bb.put((byte) (voxelTexture.color.y * 255));
				bb.put((byte) (voxelTexture.color.z * 255));
				bb.put((byte) (voxelTexture.color.w * 255));

				voxelTexture.positionInColorIndex = counter;
				counter++;
			}

			//Padding
			while (counter < size)
			{
				bb.put((byte) (0));
				bb.put((byte) (0));
				bb.put((byte) (0));
				bb.put((byte) (0));
				counter++;
			}

			bb.flip();

			blockTexturesSummary.uploadTextureData(size, bb);
			blockTexturesSummary.setLinearFiltering(false);

			blocksTexturesSummaryDone = true;
		}

		return blockTexturesSummary;
	}

	public int draw(RenderingContext renderingContext, ShaderInterface terrainShader)
	{
		if (!this.isTerrainUpdateRunning.get() && farTerrainUpdatesToTakeIntoAccount.get() > 0 && (System.currentTimeMillis() - this.lastTerrainUpdateTiming) > this.timeToWaitBetweenTerrainUpdates)
		{
			this.isTerrainUpdateRunning.set(true);
			this.startAsynchSummaryRegeneration(renderingContext.getCamera());
		}

		int elements = 0;
		renderingContext.setCullingMode(CullingMode.COUNTERCLOCKWISE);
		renderingContext.setDepthTestMode(DepthTestMode.LESS_OR_EQUAL);
		//glEnable(GL_CULL_FACE); // culling for our glorious terrain
		glLineWidth(1.0f);

		//Sort to draw near first
		List<RegionMesh> regionsMeshesToRenderSorted = new ArrayList<RegionMesh>(regionsToRender);
		//renderingContext.getCamera().getLocation;
		Camera camera = renderingContext.getCamera();
		int camRX = (int) (-camera.pos.getX() / 256);
		int camRZ = (int) (-camera.pos.getZ() / 256);

		regionsMeshesToRenderSorted.sort(new Comparator<RegionMesh>()
		{

			@Override
			public int compare(RegionMesh a, RegionMesh b)
			{
				int distanceA = Math.abs(a.regionDisplayedX - camRX) + Math.abs(a.regionDisplayedZ - camRZ);
				//System.out.println(camRX + " : " + distanceA);
				int distanceB = Math.abs(b.regionDisplayedX - camRX) + Math.abs(b.regionDisplayedZ - camRZ);
				return distanceA - distanceB;
			}

		});

		try
		{
			for (RegionMesh regionMesh : regionsMeshesToRenderSorted)
			{
				float height = 1024f;
				if (!renderingContext.getCamera().isBoxInFrustrum(new Vector3f(regionMesh.regionDisplayedX * 256 + 128, height / 2, regionMesh.regionDisplayedZ * 256 + 128), new Vector3f(256, height, 256)))
					continue;
				
				renderingContext.bindTexture2D("groundTexture", regionMesh.regionSummary.voxelTypesTexture);
				renderingContext.bindTexture2D("heightMap", regionMesh.regionSummary.heightsTexture);
				renderingContext.bindTexture1D("blocksTexturesSummary", getBlocksTexturesSummary());
				terrainShader.setUniform2f("regionPosition", regionMesh.regionSummary.getRegionX(), regionMesh.regionSummary.getRegionZ());
				terrainShader.setUniform2f("chunkPosition", regionMesh.regionDisplayedX * 256, regionMesh.regionDisplayedZ * 256);

				if (regionMesh.regionSummary.verticesObject.isDataPresent())
				{
					int stride = 4 * 2 + 4 + 0 * 4;

					int vertices2draw = (int) (regionMesh.regionSummary.verticesObject.getVramUsage() / stride);

					renderingContext.bindAttribute("vertexIn", regionMesh.regionSummary.verticesObject.asAttributeSource(VertexFormat.SHORT, 3, stride, 0L));
					renderingContext.bindAttribute("normalIn", regionMesh.regionSummary.verticesObject.asAttributeSource(VertexFormat.UBYTE, 4, stride, 8L));

					elements += vertices2draw;
					
					renderingContext.draw(Primitive.TRIANGLE, 0, vertices2draw);
				}
			}
		}
		catch (NullPointerException npe)
		{
			
		}

		return elements;
	}

	private void startAsynchSummaryRegeneration(Camera camera)
	{
		cameraChunkX = (int) (-camera.pos.getX() / 32);
		cameraChunkZ = (int) (-camera.pos.getZ() / 32);

		Thread asynchGenerateThread = new Thread()
		{
			@Override
			public void run()
			{
				Thread.currentThread().setName("Far terrain rebuilder thread");
				Thread.currentThread().setPriority(Constants.TERRAIN_RENDERER_THREAD_PRIORITY);

				generateArround();
			}
		};
		asynchGenerateThread.start();
	}

	/**
	 * 
	 */
	private void generateArround()
	{
		List<RegionMesh> regionsToRender_NewList = new ArrayList<RegionMesh>();
		int summaryDistance = 32;

		//Double check we won't run this concurrently
		synchronized (regionMeshBuffer)
		{
			//Iterate over X chunks but skip whole regions
			int currentChunkX = cameraChunkX - summaryDistance;
			while (currentChunkX < cameraChunkX + summaryDistance)
			{
				//Computes where are we
				int currentRegionX = (int) Math.floor(currentChunkX / 8);
				//if(currentChunkX < 0)
				//	currentRegionX--;
				int nextRegionX = currentRegionX + 1;
				int nextChunkX = nextRegionX * 8;

				//System.out.println("cx:" + currentChunkX + "crx: "+ currentRegionX + "nrx: " + nextRegionX + "ncx: " + nextChunkX);

				//Iterate over Z chunks but skip whole regions
				int currentChunkZ = cameraChunkZ - summaryDistance;
				while (currentChunkZ < cameraChunkZ + summaryDistance)
				{
					//Computes where are we
					int currentRegionZ = (int) Math.floor(currentChunkZ / 8);
					int nextRegionZ = currentRegionZ + 1;
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

					RegionSummaryImplementation summary = world.getRegionsSummariesHolder().getRegionSummaryWorldCoordinates(currentChunkX * 32, currentChunkZ * 32);

					if (summary == null)
					{
						currentChunkZ = nextChunkZ;
						continue;
					}

					RegionMesh regionMesh = new RegionMesh(rx, rz, summary);

					int rcx = currentChunkX % world.getSizeInChunks();
					if (rcx < 0)
						rcx += world.getSizeInChunks();
					int rcz = currentChunkZ % world.getSizeInChunks();
					if (rcz < 0)
						rcz += world.getSizeInChunks();

					int[] heightMap = regionMesh.regionSummary.heights;
					int[] ids = regionMesh.regionSummary.ids;

					@SuppressWarnings("unused")
					int vertexCount = 0;

					//Details cache array
					int[] details2use = new int[100];
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

							details2use[(scx + 1) * 10 + (scz + 1)] = detail;
						}

					for (int scx = 0; scx < 8; scx++)
						for (int scz = 0; scz < 8; scz++)
						{
							int details = details2use[(scx + 1) * 10 + (scz + 1)];
							int cellSize = (int) Math.pow(2, details);

							int x0 = (scx * 32) / cellSize;
							int y0 = (scz * 32) / cellSize;
							HeightmapMeshSummarizer mesher = new HeightmapMeshSummarizer(heightMap, ids, offsets[details], 32 / cellSize, x0, y0, 256 / cellSize);
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
								int heightCurrent = getHeight(heightMap, world, vx - cellSize, vz, currentRegionX, currentRegionZ, details2use[((int) Math.floor((vx - cellSize) / 32f) + 1) * 10 + (scz + 1)]);
								int d = 0;
								for (int i = 1; i < surf.getH() + 1; i++)
								{
									int newHeight = (i < surf.getH()) ? getHeight(heightMap, world, vx - cellSize, vz + i * cellSize, currentRegionX, currentRegionZ,
											details2use[((int) Math.floor((vx - cellSize) / 32f) + 1) * 10 + ((int) Math.floor((vz + (i) * cellSize) / 32f) + 1)]) : -1;
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
								heightCurrent = getHeight(heightMap, world, vx, vz - cellSize, currentRegionX, currentRegionZ, details2use[((int) Math.floor((vx) / 32f) + 1) * 10 + ((int) Math.floor((vz - cellSize) / 32f) + 1)]);
								d = 0;
								for (int i = 1; i < surf.getW() + 1; i++)
								{
									int newHeight = (i < surf.getW()) ? getHeight(heightMap, world, vx + i * cellSize, vz - cellSize, currentRegionX, currentRegionZ,
											details2use[((int) Math.floor((vx + i * cellSize) / 32f) + 1) * 10 + ((int) Math.floor((vz - cellSize) / 32f) + 1)]) : -1;
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
							int nextMeshDetailsX = details2use[(scx + 2) * 10 + (scz + 1)];
							if (nextMeshDetailsX > details)
							{
								int vx = scx * 32 + 32;
								for (int vz = scz * 32; vz < scz * 32 + 32; vz += cellSize)
								{

									int height = getHeight(heightMap, world, vx - 1, vz, currentRegionX, currentRegionZ, details);
									int heightNext = getHeight(heightMap, world, vx + 1, vz, currentRegionX, currentRegionZ, nextMeshDetailsX);

									if (heightNext > height)
									{
										int gapData = getIds(ids, world, vx - 1, vz, currentRegionX, currentRegionZ, details);

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
										int gapData = getIds(ids, world, vx + 1, vz, currentRegionX, currentRegionZ, details);

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

							int nextMeshDetailsZ = details2use[(scx + 1) * 10 + (scz + 2)];
							if (nextMeshDetailsZ > details)
							{
								int vz = scz * 32 + 32;
								for (int vx = scx * 32; vx < scx * 32 + 32; vx += cellSize)
								{
									int height = getHeight(heightMap, world, vx, vz - 1, currentRegionX, currentRegionZ, details);
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

					//System.out.println("vc:" + vertexCount);

					if(rx == 10 && rz == 2)
					{
						//System.out.println("wait");
						//regionMesh.regionSummary.sendNewModel(vboContent);
						//System.out.println(regionMeshBuffer);
					}
					byte[] vboContent = new byte[regionMeshBuffer.position()];
					regionMeshBuffer.flip();
					regionMeshBuffer.get(vboContent);
					/*if(rx == 10 && rz == 2)
					{
						System.out.println("2");
						//regionMesh.regionSummary.sendNewModel(vboContent);
						System.out.println(regionMeshBuffer);
						regionMeshBuffer.flip();
						
						int stride = 4 * 2 + 4 + 0 * 4;
						int vertices2draw = (int) (regionMeshBuffer.limit() / stride);
						
						for(int i = 0; i < vertices2draw; i++)
						{
							float k = regionMeshBuffer.getShort();
							regionMeshBuffer.getShort();
							System.out.println(k);
						}
						
						System.out.println(regionMeshBuffer);
					}*/
					//regionMesh.regionSummary.sendNewModel(new byte[0]);
					regionMesh.regionSummary.sendNewModel(vboContent);
					//else
					//	regionMesh.regionSummary.sendNewModel(vboContent);

					/*glBindBuffer(GL_ARRAY_BUFFER, regionMesh.regionSummary.vboId);
					glBufferData(GL_ARRAY_BUFFER, regionMeshBuffer, GL_DYNAMIC_DRAW);
					regionMesh.regionSummary.vboSize = vertexCount;*/

					lastRegionX = rcx / 8;
					lastRegionZ = rcz / 8;

					regionsToRender_NewList.add(regionMesh);

					currentChunkZ = nextChunkZ;
				}

				currentChunkX = nextChunkX;
			}

			lastLevelDetail = -1;
			lastRegionX = -1;
			lastRegionZ = -1;

			for (RegionMesh rs : regionsToRender)
			{
				rs.delete();
			}
			regionsToRender = regionsToRender_NewList;
			//regionsToRender.clear();
		}

		this.isTerrainUpdateRunning.set(false);
		this.lastTerrainUpdateTiming = System.currentTimeMillis();
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

	public void uploadGeneratedMeshes()
	{
		for (RegionMesh rs : regionsToRender)
		{
			if (rs == null)
				continue;
			if (rs.regionSummary == null)
				continue;
			//TODO investigate
			boolean generated = rs.regionSummary.uploadNeededData();
			if (generated)
			{
				//System.out.println("generated RS texture "+ rs.dataSource.hId);
			}
			//System.out.println(rs.dataSource.loaded.get());
			if (!rs.regionSummary.summaryLoaded.get())
				rs.regionSummary = world.getRegionsSummariesHolder().getRegionSummaryWorldCoordinates(rs.regionSummary.getRegionX() * 256, rs.regionSummary.getRegionZ() * 256);
		}
	}

	class RegionMesh
	{
		int regionDisplayedX, regionDisplayedZ;
		RegionSummaryImplementation regionSummary;

		public RegionMesh(int rxDisplay, int rzDisplay, RegionSummaryImplementation dataSource)
		{
			this.regionDisplayedX = rxDisplay;
			this.regionDisplayedZ = rzDisplay;
			this.regionSummary = dataSource;
		}

		public void delete()
		{
			//glDeleteBuffers(regionSummary.vboId);
		}
	}

	private void addVertexBytes(ByteBuffer terrain, int x, int y, int z, int nx, int ny, int nz, int voxelData)
	{
		terrain.putShort((short) x);
		terrain.putShort((short) (y + 1));
		terrain.putShort((short) z);
		terrain.putShort((short) 0x00);
		//terrain.putShort((short) VoxelFormat.id(voxelData));

		terrain.put((byte) (nx + 1));
		terrain.put((byte) (ny + 1));
		terrain.put((byte) (nz + 1));
		terrain.put((byte) (0x00));

		//terrain.putInt(VoxelFormat.id(voxelData));
	}

	public void destroy()
	{
		blockTexturesSummary.destroy();
		//glDeleteTextures(blocksTexturesSummaryId);
	}

}
