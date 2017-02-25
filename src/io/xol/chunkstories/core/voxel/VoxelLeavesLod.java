package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.voxel.VoxelType;
import io.xol.chunkstories.api.voxel.models.ChunkRenderer;
import io.xol.chunkstories.api.voxel.models.VoxelBakerCubic;
import io.xol.chunkstories.api.voxel.models.VoxelRenderer;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.LodLevel;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.ShadingType;
import io.xol.chunkstories.api.voxel.models.ChunkRenderer.ChunkRenderContext;
import io.xol.chunkstories.api.world.VoxelContext;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.voxel.VoxelsStore;

public class VoxelLeavesLod extends Voxel
{

	LodedLeavesBlocksRenderer renderer = new LodedLeavesBlocksRenderer();
	
	public VoxelLeavesLod(VoxelType type)
	{
		super(type);
	}
	
	@Override
	public VoxelRenderer getVoxelRenderer(VoxelContext info) {
		return renderer;
	}
	
	class LodedLeavesBlocksRenderer extends DefaultVoxelRenderer {
		
		@Override
		public int renderInto(ChunkRenderer chunkRenderer, ChunkRenderContext bakingContext, Chunk chunk, VoxelContext voxelInformations)
		{
			renderLodVersion(chunkRenderer, bakingContext, chunk, voxelInformations, LodLevel.LOW);
			renderLodVersion(chunkRenderer, bakingContext, chunk, voxelInformations, LodLevel.HIGH);
			return 0;
		}
		
		protected boolean shallBuildWallArround(VoxelContext renderInfo, int face, LodLevel lodLevel)
		{
			//int baseID = renderInfo.data;
			Voxel facing = VoxelsStore.get().getVoxelById(renderInfo.getSideId(face));
			Voxel voxel = renderInfo.getVoxel();

			if (voxel.getType().isLiquid() && !facing.getType().isLiquid())
				return true;
			if (!facing.getType().isOpaque() && ( (!voxel.sameKind(facing) || (lodLevel == LodLevel.HIGH && !voxel.getType().isSelfOpaque())) ) )
				return true;
			return false;
		}
		
		public void renderLodVersion(ChunkRenderer chunkRenderer, ChunkRenderContext bakingContext, Chunk chunk, VoxelContext voxelInformations, LodLevel lodLevel)
		{
			Voxel vox = voxelInformations.getVoxel();
			int src = voxelInformations.getData();
			
			int i = voxelInformations.getX() & 0x1F;
			int k = voxelInformations.getY() & 0x1F;
			int j = voxelInformations.getZ() & 0x1F;
			
			VoxelBakerCubic rawRBBF = chunkRenderer.getLowpolyBakerFor(lodLevel, ShadingType.OPAQUE);
			byte extraByte = 0;
			if (shallBuildWallArround(voxelInformations, 5, lodLevel))
			{
				if (k != 0 || bakingContext.isBottomChunkLoaded())
					addQuadBottom(chunk, bakingContext, rawRBBF, i, k, j, vox.getVoxelTexture(src, VoxelSides.BOTTOM, voxelInformations), extraByte);
			}
			if (shallBuildWallArround(voxelInformations, 4, lodLevel))
			{
				if (k != 31 || bakingContext.isTopChunkLoaded())
					addQuadTop(chunk, bakingContext, rawRBBF, i, k, j, vox.getVoxelTexture(src, VoxelSides.TOP, voxelInformations), extraByte);
			}
			if (shallBuildWallArround(voxelInformations, 2, lodLevel))
			{
				if (i != 31 || bakingContext.isRightChunkLoaded())
					addQuadRight(chunk, bakingContext, rawRBBF, i, k, j, vox.getVoxelTexture(src, VoxelSides.RIGHT, voxelInformations), extraByte);
			}
			if (shallBuildWallArround(voxelInformations, 0, lodLevel))
			{
				if (i != 0 || bakingContext.isLeftChunkLoaded())
					addQuadLeft(chunk, bakingContext, rawRBBF, i, k, j, vox.getVoxelTexture(src, VoxelSides.LEFT, voxelInformations), extraByte);
			}
			if (shallBuildWallArround(voxelInformations, 1, lodLevel))
			{
				if (j != 31 || bakingContext.isFrontChunkLoaded())
					addQuadFront(chunk, bakingContext, rawRBBF, i, k, j, vox.getVoxelTexture(src, VoxelSides.FRONT, voxelInformations), extraByte);
			}
			if (shallBuildWallArround(voxelInformations, 3, lodLevel))
			{
				if (j != 0 || bakingContext.isBackChunkLoaded())
					addQuadBack(chunk, bakingContext, rawRBBF, i, k, j, vox.getVoxelTexture(src, VoxelSides.BACK, voxelInformations), extraByte);
			}
		}
	}

}
