package io.xol.chunkstories.core.voxel.renderers;

import io.xol.chunkstories.api.Content.Voxels;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.voxel.VoxelSides.Corners;
import io.xol.chunkstories.api.voxel.models.ChunkRenderer;
import io.xol.chunkstories.api.voxel.models.VoxelBakerCubic;
import io.xol.chunkstories.api.voxel.models.VoxelRenderer;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.LodLevel;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.ShadingType;
import io.xol.chunkstories.api.voxel.models.ChunkRenderer.ChunkRenderContext;
import io.xol.chunkstories.api.voxel.textures.VoxelTexture;
import io.xol.chunkstories.api.world.VoxelContext;
import io.xol.chunkstories.api.world.chunk.Chunk;

/** Renders classic voxels as cubes ( and intelligently culls faces ) */
public class DefaultVoxelRenderer implements VoxelRenderer
{
	final Voxels store;
	
	public DefaultVoxelRenderer(Voxels store) {
		this.store = store;
	}
	
	@Override
	public int renderInto(ChunkRenderer chunkRenderer, ChunkRenderContext bakingContext, Chunk chunk, VoxelContext voxelInformations)
	{
		Voxel vox = voxelInformations.getVoxel();
		int src = voxelInformations.getData();
		
		int i = voxelInformations.getX() & 0x1F;
		int k = voxelInformations.getY() & 0x1F;
		int j = voxelInformations.getZ() & 0x1F;
		
		VoxelBakerCubic vbc = chunkRenderer.getLowpolyBakerFor(LodLevel.ANY, ShadingType.OPAQUE);
		byte wavyVegetationFlag = 0;
		
		int vertices = 0;
		
		if (shallBuildWallArround(voxelInformations, 5) && (k != 0 || bakingContext.isBottomChunkLoaded()))
		{
			addQuadBottom(chunk, bakingContext, vbc, i, k, j, vox.getVoxelTexture(src, VoxelSides.BOTTOM, voxelInformations), wavyVegetationFlag);
			vertices += 6;
		}
		if (shallBuildWallArround(voxelInformations, 4) && (k != 31 || bakingContext.isTopChunkLoaded()))
		{
			addQuadTop(chunk, bakingContext, vbc, i, k, j, vox.getVoxelTexture(src, VoxelSides.TOP, voxelInformations), wavyVegetationFlag);
			vertices += 6;
		}
		if (shallBuildWallArround(voxelInformations, 2) && (i != 31 || bakingContext.isRightChunkLoaded()))
		{
			addQuadRight(chunk, bakingContext, vbc, i, k, j, vox.getVoxelTexture(src, VoxelSides.RIGHT, voxelInformations), wavyVegetationFlag);
			vertices += 6;
		}
		if (shallBuildWallArround(voxelInformations, 0) && (i != 0 || bakingContext.isLeftChunkLoaded()))
		{
			addQuadLeft(chunk, bakingContext, vbc, i, k, j, vox.getVoxelTexture(src, VoxelSides.LEFT, voxelInformations), wavyVegetationFlag);
			vertices += 6;
		}
		if (shallBuildWallArround(voxelInformations, 1) && (j != 31 || bakingContext.isFrontChunkLoaded()))
		{
			addQuadFront(chunk, bakingContext, vbc, i, k, j, vox.getVoxelTexture(src, VoxelSides.FRONT, voxelInformations), wavyVegetationFlag);
			vertices += 6;
		}
		if (shallBuildWallArround(voxelInformations, 3) && (j != 0 || bakingContext.isBackChunkLoaded()))
		{
			addQuadBack(chunk, bakingContext, vbc, i, k, j, vox.getVoxelTexture(src, VoxelSides.BACK, voxelInformations), wavyVegetationFlag);
			vertices += 6;
		}
		
		vbc.reset();

		return vertices;
	}

