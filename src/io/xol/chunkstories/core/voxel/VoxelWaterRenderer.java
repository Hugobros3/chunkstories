package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.renderer.BlockRenderInfo;
import io.xol.chunkstories.renderer.chunks.ChunksRenderer;
import io.xol.chunkstories.renderer.chunks.VoxelBaker;
import io.xol.chunkstories.voxel.VoxelTexture;
import io.xol.chunkstories.voxel.VoxelTextures;
import io.xol.chunkstories.voxel.VoxelTypes;
import io.xol.chunkstories.voxel.models.VoxelModel;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class VoxelWaterRenderer extends VoxelModel
{

	public VoxelWaterRenderer(VoxelModel model)
	{
		super(model.name);
		
		//Copy attributes
		this.culling = model.culling;
		this.jitterX = model.jitterX;
		this.jitterY = model.jitterY;
		this.jitterZ = model.jitterZ;
		
		this.normals = model.normals;
		this.texCoords = model.texCoords;
		this.vertices = model.vertices;
		this.texturesNames = model.texturesNames;
		this.texturesOffsets = model.texturesOffsets;
	}

	@Override
	public int renderInto(VoxelBaker renderByteBuffer, BlockRenderInfo info, Chunk chunk, int x, int y, int z)
	{
		int llMs = chunk.getSunLight(x, y, z);//getSunlight(c, x, y, z);
		int llMb = chunk.getBlockLight(x, y, z);//getBlocklight(c, x, y, z);

		float[] lightColors = ChunksRenderer.bakeLightColors(llMb, llMb, llMb, llMb, llMs, llMs, llMs, llMs);
		
		int depth = 0;
		for(int i = 1; i < 16; i++)
		{
			int id = chunk.getWorld().getVoxelData(chunk.getChunkX() * 32 + x, chunk.getChunkY() * 32 + y - i, chunk.getChunkZ() * 32 + z);
			if(VoxelTypes.get(id) != null && VoxelTypes.get(id).isVoxelLiquid())
				depth++;
			else
				break;
		}
		
		String voxelName = VoxelTypes.get(info.data).getName();
		
		int modelTextureIndex = 0;
		
		VoxelTexture texture = info.getTexture();
		
		if(!this.texturesNames[modelTextureIndex].equals("~"))
			texture = VoxelTextures.getVoxelTexture(this.texturesNames[modelTextureIndex].replace("~", voxelName));
		int useUntil = this.texturesOffsets[modelTextureIndex];
		int textureS = texture.atlasS;// +mod(sx,texture.textureScale)*offset;
		int textureT = texture.atlasT;// +mod(sz,texture.textureScale)*offset;

		Voxel occTest;

		boolean[] cullingCache = new boolean[6];
		for (int j = 0; j < 6; j++)
		{
			int id = VoxelFormat.id(info.neightborhood[j]);
			int meta = VoxelFormat.meta(info.neightborhood[j]);
			occTest = VoxelTypes.get(id);
			// If it is, don't draw it.
			cullingCache[j] = (occTest.isVoxelOpaque() || occTest.isFaceOpaque(VoxelSides.values()[j], info.neightborhood[j])) || occTest.isFaceOpaque(VoxelSides.values()[j], info.neightborhood[j])
					|| (info.voxelType.isVoxelOpaqueWithItself() && id == VoxelFormat.id(info.data) && meta == info.getMetaData());
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
				if(!this.texturesNames[modelTextureIndex].equals("~"))
					texture = VoxelTextures.getVoxelTexture(this.texturesNames[modelTextureIndex].replace("~", voxelName));
				else
					texture = info.getTexture();
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
				//If rendering top face
				if(this.normals[i*3+1] > 0)
				
				
				renderByteBuffer.addVerticeFloat(this.vertices[i*3+0] + x + dx, this.vertices[i*3+1] + y + dy, this.vertices[i*3+2] + z + dz);
				//vertices.add(new float[] { vert[0] + sx + dx, vert[1] + sy + dy, vert[2] + sz + dz });
				renderByteBuffer.addTexCoordInt((int) (textureS + this.texCoords[i*2+0] * texture.atlasOffset), (int) (textureT + this.texCoords[i*2+1] * texture.atlasOffset));
				//texcoords.add(new int[] { (int) (textureS + tex[0] * texture.atlasOffset), (int) (textureT + tex[1] * texture.atlasOffset) });
				renderByteBuffer.addColorsSpecial(lightColors, depth * 16);
				//colors.add(lightColors);
				renderByteBuffer.addNormalsInt(ChunksRenderer.intifyNormal(this.normals[i*3+0]), ChunksRenderer.intifyNormal(this.normals[i*3+1]), ChunksRenderer.intifyNormal(this.normals[i*3+2]), info.isWavy());
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
}
