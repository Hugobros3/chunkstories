package io.xol.chunkstories.api.world;

import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.voxel.models.VoxelRenderer;
import io.xol.chunkstories.api.voxel.textures.VoxelTexture;

public interface VoxelContext
{
	public Voxel getVoxel();
	
	public int getData();

	public int getNeightborData(int side);

	public default int getSideId(int side) {
		return VoxelFormat.id(getNeightborData(side));
	}

	public default VoxelRenderer getVoxelRenderer() {
		Voxel voxel = getVoxel();
		return voxel != null ? voxel.getVoxelRenderer(this) : null;
	}

	public default VoxelTexture getTexture(VoxelSides side) {
		Voxel voxel = getVoxel();
		return voxel != null ? voxel.getVoxelTexture(getData(), side, this) : null;
	}

	public default int getMetaData() {
		return VoxelFormat.meta(getData());
	}
}