	protected void addQuadTop(Chunk c, ChunkRenderContext bakingContext, VoxelBakerCubic rbbf, int sx, int sy, int sz, VoxelTexture texture, byte wavy)
	{
		rbbf.usingTexture(texture);
		rbbf.setNormal(0f, 1f, 0f);
		rbbf.setWavyFlag(wavy != 0);
		
		rbbf.beginVertex(sx, sy + 1, sz);
		rbbf.setTextureCoordinates(0f, 0f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_BACK_LEFT);
		rbbf.endVertex();

		rbbf.beginVertex(sx + 1, sy + 1, sz + 1);
		rbbf.setTextureCoordinates(1f, 1f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_FRONT_RIGHT);
		rbbf.endVertex();

		rbbf.beginVertex(sx + 1, sy + 1, sz);
		rbbf.setTextureCoordinates(1f, 0f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_BACK_RIGHT);
		rbbf.endVertex();

		rbbf.beginVertex(sx, sy + 1, sz + 1);
		rbbf.setTextureCoordinates(0f, 1f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_FRONT_LEFT);
		rbbf.endVertex();

		rbbf.beginVertex(sx + 1, sy + 1, sz + 1);
		rbbf.setTextureCoordinates(1f, 1f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_FRONT_RIGHT);
		rbbf.endVertex();

		rbbf.beginVertex(sx, sy + 1, sz);
		rbbf.setTextureCoordinates(0f, 0f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_BACK_LEFT);
		rbbf.endVertex();
	}

	protected void addQuadBottom(Chunk c, ChunkRenderContext bakingContext, VoxelBakerCubic rbbf, int sx, int sy, int sz, VoxelTexture texture, byte wavy)
	{
		rbbf.usingTexture(texture);
		rbbf.setNormal(0f, -1f, 0f);
		rbbf.setWavyFlag(wavy != 0);

		rbbf.beginVertex(sx + 1, sy, sz);
		rbbf.setTextureCoordinates(0f, 0f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_BACK_RIGHT);
		rbbf.endVertex();

		rbbf.beginVertex(sx + 1, sy, sz + 1);
		rbbf.setTextureCoordinates(0f, 1f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_FRONT_RIGHT);
		rbbf.endVertex();

		rbbf.beginVertex(sx, sy, sz);
		rbbf.setTextureCoordinates(1f, 0f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_BACK_LEFT);
		rbbf.endVertex();

		rbbf.beginVertex(sx, sy, sz);
		rbbf.setTextureCoordinates(1f, 0f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_BACK_LEFT);
		rbbf.endVertex();

		rbbf.beginVertex(sx + 1, sy, sz + 1);
		rbbf.setTextureCoordinates(0f, 1f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_FRONT_RIGHT);
		rbbf.endVertex();

		rbbf.beginVertex(sx, sy, sz + 1);
		rbbf.setTextureCoordinates(1f, 1f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_FRONT_LEFT);
		rbbf.endVertex();
	}

	protected void addQuadRight(Chunk c, ChunkRenderContext bakingContext, VoxelBakerCubic rbbf, int sx, int sy, int sz, VoxelTexture texture, byte wavy)
	{
		rbbf.usingTexture(texture);
		rbbf.setNormal(1f, 0f, 0f);
		rbbf.setWavyFlag(wavy != 0);

		rbbf.beginVertex(sx + 1, sy + 1, sz);
		rbbf.setTextureCoordinates(1f, 0f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_BACK_RIGHT);
		rbbf.endVertex();

		rbbf.beginVertex(sx + 1, sy + 1, sz + 1);
		rbbf.setTextureCoordinates(0f, 0f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_FRONT_RIGHT);
		rbbf.endVertex();

		rbbf.beginVertex(sx + 1, sy - 0, sz);
		rbbf.setTextureCoordinates(1f, 1f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_BACK_RIGHT);
		rbbf.endVertex();

		rbbf.beginVertex(sx + 1, sy - 0, sz);
		rbbf.setTextureCoordinates(1f, 1f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_BACK_RIGHT);
		rbbf.endVertex();

		rbbf.beginVertex(sx + 1, sy + 1, sz + 1);
		rbbf.setTextureCoordinates(0f, 0f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_FRONT_RIGHT);
		rbbf.endVertex();

		rbbf.beginVertex(sx + 1, sy - 0, sz + 1);
		rbbf.setTextureCoordinates(0f, 1f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_FRONT_RIGHT);
		rbbf.endVertex();
	}

