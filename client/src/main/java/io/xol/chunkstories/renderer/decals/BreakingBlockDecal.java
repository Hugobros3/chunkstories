package io.xol.chunkstories.renderer.decals;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.system.MemoryUtil;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.client.ClientContent;
import io.xol.chunkstories.api.rendering.Primitive;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.BlendMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.CullingMode;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.api.rendering.vertex.RecyclableByteBuffer;
import io.xol.chunkstories.api.rendering.vertex.VertexFormat;
import io.xol.chunkstories.api.voxel.VoxelSides.Corners;
import io.xol.chunkstories.api.voxel.models.ChunkRenderer;
import io.xol.chunkstories.api.voxel.models.VoxelBakerCubic;
import io.xol.chunkstories.api.voxel.models.VoxelBakerHighPoly;
import io.xol.chunkstories.api.voxel.models.VoxelRenderer;
import io.xol.chunkstories.api.voxel.models.layout.BaseLayoutBaker;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.LodLevel;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.ShadingType;
import io.xol.chunkstories.api.world.VoxelContext;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.core.item.ItemMiningTool.MiningProgress;
import io.xol.chunkstories.core.voxel.renderers.DefaultVoxelRenderer;
import io.xol.engine.base.MemFreeByteBuffer;
import io.xol.engine.graphics.geometry.VertexBufferGL;

public class BreakingBlockDecal {
	
	public final MiningProgress miningProgress;
	
	private final VertexBufferGL vertexBuffer;
	private int size;
	
	public BreakingBlockDecal(MiningProgress miningProgress) {
		this.miningProgress = miningProgress;
		VoxelContext ctx = miningProgress.loc.getWorld().peekSafely(miningProgress.loc);
		
		BreakingBlockDecalVoxelBaker bbdvb = new BreakingBlockDecalVoxelBaker(((WorldClient) ctx.getWorld()).getClient().getContent(), miningProgress.loc);
		
		ChunkRenderer chunkRenderer = new ChunkRenderer() {

			@Override
			public VoxelBakerHighPoly getHighpolyBakerFor(LodLevel lodLevel, ShadingType renderPass)
			{
				return bbdvb;
			}

			@Override
			public VoxelBakerCubic getLowpolyBakerFor(LodLevel lodLevel, ShadingType renderPass)
			{
				return bbdvb;
			}
			
		};
		
		ChunkRenderer.ChunkRenderContext o2 = new ChunkRenderer.ChunkRenderContext()
		{
			
			private VoxelLighter voxeLighter = new VoxelLighter() {

				@Override
				public byte getSunlightLevelForCorner(Corners corner)
				{
					return 0;
				}

				@Override
				public byte getBlocklightLevelForCorner(Corners corner)
				{
					return 0;
				}

				@Override
				public byte getAoLevelForCorner(Corners corner)
				{
					return 0;
				}

				@Override
				public byte getSunlightLevelInterpolated(float vertX, float vertY, float vertZ)
				{
					return 0;
				}

				@Override
				public byte getBlocklightLevelInterpolated(float vertX, float vertY, float vertZ)
				{
					return 0;
				}

				@Override
				public byte getAoLevelInterpolated(float vertX, float vertY, float vertZ)
				{
					return 0;
				}
				
			};

			@Override
			public boolean isTopChunkLoaded()
			{
				return false;
			}
			
			@Override
			public boolean isRightChunkLoaded()
			{
				return false;
			}
			
			@Override
			public boolean isLeftChunkLoaded()
			{
				return false;
			}
			
			@Override
			public boolean isFrontChunkLoaded()
			{
				return false;
			}
			
			@Override
			public boolean isBottomChunkLoaded()
			{
				return false;
			}
			
			@Override
			public boolean isBackChunkLoaded()
			{
				return false;
			}

			@Override
			public int getRenderedVoxelPositionInChunkX()
			{
				return ctx.getX() & 0x1f;
			}

			@Override
			public int getRenderedVoxelPositionInChunkY()
			{
				return ctx.getY() & 0x1f;
			}

			@Override
			public int getRenderedVoxelPositionInChunkZ()
			{
				return ctx.getZ() & 0x1f;
			}

			@Override
			public VoxelLighter getCurrentVoxelLighter()
			{
				return voxeLighter;
			}
		};
		
		//System.out.println(ctx.getVoxel().getVoxelRenderer(ctx));
		VoxelRenderer voxelRenderer = ctx.getVoxelRenderer();
		if(voxelRenderer == null) {
			voxelRenderer = new DefaultVoxelRenderer(ctx.getVoxel().store());
		}
		
		voxelRenderer.renderInto(chunkRenderer, o2, ctx.getWorld().getChunkWorldCoordinates(miningProgress.loc), ctx);
		
		RecyclableByteBuffer buffer = bbdvb.cum();
		
		vertexBuffer = new VertexBufferGL();
		vertexBuffer.uploadData(buffer);
	}
	
