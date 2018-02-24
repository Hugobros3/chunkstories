//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.voxel.models;

import io.xol.chunkstories.api.content.Content;
import io.xol.chunkstories.api.content.Content.Voxels.VoxelModels;
import io.xol.chunkstories.api.rendering.voxel.VoxelBakerHighPoly;
import io.xol.chunkstories.api.rendering.voxel.VoxelRenderer;
import io.xol.chunkstories.api.rendering.world.chunk.ChunkMeshDataSubtypes.LodLevel;
import io.xol.chunkstories.api.rendering.world.chunk.ChunkMeshDataSubtypes.ShadingType;
import io.xol.chunkstories.api.rendering.world.chunk.ChunkRenderer;
import io.xol.chunkstories.api.rendering.world.chunk.ChunkRenderer.ChunkRenderContext;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.voxel.models.VoxelModel;
import io.xol.chunkstories.api.voxel.textures.VoxelTexture;
import io.xol.chunkstories.api.world.cell.CellData;
import io.xol.chunkstories.api.world.chunk.Chunk;



public class VoxelModelLoaded implements VoxelRenderer, VoxelModel
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

	public VoxelModelLoaded(VoxelModels store, String name, float[] vertices, float[] texCoords, String[] texturesNames, int[] texturesOffsets, float[] normals, byte[] extra, boolean[][] culling)
	{
		this(store, name, vertices, texCoords, texturesNames, texturesOffsets, normals, extra, culling, 0, 0, 0);
	}

	public VoxelModelLoaded(VoxelModels store, String name, float[] vertices, float[] texCoords, String[] texturesNames, int[] texturesOffsets, float[] normals, byte[] extra, boolean[][] culling, float jitterX, float jitterY, float jitterZ)
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
	
	@Override
	public String getName()
	{
		return name;
	}
	
	public int renderInto(VoxelBakerHighPoly baker, ChunkRenderContext bakingContext, CellData info, Chunk chunk, int x, int y, int z)
	{
		//int lightLevelSun = chunk.getSunLight(x, y, z);
		//int lightLevelVoxel = chunk.getBlockLight(x, y, z);

		//Obtains the light amount (sun/voxel) for this model
		//TODO interpolation w/ neighbors
		//float[] lightColors = bakeLightColors(lightLevelVoxel, lightLevelVoxel, lightLevelVoxel, lightLevelVoxel, lightLevelSun, lightLevelSun, lightLevelSun, lightLevelSun);
		
		//We have an array of textures and we jump to the next based on baked offsets and vertice counting
		int modelTextureIndex = 0;
		VoxelTexture currentVoxelTexture = null;
		
		//Selects an appropriate texture
		currentVoxelTexture = selectsTextureFromIndex(info, modelTextureIndex);
		baker.usingTexture(currentVoxelTexture);
		
		int maxVertexIndexToUseThisTextureFor = this.texturesOffsets[modelTextureIndex];
		
		//Actual coordinates in the Atlas
		//int textureS = currentVoxelTexture.getAtlasS();
		//int textureT = currentVoxelTexture.getAtlasT();

		//We look the 6 adjacent faces to determine wether or not we should consider them culled
		boolean[] cullingCache = new boolean[6];
		for (int j = 0; j < 6; j++)
		{
			CellData adj = info.getNeightbor(j);
			
			// If it is, don't draw it.
			cullingCache[j] = adj.getVoxel().getDefinition().isOpaque() || 
					adj.getVoxel().isFaceOpaque(VoxelSides.values()[j], adj.getMetaData()) || 
					info.getVoxel().getDefinition().isSelfOpaque() && adj.getVoxel().sameKind(info.getVoxel()) && adj.getMetaData() == info.getMetaData();
		}

		//Generate some jitter if it is enabled
		float dx = 0f, dy = 0f, dz = 0f;
		if (this.jitterX != 0.0f)
			dx = (float) ((Math.random() * 2.0 - 1.0) * this.jitterX);
		if (this.jitterY != 0.0f)
			dy = (float) ((Math.random() * 2.0 - 1.0) * this.jitterY);
		if (this.jitterZ != 0.0f)
			dz = (float) ((Math.random() * 2.0 - 1.0) * this.jitterZ);

		//int drewVertices = 0;
		drawVertex:
		for (int i_currentVertex = 0; i_currentVertex < this.vertices.length / 3; i_currentVertex++)
		{
			if(i_currentVertex >= maxVertexIndexToUseThisTextureFor)
			{
				modelTextureIndex++;
				
				//Selects an appropriate texture
				currentVoxelTexture = selectsTextureFromIndex(info, modelTextureIndex);
				
				maxVertexIndexToUseThisTextureFor = this.texturesOffsets[modelTextureIndex];
				
				baker.usingTexture(currentVoxelTexture);
				//textureS = currentVoxelTexture.getAtlasS();// +mod(sx,texture.textureScale)*offset;
				//textureT = currentVoxelTexture.getAtlasT();// +mod(sz,texture.textureScale)*offset;
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
			
			float vertX = this.vertices[i_currentVertex*3+0];
			float vertY = this.vertices[i_currentVertex*3+1];
			float vertZ = this.vertices[i_currentVertex*3+2];
			
			baker.beginVertex(vertX + x + dx, vertY + y + dy, vertZ + z + dz);
			//renderByteBuffer.addVerticeFloat(vertX + x + dx, vertY + y + dy, vertZ + z + dz);
			
			baker.setTextureCoordinates(this.texCoords[i_currentVertex*2+0], this.texCoords[i_currentVertex*2+1]);
			//renderByteBuffer.addTexCoordInt((int) (textureS + this.texCoords[i_currentVertex*2+0] * currentVoxelTexture.getAtlasOffset()), (int) (textureT + this.texCoords[i_currentVertex*2+1] * currentVoxelTexture.getAtlasOffset()));
			
			byte sunLight, blockLight, ao;
			sunLight = bakingContext.getCurrentVoxelLighter().getSunlightLevelInterpolated(vertX, vertY, vertZ);
			blockLight = bakingContext.getCurrentVoxelLighter().getBlocklightLevelInterpolated(vertX, vertY, vertZ);
			ao = bakingContext.getCurrentVoxelLighter().getAoLevelInterpolated(vertX, vertY, vertZ);
			
			baker.setVoxelLight(sunLight, blockLight, ao);
			//renderByteBuffer.addColors(sunLight, blockLight, ao);
			
			baker.setNormal(normals[i_currentVertex*3+0], normals[i_currentVertex*3+1], normals[i_currentVertex*3+2]);
			//renderByteBuffer.addNormalsInt(intifyNormal(this.normals[i_currentVertex*3+0]), intifyNormal(this.normals[i_currentVertex*3+1]), intifyNormal(this.normals[i_currentVertex*3+2]), this.extra[i_currentVertex]);
		
			baker.endVertex();
			//drewVertices++;
		}
		
		return this.vertices.length;
	}
	
	/** Helper method to convert to engine internal format */
	public static int intifyNormal(float n)
	{
		return (int) ((n + 1) * 511.5f);
	}

	private VoxelTexture selectsTextureFromIndex(CellData info, int modelTextureIndex)
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
	
	@Override
	public int getSizeInVertices()
	{
		return vertices.length / 3;
	}
	
	@Override
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
	
	@Override
	public String[] getTexturesNames()
	{
		return texturesNames;
	}
	
	@Override
	public int[] getTexturesOffsets()
	{
		return texturesOffsets;
	}
	
	@Override
	public float[] getVertices()
	{
		return vertices;
	}
	
	@Override
	public float[] getTexCoords()
	{
		return texCoords;
	}
	
	@Override
	public float[] getNormals()
	{
		return normals;
	}
	
	@Override
	public byte[] getExtra()
	{
		return extra;
	}
	
	@Override
	public float getJitterX()
	{
		return jitterX;
	}
	
	@Override
	public float getJitterY()
	{
		return jitterY;
	}
	
	@Override
	public float getJitterZ()
	{
		return jitterZ;
	}
	
	@Override
	public Content.Voxels.VoxelModels store()
	{
		return store;
	}

	@Override
	public int bakeInto(ChunkRenderer chunkRenderer, ChunkRenderContext bakingContext, Chunk chunk, CellData cell)
	{
		VoxelBakerHighPoly renderByteBuffer = chunkRenderer.getHighpolyBakerFor(LodLevel.ANY, ShadingType.OPAQUE);
		return this.renderInto(renderByteBuffer, bakingContext, cell, chunk, cell.getX() & 0x1F, cell.getY() & 0x1F, cell.getZ() & 0x1F);
	}
}
