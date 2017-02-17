package io.xol.chunkstories.api.voxel.models;

/**
 * Abstract.
 * Used to bake static voxel mesh into, each family of function must be called in order ( vert, tex, col, norm ) 3 times to form triangles
 */
public interface VoxelBakerCommon
{
	public void addTexCoordInt(int i0, int i1);

	public void addColors(float[] t);

	public void addColorsSpecial(float[] t, int extended);

	public void addColors(float f0, float f1, float f2);

	public void addColorsSpecial(float f0, float f1, float f2, int extended);

	public void addNormalsInt(int i0, int i1, int i2, byte extra);
}
