package io.xol.chunkstories.core.voxel;

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
import io.xol.chunkstories.voxel.VoxelsStore;

public class DefaultVoxelRenderer implements VoxelRenderer
{
	@Override
	public int renderInto(ChunkRenderer chunkRenderer, ChunkRenderContext bakingContext, Chunk chunk, VoxelContext voxelInformations)
	{
		Voxel vox = voxelInformations.getVoxel();
		int src = voxelInformations.getData();
		
		int i = voxelInformations.getX() & 0x1F;
		int k = voxelInformations.getY() & 0x1F;
		int j = voxelInformations.getZ() & 0x1F;
		
		VoxelBakerCubic rawRBBF = chunkRenderer.getLowpolyBakerFor(LodLevel.ANY, ShadingType.OPAQUE);
		byte extraByte = 0;
		if (shallBuildWallArround(voxelInformations, 5))
		{
			if (k != 0 || bakingContext.isBottomChunkLoaded())
				addQuadBottom(chunk, bakingContext, rawRBBF, i, k, j, vox.getVoxelTexture(src, VoxelSides.BOTTOM, voxelInformations), extraByte);
		}
		if (shallBuildWallArround(voxelInformations, 4))
		{
			if (k != 31 || bakingContext.isTopChunkLoaded())
				addQuadTop(chunk, bakingContext, rawRBBF, i, k, j, vox.getVoxelTexture(src, VoxelSides.TOP, voxelInformations), extraByte);
		}
		if (shallBuildWallArround(voxelInformations, 2))
		{
			if (i != 31 || bakingContext.isRightChunkLoaded())
				addQuadRight(chunk, bakingContext, rawRBBF, i, k, j, vox.getVoxelTexture(src, VoxelSides.RIGHT, voxelInformations), extraByte);
		}
		if (shallBuildWallArround(voxelInformations, 0))
		{
			if (i != 0 || bakingContext.isLeftChunkLoaded())
				addQuadLeft(chunk, bakingContext, rawRBBF, i, k, j, vox.getVoxelTexture(src, VoxelSides.LEFT, voxelInformations), extraByte);
		}
		if (shallBuildWallArround(voxelInformations, 1))
		{
			if (j != 31 || bakingContext.isFrontChunkLoaded())
				addQuadFront(chunk, bakingContext, rawRBBF, i, k, j, vox.getVoxelTexture(src, VoxelSides.FRONT, voxelInformations), extraByte);
		}
		if (shallBuildWallArround(voxelInformations, 3))
		{
			if (j != 0 || bakingContext.isBackChunkLoaded())
				addQuadBack(chunk, bakingContext, rawRBBF, i, k, j, vox.getVoxelTexture(src, VoxelSides.BACK, voxelInformations), extraByte);
		}

		return 0;
	}

