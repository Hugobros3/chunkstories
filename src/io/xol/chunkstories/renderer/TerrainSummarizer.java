package io.xol.chunkstories.renderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import org.lwjgl.BufferUtils;

import io.xol.chunkstories.client.FastConfig;
import io.xol.chunkstories.renderer.buffers.FloatBufferPool;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.voxel.VoxelTextures;
import io.xol.chunkstories.voxel.VoxelTypes;
import io.xol.chunkstories.world.World;
import io.xol.chunkstories.world.summary.ChunkSummary;
import io.xol.engine.model.RenderingContext;
import io.xol.engine.shaders.ShaderProgram;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class TerrainSummarizer
{

	private static final int TRIANGLES_PER_FACE = 2; // 2 triangles per face
	private static final int TRIANGLE_SIZE = 3; // 3 vertex per triangles
	private static final int VERTEX_SIZE = 3; // A vertex is 3 coordinates : xyz

	World world;

	FloatBuffer[][][][] vboContents = new FloatBuffer[8][8][4][2];

	List<RegionSummary> regionsToRender = new ArrayList<RegionSummary>();

	FloatBufferPool fbPool = new FloatBufferPool(96, 25000 * VERTEX_SIZE * TRIANGLE_SIZE * TRIANGLES_PER_FACE);

	public TerrainSummarizer(World world)
	{
		this.world = world;
		for (int dx = 0; dx < 8; dx++)
			for (int dy = 0; dy < 8; dy++)
				for (int lod = 0; lod < 4; lod++)
				{
					vboContents[dx][dy][lod][0] = generateFloatBuffer(lod, dx * 32, dy * 32, false);
					vboContents[dx][dy][lod][1] = generateFloatBuffer(lod, dx * 32, dy * 32, true);
				}
	}

	class RegionSummary
	{

		public RegionSummary(int rxDisplay, int rzDisplay, ChunkSummary dataSource)
		{
			this.rxDisplay = rxDisplay;
			this.rzDisplay = rzDisplay;
			this.dataSource = dataSource;
			fbId = fbPool.requestFloatBuffer();
			vbo = glGenBuffers();
			// System.out.println("Init rs "+rxDisplay+" : "+rzDisplay+" to "+fbId);
		}

		int rxDisplay, rzDisplay;
		ChunkSummary dataSource;
		int fbId;
		int vbo, vboSize;

		public void delete()
		{
			fbPool.releaseFloatBuffer(fbId);
			glDeleteBuffers(vbo);
		}

		public FloatBuffer accessFB()
		{
			return fbPool.accessFloatBuffer(fbId);
		}
	}

	private FloatBuffer generateFloatBuffer(int level, int dx, int dz, boolean border)
	{
		int resolution = 2 + level * 4;
		int nTiles = (int) Math.ceil(32f / resolution);
		float resolutionf = 32f / nTiles;
		FloatBuffer terrain = BufferUtils.createFloatBuffer(nTiles * nTiles * VERTEX_SIZE * TRIANGLE_SIZE * TRIANGLES_PER_FACE + (border ? (nTiles * 4 * VERTEX_SIZE * TRIANGLE_SIZE * TRIANGLES_PER_FACE) : 0));
		int y = 0;
		for (int i = 0; i < nTiles; i++)
		{
			for (int j = 0; j < nTiles; j++)
			{
				float x = dx + i * resolutionf;
				float z = dz + j * resolutionf;
				addVertex(terrain, x, y, z);
				addVertex(terrain, x + resolutionf, y, z);
				addVertex(terrain, x + resolutionf, y, z + resolutionf);

				addVertex(terrain, x, y, z);
				addVertex(terrain, x + resolutionf, y, z + resolutionf);
				addVertex(terrain, x, y, z + resolutionf);
			}
		}
		if (border)
		{
			for (int i = 0; i < nTiles; i++)
			{
				// North side
				float x = dx + i * resolutionf;
				float z = dz + 32;
				float ym = 10;
				addVertex(terrain, x, y, z);
				addVertex(terrain, x + resolutionf, y, z);
				addVertex(terrain, x + resolutionf, y - ym, z);

				addVertex(terrain, x, y, z);
				addVertex(terrain, x + resolutionf, y - ym, z);
				addVertex(terrain, x, y - ym, z);
				// South side
				z = dz + 0;
				addVertex(terrain, x, y, z);
				addVertex(terrain, x + resolutionf, y - ym, z);
				addVertex(terrain, x + resolutionf, y, z);

				addVertex(terrain, x, y, z);
				addVertex(terrain, x, y - ym, z);
				addVertex(terrain, x + resolutionf, y - ym, z);
				// East side
				x = dx + 0;
				z = dz + i * resolutionf;
				addVertex(terrain, x, y, z);
				addVertex(terrain, x, y, z + resolutionf);
				addVertex(terrain, x, y - ym, z + resolutionf);

				addVertex(terrain, x, y, z);
				addVertex(terrain, x, y - ym, z + resolutionf);
				addVertex(terrain, x, y - ym, z);
				// West side
				x = dx + 32;
				addVertex(terrain, x, y, z);
				addVertex(terrain, x, y, z + resolutionf);
				addVertex(terrain, x, y - ym, z + resolutionf);

				addVertex(terrain, x, y, z);
				addVertex(terrain, x, y - ym, z);
				addVertex(terrain, x, y - ym, z + resolutionf);
			}
		}
		terrain.flip();

		return terrain;
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

	int cx, cz;

	public int draw(RenderingContext renderingContext, ShaderProgram terrain)
	{
		int elements = 0;
		glDisable(GL_CULL_FACE); // culling for our glorious terrain
		
		//glPolygonMode( GL_FRONT_AND_BACK, GL_LINE );
		
		int vertexIn = terrain.getVertexAttributeLocation("vertexIn");
		renderingContext.enableVertexAttribute(vertexIn);	
		
		//Sort to draw near first
		List<RegionSummary> regionsToRenderSorted = new ArrayList<RegionSummary>(regionsToRender);
		//renderingContext.getCamera().getLocation;
		Camera camera = renderingContext.getCamera();
		int camRX = (int) (-camera.pos.x / 256);
		int camRZ = (int) (-camera.pos.z / 256);
		
		regionsToRenderSorted.sort(new Comparator<RegionSummary>() {

			@Override
			public int compare(RegionSummary a, RegionSummary b)
			{
				int distanceA = Math.abs(a.rxDisplay - camRX) + Math.abs(a.rzDisplay - camRZ);
				//System.out.println(camRX + " : " + distanceA);
				int distanceB = Math.abs(b.rxDisplay - camRX) + Math.abs(b.rzDisplay - camRZ);
				return distanceA - distanceB;
			}
			
		});
		
		for (RegionSummary rs : regionsToRenderSorted)
		{
			float height = 1024f;
			if(!renderingContext.getCamera().isBoxInFrustrum(new Vector3f(rs.rxDisplay * 256 + 128, height / 2, rs.rzDisplay * 256 + 128), new Vector3f(256, height, 256)))
				continue;
			
			terrain.setUniformSampler(1, "groundTexture", rs.dataSource.tId);

			glBindTexture(GL_TEXTURE_2D, rs.dataSource.tId);
			//glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER,  GL_NEAREST_MIPMAP_NEAREST);
			//glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER,  GL_NEAREST_MIPMAP_NEAREST);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			
			terrain.setUniformSampler(0, "heightMap", rs.dataSource.hId);
			glBindTexture(GL_TEXTURE_2D, rs.dataSource.hId);
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
			terrain.setUniformFloat2("regionPosition", rs.dataSource.rx, rs.dataSource.rz);

			// terrain.setUniformFloat2("chunkPositionActual", cs.dekalX,
			// cs.dekalZ);
			terrain.setUniformFloat2("chunkPosition", rs.rxDisplay * 256, rs.rzDisplay * 256);

			glBindBuffer(GL_ARRAY_BUFFER, rs.vbo);
			//glVertexPointer(3, GL_FLOAT, 0, 0L);
			glVertexAttribPointer(vertexIn, 3, GL_FLOAT, false, 0, 0L);

			//if(rs.vbo == 840)
			//	System.out.println("drawing cs size="+rs.vboSize+" t"+rs.dataSource.hId+"vbo"+rs.vbo);

			elements += rs.vboSize;

			if (rs.vboSize > 0 && rs.dataSource.hId >= 0)
				glDrawArrays(GL_TRIANGLES, 0, rs.vboSize);
		}
		//System.out.println(regionsToRender.size()+"parts");

		//glDisableClientState(GL_VERTEX_ARRAY);
		renderingContext.disableVertexAttribute(vertexIn);	
		
		glPolygonMode( GL_FRONT_AND_BACK, GL_FILL );
		return elements;
	}

	int lastRegionX = -1;
	int lastRegionZ = -1;

	int lastLevelDetail = -1;

	private int drawDetailIntoFloatBuffer(RegionSummary summary, int level, boolean border, boolean changedRegion, int dx, int dz)
	{

		int resolution = 2 + level * 4;
		int nTiles = (int) Math.ceil(32f / resolution);
		// border = true;
		int elements = (nTiles * nTiles + nTiles * (border ? 4 : 0)) * 2 * 3;

		FloatBuffer detailBuffer = vboContents[dx][dz][level][border ? 1 : 0];
		// detailBuffer = this.generateFloatBuffer(level, dx, dz, false);
		// detailBuffer = this.generateFloatBuffer(level, dx*32, dz*32, false);
		detailBuffer.position(0);

		// System.out.println(detailBuffer.toString());

		// elements = detailBuffer.capacity();
		summary.accessFB().put(detailBuffer);
		// summary.vboBuffer.put(this.generateFloatBuffer(level, dx, dz,
		// false));
		summary.vboSize += elements;
		return elements;
	}

	long lastGen = 0;

	int totalSize = 0;

	// public FloatBuffer vboBuffer =
	// BufferUtils.createFloatBuffer(25000*VERTEX_SIZE*TRIANGLE_SIZE*TRIANGLES_PER_FACE);

	public RegionSummary getRegionSummaryAt(int cx, int cz)
	{
		int rx = cx / 8;
		int rz = cz / 8;
		if (cz < 0 && cz % 8 != 0)
			rz--;
		if (cx < 0 && cx % 8 != 0)
			rx--;
		for (RegionSummary rs : regionsToRender)
		{
			if (rs.rxDisplay == rx && rs.rzDisplay == rz)
				return rs;
		}
		RegionSummary rs = new RegionSummary(rx, rz, world.chunkSummaries.get(cx * 32, cz * 32));
		//System.out.println(rs.dataSource+"");
		regionsToRender.add(rs);
		return rs;
	}

	public void generateArround(double camPosX, double camPosZ)
	{
		long time = System.currentTimeMillis();
		
		cx = (int) (camPosX / 32);
		cz = (int) (camPosZ / 32);
		
		totalSize = 0;

		for (RegionSummary rs : regionsToRender)
		{
			rs.delete();
		}
		regionsToRender.clear();

		int summaryDistance = 32;

		for (int a = cx - summaryDistance; a < cx + summaryDistance; a++)
		{
			for (int b = cz - summaryDistance; b < cz + summaryDistance; b++)
			{
				int rcx = a % world.getSizeInChunks();
				if (rcx < 0)
					rcx += world.getSizeInChunks();
				int rcz = b % world.getSizeInChunks();
				if (rcz < 0)
					rcz += world.getSizeInChunks();
				boolean changedRegion = false;

				RegionSummary rs = getRegionSummaryAt(a, b);
				
				if (lastRegionX != rcx / 8 || lastRegionZ != rcz / 8)
				{
					changedRegion = true;
				}
				// Dekal
				// cs.dekalX = rcx-a;
				// cs.dekalZ = rcz-b;

				int distance = Math.abs(a - cx) + Math.abs(b - cz);
				boolean border = false;
				int detail = 0;
				
				double distanceScaleFactor = 1.0;
				if(FastConfig.hqTerrain)
					distanceScaleFactor = 0.5;
				
				if (distance * distanceScaleFactor > 5)
					detail = 1;
				if (distance * distanceScaleFactor > 10)
					detail = 2;
				if (distance * distanceScaleFactor > 15)
					detail = 3;
				if (distance == 4 || distance == 11 || distance == 16)
					border = true;

				// detail = 3;
				// border = false;

				if (rcx % 8 == 0 || rcz % 8 == 0 || rcx % 8 == 7 || rcz % 8 == 7)
					border = true;

				totalSize += drawDetailIntoFloatBuffer(rs, detail, border, changedRegion, rcx % 8, rcz % 8);

				lastRegionX = rcx / 8;
				lastRegionZ = rcz / 8;
			}
		}

		//System.out.println(regionsToRender.size()+"parts");
		
		for (RegionSummary rs : regionsToRender)
		{
			glBindBuffer(GL_ARRAY_BUFFER, rs.vbo);
			rs.accessFB().flip();
			glBufferData(GL_ARRAY_BUFFER, rs.accessFB(), GL_DYNAMIC_DRAW);
			// System.out.println("cs init size :"+cs.vboSize);

		}

		lastLevelDetail = -1;
		lastRegionX = -1;
		lastRegionZ = -1;

		lastGen = time;
	}
	
	public void updateData()
	{
		
		for(RegionSummary rs : regionsToRender)
		{
			boolean generated = rs.dataSource.glGen();
			if(generated)
			{
				//System.out.println("generated RS texture "+ rs.dataSource.hId);
			}
			//System.out.println(rs.dataSource.loaded.get());
			if(!rs.dataSource.loaded.get())
				rs.dataSource = world.chunkSummaries.get(rs.dataSource.rx * 256, rs.dataSource.rz * 256);
		}
		//System.out.println(regionsToRender.size()+"parts");
	}

	private void addVertex(FloatBuffer terrain, float x, float y, float z)
	{
		// Add vertex coordinates
		float[] vertexPos = new float[] { x, y, z };
		terrain.put(vertexPos);
	}

	public void destroy()
	{
		glDeleteTextures(blocksTexturesSummaryId);
	}
}