	ThreadLocal<ArrayList<Float>> memesAreDreams = new ThreadLocal<ArrayList<Float>>() {
		@Override
		protected ArrayList<Float> initialValue() {
			return new ArrayList<Float>();
		}
	};
	
	class BreakingBlockDecalVoxelBaker extends BaseLayoutBaker implements VoxelBakerCubic, VoxelBakerHighPoly {

		protected BreakingBlockDecalVoxelBaker(ClientContent content, Location loc) {
			super(content);
			
			memesAreMyReality = memesAreDreams.get();
			this.loc = loc;
			
			cx = (int)Math.floor(loc.x / 32);
			cy = (int)Math.floor(loc.y / 32);
			cz = (int)Math.floor(loc.z / 32);
		}

		Location loc;
		ArrayList<Float> memesAreMyReality;
		int cx,cy,cz;

		Vector3f position = new Vector3f();
		Vector3f positionF = new Vector3f();
		Vector3f normal2 = new Vector3f();
		Vector3f scrap = new Vector3f();
		
		Vector3f currentVertex = new Vector3f();
		
		/*BreakingBlockDecalVoxelBaker(Location loc) {
			memesAreMyReality = memesAreDreams.get();
			this.loc = loc;
			
			cx = (int)Math.floor(loc.x / 32);
			cy = (int)Math.floor(loc.y / 32);
			cz = (int)Math.floor(loc.z / 32);
		}*/
		
		@Override
		public void beginVertex(int i0, int i1, int i2) {
			this.beginVertex((float)i0, (float)i1, (float)i2);
		}
		
		public void beginVertex(float f0, float f1, float f2) {
			currentVertex.set(f0, f1, f2);
		}

		@Override
		public void beginVertex(Vector3fc vertex) {
			currentVertex.set(vertex);
		}

		@Override
		public void beginVertex(Vector3dc vertex) {
			currentVertex.set(vertex);
		}
		
		/*@Override
		public void addTexCoordInt(int i0, int i1) {}

		@Override
		public void addColors(float[] t) {}

		@Override
		public void addColors(byte sunLight, byte blockLight, byte ao) {}

		@Override
		public void addColorsSpecial(float[] t, int extended) {}

		@Override
		public void addColors(float f0, float f1, float f2) {}

		@Override
		public void addColorsSpecial(float f0, float f1, float f2, int extended) {}

		@Override
		public void addNormalsInt(int i0, int i1, int i2, byte extra) {
			//System.out.println(i0 + " " + i1 + " " + i2);
			normal2.set(i0 / 512f - 1.0f, i1 / 512f - 1.0f, i2 / 512f - 1.0f);
			normal2.normalize();
			
			//System.out.println(normal.x + " " + normal.y + " " + normal.z);
			
			scrap.set(normal2.z, normal2.x, normal2.y);
			
			float scrapzer = scrap.dot(positionF);
			memesAreMyReality.add(scrapzer);
			
			//System.out.println(scrapzer);
			
			scrap.set(normal2.y, normal2.z, normal2.x);
			
			scrapzer = scrap.dot(positionF);
			memesAreMyReality.add(scrapzer);
		}

		@Override
		public void addColorsAuto(VoxelLighter voxelLighter, Corners corner) {}

		@Override
		public void addVerticeInt(int i0, int i1, int i2) {
			this.addVerticeFloat((float)i0, (float)i1, (float)i2);
		}

		@Override
		public void addVerticeFloat(float f0, float f1, float f2) {
			memesAreMyReality.add(f0 + cx * 32);
			memesAreMyReality.add(f1 + cy * 32);
			memesAreMyReality.add(f2 + cz * 32);
			
			position.set(f0 + cx * 32, f1 + cy * 32, f2 + cz * 32);
			
			float fx = f0 - (float)loc.x;
			float fy = f1 - (float)loc.y;
			float fz = f2 - (float)loc.z;
			positionF.set(fx, fy, fz);
			
			System.out.println(f0);
			//System.out.println(position.x + " " + position.y + " " + position.z);
			
			//System.out.println(f0 + cx * 32 + "\\" + loc);
			
			size++;
		}*/
		