	protected void addQuadTop(Chunk c, ChunkRenderContext bakingContext, VoxelBakerCubic rbbf, int sx, int sy, int sz, VoxelTexture texture, byte wavy)
	{
		/*int llMs = getSunlight(c, sx, sy + 1, sz);
		int llMb = getBlocklight(c, sx, sy + 1, sz);

		int llAs = getSunlight(c, sx + 1, sy + 1, sz);
		int llBs = getSunlight(c, sx + 1, sy + 1, sz + 1);
		int llCs = getSunlight(c, sx, sy + 1, sz + 1);
		int llDs = getSunlight(c, sx - 1, sy + 1, sz + 1);

		int llEs = getSunlight(c, sx - 1, sy + 1, sz);
		int llFs = getSunlight(c, sx - 1, sy + 1, sz - 1);
		int llGs = getSunlight(c, sx, sy + 1, sz - 1);
		int llHs = getSunlight(c, sx + 1, sy + 1, sz - 1);

		int llAb = getBlocklight(c, sx + 1, sy + 1, sz);
		int llBb = getBlocklight(c, sx + 1, sy + 1, sz + 1);
		int llCb = getBlocklight(c, sx, sy + 1, sz + 1);
		int llDb = getBlocklight(c, sx - 1, sy + 1, sz + 1);

		int llEb = getBlocklight(c, sx - 1, sy + 1, sz);
		int llFb = getBlocklight(c, sx - 1, sy + 1, sz - 1);
		int llGb = getBlocklight(c, sx, sy + 1, sz - 1);
		int llHb = getBlocklight(c, sx + 1, sy + 1, sz - 1);

		float[] aoA = new float[] { 1f, 1f, 1f };
		float[] aoB = new float[] { 1f, 1f, 1f };
		float[] aoC = new float[] { 1f, 1f, 1f };
		float[] aoD = new float[] { 1f, 1f, 1f };

		// float amB = (llCb+llBb+llAb+llMb)/15f/4f;
		// float amS = (llCs+llBs+llAs+llMs)/15f/4f;
		aoA = bakeLightColors(llCb, llBb, llAb, llMb, llCs, llBs, llAs, llMs);
		// aoA = blendLights(amB,amS);

		// amB = (llCb+llDb+llEb+llMb)/15f/4f;
		// amS = (llCs+llDs+llEs+llMs)/15f/4f;
		aoD = bakeLightColors(llCb, llDb, llEb, llMb, llCs, llDs, llEs, llMs);
		// aoD = bakeLightColors(llCb, llDb, llEb, llMb, llCs, llDs, llEs,
		// llMs);

		// amB = (llGb+llHb+llAb+llMb)/15f/4f;
		// amS = (llGs+llHs+llAs+llMs)/15f/4f;
		aoB = bakeLightColors(llGb, llHb, llAb, llMb, llGs, llHs, llAs, llMs);
		// aoB = bakeLightColors(llGb, llHb, llAb ,llMb, llGs, llHs, llAs,
		// llMs);

		// amB = (llEb+llFb+llGb+llMb)/15f/4f;
		// amS = (llEs+llFs+llGs+llMs)/15f/4f;
		aoC = bakeLightColors(llEb, llFb, llGb, llMb, llEs, llFs, llGs, llMs);
		// aoC = bakeLightColors(llEb, llFb, llGb, llMb, llEs, llFs, llGs,
		// llMs);

		// float s = (llMs)/15f;
		// aoA = aoB = aoC = aoD = new float[]{s,s,s};
*/
		int offset = texture.getAtlasOffset() / texture.getTextureScale();
		int textureS = texture.getAtlasS() + (sx % texture.getTextureScale()) * offset;
		int textureT = texture.getAtlasT() + (sz % texture.getTextureScale()) * offset;
		
		rbbf.addVerticeInt(sx, sy + 1, sz);
		rbbf.addTexCoordInt(textureS, textureT);
		rbbf.addColorsAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_BACK_LEFT);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 1023 /* intifyNormal(1) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx + 1, sy + 1, sz + 1);
		rbbf.addTexCoordInt(textureS + offset, textureT + offset);
		rbbf.addColorsAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_FRONT_RIGHT);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 1023 /* intifyNormal(1) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx + 1, sy + 1, sz);
		rbbf.addTexCoordInt(textureS + offset, textureT);
		rbbf.addColorsAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_BACK_RIGHT);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 1023 /* intifyNormal(1) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx, sy + 1, sz + 1);
		rbbf.addTexCoordInt(textureS, textureT + offset);
		rbbf.addColorsAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_FRONT_LEFT);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 1023 /* intifyNormal(1) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx + 1, sy + 1, sz + 1);
		rbbf.addTexCoordInt(textureS + offset, textureT + offset);
		rbbf.addColorsAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_FRONT_RIGHT);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 1023 /* intifyNormal(1) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx, sy + 1, sz);
		rbbf.addTexCoordInt(textureS, textureT);
		rbbf.addColorsAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_BACK_LEFT);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 1023 /* intifyNormal(1) */, 511 /* intifyNormal(0) */, wavy);
	}

	protected void addQuadBottom(Chunk c, ChunkRenderContext bakingContext, VoxelBakerCubic rbbf, int sx, int sy, int sz, VoxelTexture texture, byte wavy)
	{
		/*int llMs = getSunlight(c, sx, sy - 1, sz);
		int llMb = getBlocklight(c, sx, sy - 1, sz);

		int llAb = getBlocklight(c, sx + 1, sy - 1, sz);
		int llBb = getBlocklight(c, sx + 1, sy - 1, sz + 1);
		int llCb = getBlocklight(c, sx, sy - 1, sz + 1);
		int llDb = getBlocklight(c, sx - 1, sy - 1, sz + 1);

		int llEb = getBlocklight(c, sx - 1, sy - 1, sz);
		int llFb = getBlocklight(c, sx - 1, sy - 1, sz - 1);
		int llGb = getBlocklight(c, sx, sy - 1, sz - 1);
		int llHb = getBlocklight(c, sx + 1, sy - 1, sz - 1);

		int llAs = getSunlight(c, sx + 1, sy - 1, sz);
		int llBs = getSunlight(c, sx + 1, sy - 1, sz + 1);
		int llCs = getSunlight(c, sx, sy - 1, sz + 1);
		int llDs = getSunlight(c, sx - 1, sy - 1, sz + 1);

		int llEs = getSunlight(c, sx - 1, sy - 1, sz);
		int llFs = getSunlight(c, sx - 1, sy - 1, sz - 1);
		int llGs = getSunlight(c, sx, sy - 1, sz - 1);
		int llHs = getSunlight(c, sx + 1, sy - 1, sz - 1);

		float[] aoA = new float[] { 1f, 1f, 1f };
		float[] aoB = new float[] { 1f, 1f, 1f };
		float[] aoC = new float[] { 1f, 1f, 1f };
		float[] aoD = new float[] { 1f, 1f, 1f };

		aoA = bakeLightColors(llCb, llBb, llAb, llMb, llCs, llBs, llAs, llMs);

		aoD = bakeLightColors(llCb, llDb, llEb, llMb, llCs, llDs, llEs, llMs);

		aoB = bakeLightColors(llGb, llHb, llAb, llMb, llGs, llHs, llAs, llMs);

		aoC = bakeLightColors(llEb, llFb, llGb, llMb, llEs, llFs, llGs, llMs);*/

		int offset = texture.getAtlasOffset() / texture.getTextureScale();
		int textureS = texture.getAtlasS() + (sx % texture.getTextureScale()) * offset;
		int textureT = texture.getAtlasT() + (sz % texture.getTextureScale()) * offset;

		rbbf.addVerticeInt(sx + 1, sy, sz);
		rbbf.addTexCoordInt(textureS, textureT);
		rbbf.addColorsAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_BACK_RIGHT);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 0 /* intifyNormal(-1) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx + 1, sy, sz + 1);
		rbbf.addTexCoordInt(textureS, textureT + offset);
		rbbf.addColorsAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_FRONT_RIGHT);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 0 /* intifyNormal(-1) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx, sy, sz);
		rbbf.addTexCoordInt(textureS + offset, textureT);
		rbbf.addColorsAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_BACK_LEFT);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 0 /* intifyNormal(-1) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx, sy, sz);
		rbbf.addTexCoordInt(textureS + offset, textureT);
		rbbf.addColorsAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_BACK_LEFT);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 0 /* intifyNormal(-1) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx + 1, sy, sz + 1);
		rbbf.addTexCoordInt(textureS, textureT + offset);
		rbbf.addColorsAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_FRONT_RIGHT);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 0 /* intifyNormal(-1) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx, sy, sz + 1);
		rbbf.addTexCoordInt(textureS + offset, textureT + offset);
		rbbf.addColorsAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_FRONT_LEFT);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 0 /* intifyNormal(-1) */, 511 /* intifyNormal(0) */, wavy);
	}

	protected void addQuadRight(Chunk c, ChunkRenderContext bakingContext, VoxelBakerCubic rbbf, int sx, int sy, int sz, VoxelTexture texture, byte wavy)
	{
		// ++x for dekal

		// +1 -1 0
		/*int llMs = getSunlight(c, sx, sy, sz);
		int llMb = getBlocklight(c, sx, sy, sz);

		int llAs = getSunlight(c, sx, sy + 1, sz); // ok
		int llBs = getSunlight(c, sx, sy + 1, sz + 1); // 1 1
		int llCs = getSunlight(c, sx, sy, sz + 1); // . 1
		int llDs = getSunlight(c, sx, sy - 1, sz + 1); // -1 1

		int llEs = getSunlight(c, sx, sy - 1, sz); // -1 .
		int llFs = getSunlight(c, sx, sy - 1, sz - 1); // -1 -1
		int llGs = getSunlight(c, sx, sy, sz - 1); // ok
		int llHs = getSunlight(c, sx, sy + 1, sz - 1); // 1 -1

		int llAb = getBlocklight(c, sx, sy + 1, sz); // ok
		int llBb = getBlocklight(c, sx, sy + 1, sz + 1); // 1 1
		int llCb = getBlocklight(c, sx, sy, sz + 1); // . 1
		int llDb = getBlocklight(c, sx, sy - 1, sz + 1); // -1 1

		int llEb = getBlocklight(c, sx, sy - 1, sz); // -1 .
		int llFb = getBlocklight(c, sx, sy - 1, sz - 1); // -1 -1
		int llGb = getBlocklight(c, sx, sy, sz - 1); // ok
		int llHb = getBlocklight(c, sx, sy + 1, sz - 1); // 1 -1
		float[] aoA = new float[] { 1f, 1f, 1f };
		float[] aoB = new float[] { 1f, 1f, 1f };
		float[] aoC = new float[] { 1f, 1f, 1f };
		float[] aoD = new float[] { 1f, 1f, 1f };

		aoA = bakeLightColors(llCb, llBb, llAb, llMb, llCs, llBs, llAs, llMs);

		aoD = bakeLightColors(llCb, llDb, llEb, llMb, llCs, llDs, llEs, llMs);

		aoB = bakeLightColors(llGb, llHb, llAb, llMb, llGs, llHs, llAs, llMs);

		aoC = bakeLightColors(llEb, llFb, llGb, llMb, llEs, llFs, llGs, llMs);*/

		int offset = texture.getAtlasOffset() / texture.getTextureScale();
		int textureS = texture.getAtlasS() + mod(sz, texture.getTextureScale()) * offset;
		int textureT = texture.getAtlasT() + mod(-sy, texture.getTextureScale()) * offset;

		rbbf.addVerticeInt(sx + 1, sy + 1, sz);
		rbbf.addTexCoordInt(textureS + offset, textureT);
		rbbf.addColorsAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_BACK_RIGHT);
		rbbf.addNormalsInt(1023 /* intifyNormal(1) */, 511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx + 1, sy + 1, sz + 1);
		rbbf.addTexCoordInt(textureS, textureT);
		rbbf.addColorsAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_FRONT_RIGHT);
		rbbf.addNormalsInt(1023 /* intifyNormal(1) */, 511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx + 1, sy - 0, sz);
		rbbf.addTexCoordInt(textureS + offset, textureT + offset);
		rbbf.addColorsAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_BACK_RIGHT);
		rbbf.addNormalsInt(1023 /* intifyNormal(1) */, 511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx + 1, sy - 0, sz);
		rbbf.addTexCoordInt(textureS + offset, textureT + offset);
		rbbf.addColorsAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_BACK_RIGHT);
		rbbf.addNormalsInt(1023 /* intifyNormal(1) */, 511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx + 1, sy + 1, sz + 1);
		rbbf.addTexCoordInt(textureS, textureT);
		rbbf.addColorsAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_FRONT_RIGHT);
		rbbf.addNormalsInt(1023 /* intifyNormal(1) */, 511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx + 1, sy - 0, sz + 1);
		rbbf.addTexCoordInt(textureS, textureT + offset);
		rbbf.addColorsAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_FRONT_RIGHT);
		rbbf.addNormalsInt(1023 /* intifyNormal(1) */, 511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, wavy);
	}

	private int mod(int a, int b)
	{
		int c = a % b;
		if (c >= 0)
			return c;
		return c += b;
	}

	protected void addQuadLeft(Chunk c, ChunkRenderContext bakingContext, VoxelBakerCubic rbbf, int sx, int sy, int sz, VoxelTexture texture, byte wavy)
	{
		/*int llMs = getSunlight(c, sx - 1, sy, sz);
		int llMb = getBlocklight(c, sx - 1, sy, sz);

		int llAs = getSunlight(c, sx - 1, sy + 1, sz); // 1 .
		int llBs = getSunlight(c, sx - 1, sy + 1, sz + 1); // 1 1
		int llCs = getSunlight(c, sx - 1, sy, sz + 1); // . 1
		int llDs = getSunlight(c, sx - 1, sy - 1, sz + 1); // -1 1

		int llEs = getSunlight(c, sx - 1, sy - 1, sz); // -1 .
		int llFs = getSunlight(c, sx - 1, sy - 1, sz - 1); // -1 -1
		int llGs = getSunlight(c, sx - 1, sy, sz - 1); // . -1
		int llHs = getSunlight(c, sx - 1, sy + 1, sz - 1); // 1 -1

		int llAb = getBlocklight(c, sx - 1, sy + 1, sz); // 1 .
		int llBb = getBlocklight(c, sx - 1, sy + 1, sz + 1); // 1 1
		int llCb = getBlocklight(c, sx - 1, sy, sz + 1); // . 1
		int llDb = getBlocklight(c, sx - 1, sy - 1, sz + 1); // -1 1

		int llEb = getBlocklight(c, sx - 1, sy - 1, sz); // -1 .
		int llFb = getBlocklight(c, sx - 1, sy - 1, sz - 1); // -1 -1
		int llGb = getBlocklight(c, sx - 1, sy, sz - 1); // . -1
		int llHb = getBlocklight(c, sx - 1, sy + 1, sz - 1); // 1 -1

		float[] aoA = new float[] { 1f, 1f, 1f };
		float[] aoB = new float[] { 1f, 1f, 1f };
		float[] aoC = new float[] { 1f, 1f, 1f };
		float[] aoD = new float[] { 1f, 1f, 1f };

		aoA = bakeLightColors(llCb, llBb, llAb, llMb, llCs, llBs, llAs, llMs);
		// aoA = blendLights(amB,amS);

		aoD = bakeLightColors(llCb, llDb, llEb, llMb, llCs, llDs, llEs, llMs);

		aoB = bakeLightColors(llGb, llHb, llAb, llMb, llGs, llHs, llAs, llMs);

		aoC = bakeLightColors(llEb, llFb, llGb, llMb, llEs, llFs, llGs, llMs);*/

		int offset = texture.getAtlasOffset() / texture.getTextureScale();
		int textureS = texture.getAtlasS() + mod(sz, texture.getTextureScale()) * offset;
		int textureT = texture.getAtlasT() + mod(-sy, texture.getTextureScale()) * offset;

		rbbf.addVerticeInt(sx, sy - 0, sz);
		rbbf.addTexCoordInt(textureS, textureT + offset);
		rbbf.addColorsAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_BACK_LEFT);
		rbbf.addNormalsInt(0 /* intifyNormal(-1) */, 511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx, sy + 1, sz + 1);
		rbbf.addTexCoordInt(textureS + offset, textureT);
		rbbf.addColorsAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_FRONT_LEFT);
		rbbf.addNormalsInt(0 /* intifyNormal(-1) */, 511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx, sy + 1, sz);
		rbbf.addTexCoordInt(textureS, textureT);
		rbbf.addColorsAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_BACK_LEFT);
		rbbf.addNormalsInt(0 /* intifyNormal(-1) */, 511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx, sy - 0, sz + 1);
		rbbf.addTexCoordInt(textureS + offset, textureT + offset);
		rbbf.addColorsAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_FRONT_LEFT);
		rbbf.addNormalsInt(0 /* intifyNormal(-1) */, 511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx, sy + 1, sz + 1);
		rbbf.addTexCoordInt(textureS + offset, textureT);
		rbbf.addColorsAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_FRONT_LEFT);
		rbbf.addNormalsInt(0 /* intifyNormal(-1) */, 511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx, sy - 0, sz);
		rbbf.addTexCoordInt(textureS, textureT + offset);
		rbbf.addColorsAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_BACK_LEFT);
		rbbf.addNormalsInt(0 /* intifyNormal(-1) */, 511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, wavy);

	}

	protected void addQuadFront(Chunk c, ChunkRenderContext bakingContext, VoxelBakerCubic rbbf, int sx, int sy, int sz, VoxelTexture texture, byte wavy)
	{
		/*int llMs = getSunlight(c, sx, sy, sz);
		int llMb = getBlocklight(c, sx, sy, sz);

		int llAs = getSunlight(c, sx, sy + 1, sz); // 1 .
		int llBs = getSunlight(c, sx + 1, sy + 1, sz); // 1 1
		int llCs = getSunlight(c, sx + 1, sy, sz); // . 1
		int llDs = getSunlight(c, sx + 1, sy - 1, sz); // -1 1

		int llEs = getSunlight(c, sx, sy - 1, sz); // -1 .
		int llFs = getSunlight(c, sx - 1, sy - 1, sz); // -1 -1
		int llGs = getSunlight(c, sx - 1, sy, sz); // . -1
		int llHs = getSunlight(c, sx - 1, sy + 1, sz); // 1 -1

		int llAb = getBlocklight(c, sx, sy + 1, sz); // 1 .
		int llBb = getBlocklight(c, sx + 1, sy + 1, sz); // 1 1
		int llCb = getBlocklight(c, sx + 1, sy, sz); // . 1
		int llDb = getBlocklight(c, sx + 1, sy - 1, sz); // -1 1

		int llEb = getBlocklight(c, sx, sy - 1, sz); // -1 .
		int llFb = getBlocklight(c, sx - 1, sy - 1, sz); // -1 -1
		int llGb = getBlocklight(c, sx - 1, sy, sz); // . -1
		int llHb = getBlocklight(c, sx - 1, sy + 1, sz); // 1 -1

		float[] aoA = new float[] { 1f, 1f, 1f };
		float[] aoB = new float[] { 1f, 1f, 1f };
		float[] aoC = new float[] { 1f, 1f, 1f };
		float[] aoD = new float[] { 1f, 1f, 1f };

		aoA = bakeLightColors(llCb, llBb, llAb, llMb, llCs, llBs, llAs, llMs);
		// aoA = blendLights(amB,amS);

		aoD = bakeLightColors(llCb, llDb, llEb, llMb, llCs, llDs, llEs, llMs);

		aoB = bakeLightColors(llGb, llHb, llAb, llMb, llGs, llHs, llAs, llMs);

		aoC = bakeLightColors(llEb, llFb, llGb, llMb, llEs, llFs, llGs, llMs);*/

		int offset = texture.getAtlasOffset() / texture.getTextureScale();
		int textureS = texture.getAtlasS() + mod(sx, texture.getTextureScale()) * offset;
		int textureT = texture.getAtlasT() + mod(-sy, texture.getTextureScale()) * offset;

		rbbf.addVerticeInt(sx, sy - 0, sz + 1);
		rbbf.addTexCoordInt(textureS, textureT + offset);
		rbbf.addColorsAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_FRONT_LEFT);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, 1023 /* intifyNormal(1) */, wavy);

		rbbf.addVerticeInt(sx + 1, sy + 1, sz + 1);
		rbbf.addTexCoordInt(textureS + offset, textureT);
		rbbf.addColorsAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_FRONT_RIGHT);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, 1023 /* intifyNormal(1) */, wavy);

		rbbf.addVerticeInt(sx, sy + 1, sz + 1);
		rbbf.addTexCoordInt(textureS, textureT);
		rbbf.addColorsAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_FRONT_LEFT);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, 1023 /* intifyNormal(1) */, wavy);

		rbbf.addVerticeInt(sx + 1, sy - 0, sz + 1);
		rbbf.addTexCoordInt(textureS + offset, textureT + offset);
		rbbf.addColorsAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_FRONT_RIGHT);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, 1023 /* intifyNormal(1) */, wavy);

		rbbf.addVerticeInt(sx + 1, sy + 1, sz + 1);
		rbbf.addTexCoordInt(textureS + offset, textureT);
		rbbf.addColorsAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_FRONT_RIGHT);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, 1023 /* intifyNormal(1) */, wavy);

		rbbf.addVerticeInt(sx, sy - 0, sz + 1);
		rbbf.addTexCoordInt(textureS, textureT + offset);
		rbbf.addColorsAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_FRONT_LEFT);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, 1023 /* intifyNormal(1) */, wavy);

	}

	protected void addQuadBack(Chunk c, ChunkRenderContext bakingContext, VoxelBakerCubic rbbf, int sx, int sy, int sz, VoxelTexture texture, byte wavy)
	{

		/*int llMs = getSunlight(c, sx, sy, sz - 1);
		int llMb = getBlocklight(c, sx, sy, sz - 1);

		int llAs = getSunlight(c, sx, sy + 1, sz - 1); // 1 .
		int llBs = getSunlight(c, sx + 1, sy + 1, sz - 1); // 1 1
		int llCs = getSunlight(c, sx + 1, sy, sz - 1); // . 1
		int llDs = getSunlight(c, sx + 1, sy - 1, sz - 1); // -1 1

		int llEs = getSunlight(c, sx, sy - 1, sz - 1); // -1 .
		int llFs = getSunlight(c, sx - 1, sy - 1, sz - 1); // -1 -1
		int llGs = getSunlight(c, sx - 1, sy, sz - 1); // . -1
		int llHs = getSunlight(c, sx - 1, sy + 1, sz - 1); // 1 -1

		int llAb = getBlocklight(c, sx, sy + 1, sz - 1); // 1 .
		int llBb = getBlocklight(c, sx + 1, sy + 1, sz - 1); // 1 1
		int llCb = getBlocklight(c, sx + 1, sy, sz - 1); // . 1
		int llDb = getBlocklight(c, sx + 1, sy - 1, sz - 1); // -1 1

		int llEb = getBlocklight(c, sx, sy - 1, sz - 1); // -1 .
		int llFb = getBlocklight(c, sx - 1, sy - 1, sz - 1); // -1 -1
		int llGb = getBlocklight(c, sx - 1, sy, sz - 1); // . -1
		int llHb = getBlocklight(c, sx - 1, sy + 1, sz - 1); // 1 -1

		float[] aoA = new float[] { 1f, 1f, 1f };
		float[] aoB = new float[] { 1f, 1f, 1f };
		float[] aoC = new float[] { 1f, 1f, 1f };
		float[] aoD = new float[] { 1f, 1f, 1f };

		aoA = bakeLightColors(llCb, llBb, llAb, llMb, llCs, llBs, llAs, llMs);
		// aoA = blendLights(amB,amS);

		aoD = bakeLightColors(llCb, llDb, llEb, llMb, llCs, llDs, llEs, llMs);

		aoB = bakeLightColors(llGb, llHb, llAb, llMb, llGs, llHs, llAs, llMs);

		aoC = bakeLightColors(llEb, llFb, llGb, llMb, llEs, llFs, llGs, llMs);*/

		int offset = texture.getAtlasOffset() / texture.getTextureScale();
		int textureS = texture.getAtlasS() + mod(sx, texture.getTextureScale()) * offset;
		int textureT = texture.getAtlasT() + mod(-sy, texture.getTextureScale()) * offset;

		rbbf.addVerticeInt(sx, sy + 1, sz);
		rbbf.addTexCoordInt(textureS + offset, textureT);
		rbbf.addColorsAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_BACK_LEFT);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, 0 /* intifyNormal(-1) */, wavy);

		rbbf.addVerticeInt(sx + 1, sy + 1, sz);
		rbbf.addTexCoordInt(textureS, textureT);
		rbbf.addColorsAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_BACK_RIGHT);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, 0 /* intifyNormal(-1) */, wavy);

		rbbf.addVerticeInt(sx, sy - 0, sz);
		rbbf.addTexCoordInt(textureS + offset, textureT + offset);
		rbbf.addColorsAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_BACK_LEFT);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, 0 /* intifyNormal(-1) */, wavy);

		rbbf.addVerticeInt(sx, sy - 0, sz);
		rbbf.addTexCoordInt(textureS + offset, textureT + offset);
		rbbf.addColorsAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_BACK_LEFT);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, 0 /* intifyNormal(-1) */, wavy);

		rbbf.addVerticeInt(sx + 1, sy + 1, sz);
		rbbf.addTexCoordInt(textureS, textureT);
		rbbf.addColorsAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_BACK_RIGHT);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, 0 /* intifyNormal(-1) */, wavy);

		rbbf.addVerticeInt(sx + 1, sy - 0, sz);
		rbbf.addTexCoordInt(textureS, textureT + offset);
		rbbf.addColorsAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_BACK_RIGHT);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, 0 /* intifyNormal(-1) */, wavy);
	}

	protected boolean shallBuildWallArround(VoxelContext renderInfo, int face)
	{
		//int baseID = renderInfo.data;
		Voxel facing = VoxelsStore.get().getVoxelById(renderInfo.getSideId(face));
		Voxel voxel = renderInfo.getVoxel();

		if (voxel.getType().isLiquid() && !facing.getType().isLiquid())
			return true;
		if (!facing.getType().isOpaque() && (!voxel.sameKind(facing) || !voxel.getType().isSelfOpaque()))
			return true;
		return false;
	}

	public static float[] bakeLightColors(int bl1, int bl2, int bl3, int bl4, int sl1, int sl2, int sl3, int sl4)
	{
		float blocklightFactor = 0;

		float sunlightFactor = 0;

		float aoFactor = 4;

		if (sl1 >= 0) // If sunlight = -1 then it's a case of occlusion
		{
			blocklightFactor += bl1;
			sunlightFactor += sl1;
			aoFactor--;
		}
		if (sl2 >= 0)
		{
			blocklightFactor += bl2;
			sunlightFactor += sl2;
			aoFactor--;
		}
		if (sl3 >= 0)
		{
			blocklightFactor += bl3;
			sunlightFactor += sl3;
			aoFactor--;
		}
		if (sl4 >= 0)
		{
			blocklightFactor += bl4;
			sunlightFactor += sl4;
			aoFactor--;
		}
		if (aoFactor < 4) // If we're not 100% occlusion
		{
			blocklightFactor /= (4 - aoFactor);
			sunlightFactor /= (4 - aoFactor);
		}
		return new float[] { blocklightFactor / 15f, sunlightFactor / 15f, aoFactor / 4f };
	}
}
