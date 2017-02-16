package io.xol.chunkstories.api.voxel.models;

import io.xol.chunkstories.api.Content;
import io.xol.chunkstories.api.world.VoxelContext;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.renderer.chunks.VoxelBaker;

public interface VoxelModel extends VoxelRenderer
{
	public String getName();

	public int renderInto(VoxelBaker renderByteBuffer, VoxelContext info, Chunk chunk, int x, int y, int z);

	public int getSizeInVertices();

	public boolean[][] getCulling();

	public String[] getTexturesNames();

	public int[] getTexturesOffsets();

	public float[] getVertices();

	public float[] getTexCoords();

	public float[] getNormals();

	public byte[] getExtra();

	public float getJitterX();

	public float getJitterY();

	public float getJitterZ();

	public Content.Voxels.VoxelModels store();

}