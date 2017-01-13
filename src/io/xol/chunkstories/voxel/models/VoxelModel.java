package io.xol.chunkstories.voxel.models;

import io.xol.chunkstories.api.Content;
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
	
	public String name;

	public VoxelModel(Content.Voxels.VoxelModels store, String name)
	{
		this.store = store;
		this.name = name;
	}
	
	public String texturesNames[];
	public int texturesOffsets[];
	
	public boolean[][] culling;
	
	public float[] vertices;
	public float[] texCoords;
	public float[] normals;

	public float jitterX = 0;
	public float jitterY = 0;
	public float jitterZ = 0;
	
	/* (non-Javadoc)
	 * @see io.xol.chunkstories.voxel.models.VoxelRenderer#renderInto(io.xol.chunkstories.renderer.chunks.RenderByteBuffer, io.xol.chunkstories.renderer.BlockRenderInfo, io.xol.chunkstories.api.world.Chunk, int, int, int)
	 */
	@Override
	public int renderInto(VoxelBaker renderByteBuffer, VoxelContext info, Chunk chunk, int x, int y, int z)
	{
		int llMs = chunk.getSunLight(x, y, z);//getSunlight(c, x, y, z);
		int llMb = chunk.getBlockLight(x, y, z);//getBlocklight(c, x, y, z);

		float[] lightColors = ChunksRenderer.bakeLightColors(llMb, llMb, llMb, llMb, llMs, llMs, llMs, llMs);
		
		String voxelName = info.getVoxel().getName();
		
		int modelTextureIndex = 0;
		
		VoxelTexture texture = null;
		
		if(this.texturesNames[modelTextureIndex].equals("_top"))
			texture = info.getTexture(VoxelSides.TOP);
		else if(this.texturesNames[modelTextureIndex].equals("_bottom"))
			texture = info.getTexture(VoxelSides.BOTTOM);
		else if(this.texturesNames[modelTextureIndex].equals("_left"))
			texture = info.getTexture(VoxelSides.LEFT);
		else if(this.texturesNames[modelTextureIndex].equals("_right"))
			texture = info.getTexture(VoxelSides.RIGHT);
		else if(this.texturesNames[modelTextureIndex].equals("_front"))
			texture = info.getTexture(VoxelSides.FRONT);
		else if(this.texturesNames[modelTextureIndex].equals("_back"))
			texture = info.getTexture(VoxelSides.BACK);
		else
			texture = store.parent().textures().getVoxelTextureByName(this.texturesNames[modelTextureIndex].replace("~", voxelName));
		
		int useUntil = this.texturesOffsets[modelTextureIndex];
		int textureS = texture.atlasS;// +mod(sx,texture.textureScale)*offset;
		int textureT = texture.atlasT;// +mod(sz,texture.textureScale)*offset;

		Voxel occTest;

		boolean[] cullingCache = new boolean[6];
		for (int j = 0; j < 6; j++)
		{
			int id = VoxelFormat.id(info.neightborhood[j]);
			int meta = VoxelFormat.meta(info.neightborhood[j]);
			occTest = VoxelsStore.get().getVoxelById(id);
			// If it is, don't draw it.
			cullingCache[j] = (occTest.isVoxelOpaque() || occTest.isFaceOpaque(VoxelSides.values()[j], info.neightborhood[j])) || occTest.isFaceOpaque(VoxelSides.values()[j], info.neightborhood[j])
					|| (info.getVoxel().isVoxelOpaqueWithItself() && id == VoxelFormat.id(info.data) && meta == info.getMetaData());
			//System.out.println("generating culling cache for voxel "+VoxelFormat.id(info.data)+"y:"+sy+"model"+this.name+" cull:"+j+":"+cullingCache[j]);
		}

		float dx = 0f, dy = 0f, dz = 0f;
		if (this.jitterX != 0.0f)
			dx = (float) ((Math.random() * 2.0 - 1.0) * this.jitterX);
		if (this.jitterY != 0.0f)
			dy = (float) ((Math.random() * 2.0 - 1.0) * this.jitterY);
		if (this.jitterZ != 0.0f)
			dz = (float) ((Math.random() * 2.0 - 1.0) * this.jitterZ);

		for (int i = 0; i < this.vertices.length / 3; i++)
		{
			//vert = this.vertices[i];
			//tex = this.texCoords[i];
			//normal = this.normals[i];

			if(i >= useUntil)
			{
				modelTextureIndex++;
				
				if(this.texturesNames[modelTextureIndex].equals("_top"))
					texture = info.getTexture(VoxelSides.TOP);
				else if(this.texturesNames[modelTextureIndex].equals("_bottom"))
					texture = info.getTexture(VoxelSides.BOTTOM);
				else if(this.texturesNames[modelTextureIndex].equals("_left"))
					texture = info.getTexture(VoxelSides.LEFT);
				else if(this.texturesNames[modelTextureIndex].equals("_right"))
					texture = info.getTexture(VoxelSides.RIGHT);
				else if(this.texturesNames[modelTextureIndex].equals("_front"))
					texture = info.getTexture(VoxelSides.FRONT);
				else if(this.texturesNames[modelTextureIndex].equals("_back"))
					texture = info.getTexture(VoxelSides.BACK);
				else
					texture = store.parent().textures().getVoxelTextureByName(this.texturesNames[modelTextureIndex].replace("~", voxelName));
				
				/*if(!this.texturesNames[modelTextureIndex].equals("~"))
					texture = VoxelTextures.getVoxelTexture(this.texturesNames[modelTextureIndex].replace("~", voxelName));
				else
					texture = info.getTexture();*/
				useUntil = this.texturesOffsets[modelTextureIndex];
				textureS = texture.atlasS;// +mod(sx,texture.textureScale)*offset;
				textureT = texture.atlasT;// +mod(sz,texture.textureScale)*offset;
			}
			
			/*
			 * How culling works :
			 * culling[][] array contains [vertices.len/3][faces (6)] booleans
			 * for each triangle (vert/3) it checks for i 0 -> 6 that either ![v][i] or [v][i] && info.neightbours[i] is solid
			 * if any cull condition fails then it doesn't render this triangle.<
			 */
			int cullIndex = i / 3;
			boolean drawFace = true;
			for (int j = 0; j < 6; j++)
			{
				// Should check if face occluded ?
				if (this.culling[cullIndex][j])
				{
					/*int id = VoxelFormat.id(info.neightborhood[j]);
					int meta = VoxelFormat.meta(info.neightborhood[j]);
					occTest = VoxelTypes.get(id);
					// If it is, don't draw it.
					if(occTest.isVoxelOpaque() || (info.voxelType.isVoxelOpaqueWithItself() && id == VoxelFormat.id(info.data) && meta == info.getMetaData()))
						drawFace = false;*/

					if (cullingCache[j])
						drawFace = false;
				}
			}

			if (drawFace)
			{
				renderByteBuffer.addVerticeFloat(this.vertices[i*3+0] + x + dx, this.vertices[i*3+1] + y + dy, this.vertices[i*3+2] + z + dz);
				//vertices.add(new float[] { vert[0] + sx + dx, vert[1] + sy + dy, vert[2] + sz + dz });
				renderByteBuffer.addTexCoordInt((int) (textureS + this.texCoords[i*2+0] * texture.atlasOffset), (int) (textureT + this.texCoords[i*2+1] * texture.atlasOffset));
				//texcoords.add(new int[] { (int) (textureS + tex[0] * texture.atlasOffset), (int) (textureT + tex[1] * texture.atlasOffset) });
				renderByteBuffer.addColors(lightColors);
				//colors.add(lightColors);
				renderByteBuffer.addNormalsInt(ChunksRenderer.intifyNormal(this.normals[i*3+0]), ChunksRenderer.intifyNormal(this.normals[i*3+1]), ChunksRenderer.intifyNormal(this.normals[i*3+2]), info.isAffectedByWind());
				//normals.add(normal);
				//if (isWavy != null)
				//	isWavy.add(info.isWavy());
			}
			else
			{
				//Skip the 2 other vertices
				i += 2;
			}
		}
		
		return this.vertices.length;
	}

	public Content.Voxels.VoxelModels store()
	{
		return store;
	}
}
