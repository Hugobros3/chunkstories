package io.xol.chunkstories.voxel.models;

import io.xol.chunkstories.api.Content;
import io.xol.chunkstories.api.Content.Voxels.VoxelModels;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.renderer.VoxelContext;
import io.xol.chunkstories.renderer.chunks.ChunksRenderer;
import io.xol.chunkstories.renderer.chunks.VoxelBaker;
import io.xol.chunkstories.voxel.VoxelTexture;
import io.xol.chunkstories.voxel.VoxelsStore;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class VoxelModel implements VoxelRenderer
{
	private final Content.Voxels.VoxelModels store;
	protected final String name;

	protected final float[] vertices;
	protected final float[] texCoords;
	protected final String texturesNames[];
	protected final int texturesOffsets[];
	protected final float[] normals;
	protected final byte[] extra;
	
	protected boolean[][] culling;

	protected final float jitterX;
	protected final float jitterY;
	protected final float jitterZ;

	public VoxelModel(VoxelModels store, String name, float[] vertices, float[] texCoords, String[] texturesNames, int[] texturesOffsets, float[] normals, byte[] extra, boolean[][] culling)
	{
		this(store, name, vertices, texCoords, texturesNames, texturesOffsets, normals, extra, culling, 0, 0, 0);
	}

	public VoxelModel(VoxelModels store, String name, float[] vertices, float[] texCoords, String[] texturesNames, int[] texturesOffsets, float[] normals, byte[] extra, boolean[][] culling, float jitterX, float jitterY, float jitterZ)
	{
		this.store = store;
		this.name = name;
		this.texturesNames = texturesNames;
		this.texturesOffsets = texturesOffsets;
		this.culling = culling;
		this.vertices = vertices;
		this.texCoords = texCoords;
		this.normals = normals;
		this.extra = extra;
		this.jitterX = jitterX;
		this.jitterY = jitterY;
		this.jitterZ = jitterZ;
	}
	
	public String getName()
	{
		return name;
	}
	
	/* (non-Javadoc)
	 * @see io.xol.chunkstories.voxel.models.VoxelRenderer#renderInto(io.xol.chunkstories.renderer.chunks.RenderByteBuffer, io.xol.chunkstories.renderer.BlockRenderInfo, io.xol.chunkstories.api.world.Chunk, int, int, int)
	 */
	@Override
	public int renderInto(VoxelBaker renderByteBuffer, VoxelContext info, Chunk chunk, int x, int y, int z)
	{
		int lightLevelSun = chunk.getSunLight(x, y, z);
		int lightLevelVoxel = chunk.getBlockLight(x, y, z);

		//Obtains the light amount (sun/voxel) for this model
		//TODO interpolation w/ neighbors
		float[] lightColors = ChunksRenderer.bakeLightColors(lightLevelVoxel, lightLevelVoxel, lightLevelVoxel, lightLevelVoxel, lightLevelSun, lightLevelSun, lightLevelSun, lightLevelSun);
		
		//We have an array of textures and we jump to the next based on baked offsets and vertice counting
		int modelTextureIndex = 0;
		VoxelTexture currentVoxelTexture = null;
		
		//Selects an appropriate texture
		currentVoxelTexture = selectsTextureFromIndex(info, modelTextureIndex);
		int maxVertexIndexToUseThisTextureFor = this.texturesOffsets[modelTextureIndex];
		
		//Actual coordinates in the Atlas
		int textureS = currentVoxelTexture.atlasS;
		int textureT = currentVoxelTexture.atlasT;

		//We look the 6 adjacent faces to determine wether or not we should consider them culled
		Voxel occlusionTestedVoxel;
		boolean[] cullingCache = new boolean[6];
		for (int j = 0; j < 6; j++)
		{
			int id = VoxelFormat.id(info.neightborhood[j]);
			int meta = VoxelFormat.meta(info.neightborhood[j]);
			occlusionTestedVoxel = VoxelsStore.get().getVoxelById(id);
			// If it is, don't draw it.
			cullingCache[j] = (occlusionTestedVoxel.isVoxelOpaque() || occlusionTestedVoxel.isFaceOpaque(VoxelSides.values()[j], info.neightborhood[j])) || occlusionTestedVoxel.isFaceOpaque(VoxelSides.values()[j], info.neightborhood[j])
					|| (info.getVoxel().isVoxelOpaqueWithItself() && id == VoxelFormat.id(info.data) && meta == info.getMetaData());
		}

		//Generate some jitter if it is enabled
		float dx = 0f, dy = 0f, dz = 0f;
		if (this.jitterX != 0.0f)
			dx = (float) ((Math.random() * 2.0 - 1.0) * this.jitterX);
		if (this.jitterY != 0.0f)
			dy = (float) ((Math.random() * 2.0 - 1.0) * this.jitterY);
		if (this.jitterZ != 0.0f)
			dz = (float) ((Math.random() * 2.0 - 1.0) * this.jitterZ);

		int drewVertices = 0;
		
		drawVertex:
		for (int i_currentVertex = 0; i_currentVertex < this.vertices.length / 3; i_currentVertex++)
		{
			if(i_currentVertex >= maxVertexIndexToUseThisTextureFor)
			{
				modelTextureIndex++;
				
				//Selects an appropriate texture
				currentVoxelTexture = selectsTextureFromIndex(info, modelTextureIndex);
				
				maxVertexIndexToUseThisTextureFor = this.texturesOffsets[modelTextureIndex];
				textureS = currentVoxelTexture.atlasS;// +mod(sx,texture.textureScale)*offset;
				textureT = currentVoxelTexture.atlasT;// +mod(sz,texture.textureScale)*offset;
			}
			
			/*
			 * How culling works :
			 * culling[][] array contains [vertices.len/3][faces (6)] booleans
			 * for each triangle (vert/3) it checks for i 0 -> 6 that either ![v][i] or [v][i] && info.neightbours[i] is solid
			 * if any cull condition fails then it doesn't render this triangle.<
			 */
			int cullIndex = i_currentVertex / 3;
			//boolean drawFace = true;
			for (int j = 0; j < 6; j++)
			{
				// Is culling enabled on this face, on this vertex ?
				if (this.culling[cullIndex][j])
				{
					//Oh shit it should - we skip the next two vertices already
					//TODO possible optimization : skip to the next culling spec label
					if (cullingCache[j])
					{
						i_currentVertex += 2;
						continue drawVertex;
					}
				}
			}
			
			renderByteBuffer.addVerticeFloat(this.vertices[i_currentVertex*3+0] + x + dx, this.vertices[i_currentVertex*3+1] + y + dy, this.vertices[i_currentVertex*3+2] + z + dz);
			renderByteBuffer.addTexCoordInt((int) (textureS + this.texCoords[i_currentVertex*2+0] * currentVoxelTexture.atlasOffset), (int) (textureT + this.texCoords[i_currentVertex*2+1] * currentVoxelTexture.atlasOffset));
			renderByteBuffer.addColors(lightColors);
			renderByteBuffer.addNormalsInt(ChunksRenderer.intifyNormal(this.normals[i_currentVertex*3+0]), ChunksRenderer.intifyNormal(this.normals[i_currentVertex*3+1]), ChunksRenderer.intifyNormal(this.normals[i_currentVertex*3+2]), this.extra[i_currentVertex]);
		
			drewVertices++;
		}
		
		return this.vertices.length;
	}

	private VoxelTexture selectsTextureFromIndex(VoxelContext info, int modelTextureIndex)
	{
		if(this.texturesNames[modelTextureIndex].equals("_top"))
			return info.getTexture(VoxelSides.TOP);
		else if(this.texturesNames[modelTextureIndex].equals("_bottom"))
			return info.getTexture(VoxelSides.BOTTOM);
		else if(this.texturesNames[modelTextureIndex].equals("_left"))
			return info.getTexture(VoxelSides.LEFT);
		else if(this.texturesNames[modelTextureIndex].equals("_right"))
			return info.getTexture(VoxelSides.RIGHT);
		else if(this.texturesNames[modelTextureIndex].equals("_front"))
			return info.getTexture(VoxelSides.FRONT);
		else if(this.texturesNames[modelTextureIndex].equals("_back"))
			return info.getTexture(VoxelSides.BACK);
		
		//If none of this bs is going on
		return store.parent().textures().getVoxelTextureByName(this.texturesNames[modelTextureIndex].replace("~", info.getVoxel().getName()));
	}

	public int getSizeInVertices()
	{
		return vertices.length / 3;
	}
	
	public boolean[][] getCulling()
	{
		return culling;
	}

	public void setCulling(boolean[][] culling)
	{
		this.culling = culling;
	}

	public Content.Voxels.VoxelModels getStore()
	{
		return store;
	}

	public String[] getTexturesNames()
	{
		return texturesNames;
	}

	public int[] getTexturesOffsets()
	{
		return texturesOffsets;
	}

	public float[] getVertices()
	{
		return vertices;
	}

	public float[] getTexCoords()
	{
		return texCoords;
	}

	public float[] getNormals()
	{
		return normals;
	}

	public byte[] getExtra()
	{
		return extra;
	}

	public float getJitterX()
	{
		return jitterX;
	}

	public float getJitterY()
	{
		return jitterY;
	}

	public float getJitterZ()
	{
		return jitterZ;
	}

	public Content.Voxels.VoxelModels store()
	{
		return store;
	}
}
