package io.xol.chunkstories.renderer.chunks;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Used to bake static voxel mesh into, each family of function must be called in order ( vert, tex, col, norm ) 3 times to form triangles
 */
public interface VoxelBaker
{
	void addVerticeInt(int i0, int i1, int i2);

	void addVerticeFloat(float f0, float f1, float f2);

	void addTexCoordInt(int i0, int i1);

	void addColors(float[] t);

	void addColorsSpecial(float[] t, int extended);

	void addColors(float f0, float f1, float f2);

	void addColorsSpecial(float f0, float f1, float f2, int extended);

	void addNormalsInt(int i0, int i1, int i2, byte extra);
}