		@Override
		public void endVertex() {
			float f0 = currentVertex.x;
			float f1 = currentVertex.y;
			float f2 = currentVertex.z;
			
			memesAreMyReality.add(f0 + cx * 32);
			memesAreMyReality.add(f1 + cy * 32);
			memesAreMyReality.add(f2 + cz * 32);
			
			position.set(f0 + cx * 32, f1 + cy * 32, f2 + cz * 32);
			
			float fx = f0 - (float)loc.x;
			float fy = f1 - (float)loc.y;
			float fz = f2 - (float)loc.z;
			positionF.set(fx, fy, fz);
			
			//normal2.set(i0 / 512f - 1.0f, i1 / 512f - 1.0f, i2 / 512f - 1.0f);
			//normal2.normalize();
			
			normal2.set(normal);
			
			scrap.set(normal2.z, normal2.x, normal2.y);
			
			float scrapzer = scrap.dot(positionF);
			/*if(scrapzer < -0.89)
				scrapzer += 1f;*/
			memesAreMyReality.add(scrapzer);
			
			//System.out.println(scrapzer);
			
			scrap.set(normal2.y, normal2.z, normal2.x);
			
			scrapzer = scrap.dot(positionF);
			/*if(scrapzer < -0.89)
				scrapzer += 1f;*/
			memesAreMyReality.add(scrapzer);
			
			size++;
		}
		
		public RecyclableByteBuffer cum() {
			
			ByteBuffer buffer = MemoryUtil.memAlloc(4 * memesAreMyReality.size());
			for(float f : memesAreMyReality) {
				buffer.putFloat(f);
			}
			buffer.flip();
			
			MemFreeByteBuffer insideJob = new MemFreeByteBuffer(buffer);
			
			memesAreMyReality.clear();
			
			return insideJob;
		}
	}
	
	public void render(RenderingInterface renderingInterface) {
		ShaderInterface decalsShader = renderingInterface.useShader("decal_cracking");

		renderingInterface.getCamera().setupShader(decalsShader);
		
		//renderingInterface.bindTexture2D("zBuffer", worldRenderer.renderBuffers.zBuffer);
		
		renderingInterface.setCullingMode(CullingMode.DISABLED);
		renderingInterface.setBlendMode(BlendMode.MIX);
		renderingInterface.getRenderTargetManager().setDepthMask(false);

		int phases = 6;
		int phase = (int) (phases * miningProgress.progress);
		
		decalsShader.setUniform1f("textureScale", 1f / 6);
		decalsShader.setUniform1f("textureStart", 1f / 6 * phase);
		
		Texture2D diffuseTexture = renderingInterface.textures().getTexture("./textures/voxel_cracking.png");//decalType.getTexture();
		diffuseTexture.setTextureWrapping(true);
		diffuseTexture.setLinearFiltering(false);
		
		renderingInterface.bindAlbedoTexture(diffuseTexture);
		
		renderingInterface.bindAttribute("vertexIn", vertexBuffer.asAttributeSource(VertexFormat.FLOAT, 3, 4 * (3 + 2), 0));
		renderingInterface.bindAttribute("texCoordIn", vertexBuffer.asAttributeSource(VertexFormat.FLOAT, 2, 4 * (3 + 2), 4 * 3));
		
		renderingInterface.draw(Primitive.TRIANGLE, 0, size);
		
		renderingInterface.flush();
		

		renderingInterface.getRenderTargetManager().setDepthMask(true);
	}

	public void destroy() {
		vertexBuffer.destroy();
	}
}