	protected void addQuadLeft(Chunk c, ChunkRenderContext bakingContext, VoxelBakerCubic rbbf, int sx, int sy, int sz, VoxelTexture texture, byte wavy)
	{
		rbbf.usingTexture(texture);
		rbbf.setNormal(-1f, 0f, 0f);
		rbbf.setWavyFlag(wavy != 0);

		rbbf.beginVertex(sx, sy - 0, sz);
		rbbf.setTextureCoordinates(0f, 1f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_BACK_LEFT);
		rbbf.endVertex();

		rbbf.beginVertex(sx, sy + 1, sz + 1);
		rbbf.setTextureCoordinates(1f, 0f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_FRONT_LEFT);
		rbbf.endVertex();

		rbbf.beginVertex(sx, sy + 1, sz);
		rbbf.setTextureCoordinates(0f, 0f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_BACK_LEFT);
		rbbf.endVertex();

		rbbf.beginVertex(sx, sy - 0, sz + 1);
		rbbf.setTextureCoordinates(1f, 1f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_FRONT_LEFT);
		rbbf.endVertex();

		rbbf.beginVertex(sx, sy + 1, sz + 1);
		rbbf.setTextureCoordinates(1f, 0f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_FRONT_LEFT);
		rbbf.endVertex();

		rbbf.beginVertex(sx, sy - 0, sz);
		rbbf.setTextureCoordinates(0f, 1f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_BACK_LEFT);
		rbbf.endVertex();

	}

	protected void addQuadFront(Chunk c, ChunkRenderContext bakingContext, VoxelBakerCubic rbbf, int sx, int sy, int sz, VoxelTexture texture, byte wavy)
	{
		rbbf.usingTexture(texture);
		rbbf.setNormal(0f, 0f, 1f);
		rbbf.setWavyFlag(wavy != 0);

		rbbf.beginVertex(sx, sy - 0, sz + 1);
		rbbf.setTextureCoordinates(0f, 1f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_FRONT_LEFT);
		rbbf.endVertex();

		rbbf.beginVertex(sx + 1, sy + 1, sz + 1);
		rbbf.setTextureCoordinates(1f, 0f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_FRONT_RIGHT);
		rbbf.endVertex();

		rbbf.beginVertex(sx, sy + 1, sz + 1);
		rbbf.setTextureCoordinates(0f, 0f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_FRONT_LEFT);
		rbbf.endVertex();

		rbbf.beginVertex(sx + 1, sy - 0, sz + 1);
		rbbf.setTextureCoordinates(1f, 1f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_FRONT_RIGHT);
		rbbf.endVertex();

		rbbf.beginVertex(sx + 1, sy + 1, sz + 1);
		rbbf.setTextureCoordinates(1f, 0f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_FRONT_RIGHT);
		rbbf.endVertex();

		rbbf.beginVertex(sx, sy - 0, sz + 1);
		rbbf.setTextureCoordinates(0f, 1f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_FRONT_LEFT);
		rbbf.endVertex();

	}

	protected void addQuadBack(Chunk c, ChunkRenderContext bakingContext, VoxelBakerCubic rbbf, int sx, int sy, int sz, VoxelTexture texture, byte wavy)
	{
		rbbf.usingTexture(texture);
		rbbf.setNormal(0f, 0f, -1f);
		rbbf.setWavyFlag(wavy != 0);

		rbbf.beginVertex(sx, sy + 1, sz);
		rbbf.setTextureCoordinates(1f, 0f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_BACK_LEFT);
		rbbf.endVertex();

		rbbf.beginVertex(sx + 1, sy + 1, sz);
		rbbf.setTextureCoordinates(0f, 0f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_BACK_RIGHT);
		rbbf.endVertex();

		rbbf.beginVertex(sx, sy - 0, sz);
		rbbf.setTextureCoordinates(1f, 1f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_BACK_LEFT);
		rbbf.endVertex();

		rbbf.beginVertex(sx, sy - 0, sz);
		rbbf.setTextureCoordinates(1f, 1f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_BACK_LEFT);
		rbbf.endVertex();

		rbbf.beginVertex(sx + 1, sy + 1, sz);
		rbbf.setTextureCoordinates(0f, 0f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_BACK_RIGHT);
		rbbf.endVertex();

		rbbf.beginVertex(sx + 1, sy - 0, sz);
		rbbf.setTextureCoordinates(0f, 1f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_BACK_RIGHT);
		rbbf.endVertex();
	}

	protected boolean shallBuildWallArround(VoxelContext renderInfo, int face)
	{
		int facingId = renderInfo.getSideId(face);
		Voxel facing = store.getVoxelById(facingId);
		Voxel voxel = renderInfo.getVoxel();

		if (voxel.getType().isLiquid() && !facing.getType().isLiquid())
			return true;
		
		//Facing.isSideOpaque
		if (/*!facing.getType().isOpaque() && */!facing.isFaceOpaque(VoxelSides.values()[face].getOppositeSide(), facingId) && (!voxel.sameKind(facing) || !voxel.getType().isSelfOpaque()))
			return true;
		return false;
	}
}
