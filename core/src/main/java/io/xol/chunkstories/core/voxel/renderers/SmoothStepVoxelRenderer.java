package io.xol.chunkstories.core.voxel.renderers;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.LodLevel;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.ShadingType;
import io.xol.chunkstories.api.voxel.models.ChunkRenderer;
import io.xol.chunkstories.api.voxel.models.VoxelModel;
import io.xol.chunkstories.api.voxel.models.ChunkRenderer.ChunkRenderContext;
import io.xol.chunkstories.api.voxel.models.VoxelBakerHighPoly;
import io.xol.chunkstories.api.voxel.models.VoxelRenderer;
import io.xol.chunkstories.api.voxel.textures.VoxelTexture;
import io.xol.chunkstories.api.world.VoxelContext;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.core.voxel.Voxel8Steps;

/** Experiment attempting to make a smooth terrain system */
public class SmoothStepVoxelRenderer implements VoxelRenderer {

	private final Voxel8Steps voxel;
	private final VoxelModel[] steps;
	
	public SmoothStepVoxelRenderer(Voxel8Steps voxel, VoxelModel[] steps) {
		super();
		this.voxel = voxel;
		this.steps = steps;
	}
	
	private VoxelRenderer old(VoxelContext info) {
		return steps[info.getMetaData() % 8];
	}
	
