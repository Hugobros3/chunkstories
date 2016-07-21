package io.xol.chunkstories.renderer.chunks;

public interface VoxelBaker
{

	void addVerticeInt(int i0, int i1, int i2);

	void addVerticeFloat(float f0, float f1, float f2);

	void addTexCoordInt(int i0, int i1);

	void addColors(float[] t);

	void addColorsSpecial(float[] t, int extended);

	void addColors(float f0, float f1, float f2);

	void addColorsSpecial(float f0, float f1, float f2, int extended);

	void addNormalsInt(int i0, int i1, int i2, boolean wavy);

}