	@Override
	public int renderInto(ChunkRenderer chunkRenderer, ChunkRenderContext bakingContext, Chunk chunk,
			VoxelContext voxelInformations) {
		
		int x = bakingContext.getRenderedVoxelPositionInChunkX();
		int y = bakingContext.getRenderedVoxelPositionInChunkY();
		int z = bakingContext.getRenderedVoxelPositionInChunkZ();
		
		//Check we have 8 neighbours
		World world = voxelInformations.getWorld();
		int wx = x + chunk.getChunkX() * 32;
		int wy = y + chunk.getChunkY() * 32;
		int wz = z + chunk.getChunkZ() * 32;
		
		byte sunlight = 0;//bakingContext.getCurrentVoxelLighter().getSunlightLevelInterpolated(x + 0.5f, y + 0.5f, z + 0.5f);
		byte blockLight = 15;//bakingContext.getCurrentVoxelLighter().getBlocklightLevelInterpolated(x + 0.5f, y + 0.5f, z + 0.5f);
		byte ao = bakingContext.getCurrentVoxelLighter().getAoLevelInterpolated(x + 0.5f, y + 0.5f, z + 0.5f);
		
		VoxelBakerHighPoly baker = chunkRenderer.getHighpolyBakerFor(LodLevel.ANY, ShadingType.OPAQUE);
		
		/*for(int a = wx - 1; a <= wx + 1; a++)
			for(int b = wz - 1; b <= wz + 1; b ++) {
				//If a single one is fucky
				if(VoxelFormat.id(world.getVoxelData(a, wy, b)) != voxel.getId())
					return old(voxelInformations).renderInto(chunkRenderer, bakingContext, chunk, voxelInformations);
			}*/

		//if(true)
		//	return old(voxelInformations).renderInto(chunkRenderer, bakingContext, chunk, voxelInformations);
		
		VoxelTexture texture = voxel.getVoxelTexture(voxelInformations.getData(), VoxelSides.TOP, voxelInformations);
		
		baker.usingTexture(texture);
		
		//int offset = texture.getAtlasOffset() / texture.getTextureScale();
		//int textureS = texture.getAtlasS() + (x % texture.getTextureScale()) * offset;
		//int textureT = texture.getAtlasT() + (z % texture.getTextureScale()) * offset;
		
		//final int max_step = 8;
		int goodID = voxel.getId();
		
		float height[] = new float[9];
		
		for(int a = wx - 1; a <= wx + 1; a++)
			bLoop:
			for(int b = wz - 1; b <= wz + 1; b ++) {
				for(int h = wy + 1; h >= wy - 1; h--) {
					int data = world.getVoxelData(a, h, b);
					if(VoxelFormat.id(data) == goodID) {
						height[(a - wx + 1) * 3 + b - wz + 1] = h - chunk.getChunkY() * 32 + 1/8f + VoxelFormat.meta(data) / 8f;
						continue bLoop;
					}
				}
				return old(voxelInformations).renderInto(chunkRenderer, bakingContext, chunk, voxelInformations);
			}
		
		// X --->
		// 036
		// 147
		// 258
		
		float corner00 = (height[0] + height[3] + height[1] + height[4]) / 4f;
		float corner10 = (height[3] + height[4] + height[7] + height[6]) / 4f;
		float corner01 = (height[1] + height[4] + height[2] + height[5]) / 4f;
		float corner11 = (height[4] + height[7] + height[5] + height[8]) / 4f;
		
		
		
		
		
		/*float corner00, corner01, corner10, corner11;
		int data00, data01, data10, data11;
		
		data00 = world.getVoxelData(wx, wy, wz);
		corner00 = 1/8f + y + VoxelFormat.meta(data00) / 8f;
		
		//Merge with left/top upper lip
		int dataN0p  = world.getVoxelData(wx - 1, wy + 1, wz);
		int data0Np  = world.getVoxelData(wx, wy + 1, wz - 1);
		int dataNNp  = world.getVoxelData(wx - 1, wy + 1, wz - 1);
		if(VoxelFormat.id(dataN0p) == voxel.getId()) {
			int meta = VoxelFormat.meta(dataN0p);
			if(meta < max_step)
				corner00 = Math.max(0, 1/8f + y + 1 + meta / 8f);
		}
		else if(VoxelFormat.id(data0Np) == voxel.getId()) {
			int meta = VoxelFormat.meta(data0Np);
			if(meta < max_step)
				corner00 = Math.max(0, 1/8f + y + 1 + meta / 8f);
		}
		else if(VoxelFormat.id(dataNNp) == voxel.getId()) {
			int meta = VoxelFormat.meta(dataNNp);
			if(meta < max_step)
				corner00 = Math.max(0, 1/8f + y + 1 + meta / 8f);
		}
		
		int data01p = world.getVoxelData(wx, wy + 1, wz + 1);
		if(VoxelFormat.id(data01p) == voxel.getId()) {
			corner01 = 1/8f + y + 1 +VoxelFormat.meta(data01p) / 8f;
		}
		else {
			data01 = world.getVoxelData(wx, wy, wz + 1);
			if(VoxelFormat.id(data01) == voxel.getId()) {
				corner01 = 1/8f + y +VoxelFormat.meta(data01) / 8f;
			}
			else {
				corner01 = corner00;
			}
		}
		
		if(VoxelFormat.id(dataN0p) == voxel.getId()) {
			int meta = VoxelFormat.meta(dataN0p);
			if(meta < max_step)
				corner01 = Math.max(0, 1/8f + y + 1 + meta / 8f);
		}
		
		int data10p = world.getVoxelData(wx + 1, wy + 1, wz);
		if(VoxelFormat.id(data10p) == voxel.getId()) {
			corner10 = 1/8f + y + 1 +VoxelFormat.meta(data10p) / 8f;
		}
		else {
			data10 = world.getVoxelData(wx + 1, wy, wz);
			if(VoxelFormat.id(data10) == voxel.getId()) {
				corner10 = 1/8f + y +VoxelFormat.meta(data10) / 8f;
			}
			else {
				corner10 = corner00;
			}
		}
		
		if(VoxelFormat.id(data0Np) == voxel.getId()) {
			int meta = VoxelFormat.meta(data0Np);
			if(meta < max_step)
				corner10 = Math.max(0, 1/8f + y + 1 + meta / 8f);
		}
		
		int data11p = world.getVoxelData(wx + 1, wy + 1, wz + 1);
		if(VoxelFormat.id(data11p) == voxel.getId()) {
			corner11 = 1/8f + y + 1 +VoxelFormat.meta(data11p) / 8f;
		}
		else {
			data11 = world.getVoxelData(wx + 1, wy, wz + 1);
			if(VoxelFormat.id(data11) == voxel.getId()) {
				corner11 = 1/8f + y +VoxelFormat.meta(data11) / 8f;
			}
			else {
				corner11 = corner00;
			}
		}
		
		/*float corner00 = 1/8f + y + VoxelFormat.meta(world.getVoxelData(wx, wy, wz)) / 8f;
		int dataN0p  = world.getVoxelData(wx - 1, wy + 1, wz);
		int data0Np  = world.getVoxelData(wx, wy + 1, wz - 1);
		int dataNNp  = world.getVoxelData(wx - 1, wy + 1, wz - 1);
		if(VoxelFormat.id(dataN0p) == voxel.getId()) {
			int meta = VoxelFormat.meta(dataN0p);
			if(meta < max_step)
				corner00 = Math.max(corner00, 1/8f + y + 1 + meta / 8f);
		}
		else if(VoxelFormat.id(data0Np) == voxel.getId()) {
			int meta = VoxelFormat.meta(data0Np);
			if(meta < max_step)
				corner00 = Math.max(corner00, 1/8f + y + 1 + meta / 8f);
		}
		else if(VoxelFormat.id(dataNNp) == voxel.getId()) {
			int meta = VoxelFormat.meta(dataNNp);
			if(meta < max_step)
				corner00 = Math.max(corner00, 1/8f + y + 1 + meta / 8f);
		}
		
		int data01 = world.getVoxelData(wx, wy, wz + 1);
		float corner01;
		if(VoxelFormat.id(data01) == voxel.getId()) {
			corner01 = 1/8f + y + VoxelFormat.meta(data01) / 8f;
		}
		else {
			corner01 = corner00;
			int data01p = world.getVoxelData(wx, wy + 1, wz + 1);
			if(VoxelFormat.id(data01p) == voxel.getId()) {
				int meta = VoxelFormat.meta(data01p);
				if(meta < max_step)
					corner01 = 1/8f + y + 1 + meta / 8f;
			}
		}
		
		int dataN1p = world.getVoxelData(wx - 1, wy + 1, wz);
		if(VoxelFormat.id(dataN1p) == voxel.getId()) {
			int meta = VoxelFormat.meta(dataN1p);
			if(meta < max_step)
				corner01 = Math.max(corner01, 1/8f + y + 1 + meta / 8f);
		}
		
		//float corner01 = VoxelFormat.id(data01) == voxel.getId() ? 1/8f + y + VoxelFormat.meta(data01) / 8f : corner00;
		
		int data10 = world.getVoxelData(wx + 1, wy, wz);
		float corner10;
		if(VoxelFormat.id(data10) == voxel.getId()) {
			corner10 = 1/8f + y + VoxelFormat.meta(data10) / 8f;
		}
		else {
			corner10 = corner00;
			int data10p = world.getVoxelData(wx + 1, wy + 1, wz);
			if(VoxelFormat.id(data10p) == voxel.getId()) {
				int meta = VoxelFormat.meta(data10p);
				if(meta < max_step)
					corner10 = 1/8f + y + 1 + meta / 8f;
			}
		}
		//float corner10 = VoxelFormat.id(data10) == voxel.getId() ? 1/8f + y + VoxelFormat.meta(data10) / 8f : corner00;
		
		int data1Np = world.getVoxelData(wx, wy + 1, wz - 1);
		if(VoxelFormat.id(data1Np) == voxel.getId()) {
			int meta = VoxelFormat.meta(data1Np);
			if(meta < max_step)
				corner10 = Math.max(corner10, 1/8f + y + 1 + meta / 8f);
		}
		
		int data11 = world.getVoxelData(wx + 1, wy, wz + 1);
		float corner11;
		if(VoxelFormat.id(data11) == voxel.getId()) {
			corner11 = 1/8f + y + VoxelFormat.meta(data11) / 8f;
		}
		else {
			corner11 =  Math.max(corner10, corner01);
			int data11p = world.getVoxelData(wx + 1, wy + 1, wz + 1);
			if(VoxelFormat.id(data11p) == voxel.getId()) {
				int meta = VoxelFormat.meta(data11p);
				if(meta < max_step)
					corner11 = 1/8f + y + 1 + meta / 8f;
			}
		}*/
		//float corner11 = VoxelFormat.id(data11) == voxel.getId() ? 1/8f + y + VoxelFormat.meta(data11) / 8f : Math.min(corner10, corner01);
		
		baker.setNormal(0f, 1f, 0f);
		
		baker.beginVertex(x, corner00, z);
		baker.setTextureCoordinates(0, 0);
		baker.setVoxelLight(sunlight, blockLight, ao);
		//baker.addNormalsInt(511, 1023, 511, (byte)0);
		baker.endVertex();
		
		baker.beginVertex(x + 1, corner11, z + 1);
		baker.setTextureCoordinates(0 + 1, 0 + 1);
		baker.setVoxelLight(sunlight, blockLight, ao);
		//baker.addNormalsInt(511, 1023, 511, (byte)0);
		baker.endVertex();
		
		baker.beginVertex(x + 1, corner10, z);
		baker.setTextureCoordinates(0 + 1, 0);
		baker.setVoxelLight(sunlight, blockLight, ao);
		//baker.addNormalsInt(511, 1023, 511, (byte)0);
		baker.endVertex();
		
		// <- ------------------- ->
		
		baker.beginVertex(x, corner01, z + 1);
		baker.setTextureCoordinates(0, 0 + 1);
		baker.setVoxelLight(sunlight, blockLight, ao);
		//baker.addNormalsInt(511, 1023, 511, (byte)0);
		baker.endVertex();
		
		baker.beginVertex(x + 1, corner11, z + 1);
		baker.setTextureCoordinates(0 + 1, 0 + 1);
		baker.setVoxelLight(sunlight, blockLight, ao);
		//baker.addNormalsInt(511, 1023, 511, (byte)0);
		baker.endVertex();
		
		baker.beginVertex(x, corner00, z);
		baker.setTextureCoordinates(0, 0);
		baker.setVoxelLight(sunlight, blockLight, ao);
		//baker.addNormalsInt(511, 1023, 511, (byte)0);
		baker.endVertex();
		
		return 6;
	}

}
