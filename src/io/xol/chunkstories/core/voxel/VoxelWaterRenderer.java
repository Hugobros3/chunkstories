package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.renderer.VoxelContext;
import io.xol.chunkstories.renderer.chunks.ChunksRenderer;
import io.xol.chunkstories.renderer.chunks.VoxelBaker;
import io.xol.chunkstories.voxel.VoxelTexture;
import io.xol.chunkstories.voxel.VoxelsStore;
import io.xol.chunkstories.voxel.models.VoxelModel;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class VoxelWaterRenderer extends VoxelModel
{
	public VoxelWaterRenderer(VoxelModel model)
	{
		//Copy-paste attributes
		super(model.store(), model.getName(), model.getVertices(), model.getTexCoords(), model.getTexturesNames(), model.getTexturesOffsets(), model.getNormals(), model.getExtra(), model.getCulling(), model.getJitterX(), model.getJitterY(), model.getJitterZ());	
	}

	@Override
	public int renderInto(VoxelBaker renderByteBuffer, VoxelContext info, Chunk chunk, int x, int y, int z)
	{
		int llMs = chunk.getSunLight(x, y, z);//getSunlight(c, x, y, z);
		int llMb = chunk.getBlockLight(x, y, z);//getBlocklight(c, x, y, z);

		float[] lightColors = ChunksRenderer.bakeLightColors(llMb, llMb, llMb, llMb, llMs, llMs, llMs, llMs);
		
		int depth = 0;
		for(int i = 1; i < 16; i++)
		{
			if(chunk.getWorld() == null)
				return 0;
			
			int id = chunk.getWorld().getVoxelData(chunk.getChunkX() * 32 + x, chunk.getChunkY() * 32 + y - i, chunk.getChunkZ() * 32 + z);
			if(VoxelsStore.get().getVoxelById(id) != null && VoxelsStore.get().getVoxelById(id).getType().isLiquid())
				depth++;
			else
				break;
		}
		
		String voxelName = VoxelsStore.get().getVoxelById(info.data).getName();
		
		int modelTextureIndex = 0;
		
		VoxelTexture texture = info.getTexture(VoxelSides.TOP);
		
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
			texture = info.getVoxel().store().textures().getVoxelTextureByName(this.texturesNames[modelTextureIndex].replace("~", voxelName));
		
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
			cullingCache[j] = (occTest.getType().isOpaque() || occTest.isFaceOpaque(VoxelSides.values()[j], info.neightborhood[j])) || occTest.isFaceOpaque(VoxelSides.values()[j], info.neightborhood[j])
					|| (info.getVoxel().getType().isSelfOpaque() && id == VoxelFormat.id(info.data) && meta == info.getMetaData());
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
					texture = info.getVoxel().store().textures().getVoxelTextureByName(this.texturesNames[modelTextureIndex].replace("~", voxelName));
				
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
					if(occTest.getType().isOpaque() || (info.voxelType.isVoxelOpaqueWithItself() && id == VoxelFormat.id(info.data) && meta == info.getMetaData()))
						drawFace = false;*/

					if (cullingCache[j])
						drawFace = false;
				}
			}

			if (drawFace)
			{
				//If rendering top face
				
				//if(this.normals[i*3+1] > 0)
				renderByteBuffer.addVerticeFloat(this.vertices[i*3+0] + x + dx, this.vertices[i*3+1] + y + dy, this.vertices[i*3+2] + z + dz);
				renderByteBuffer.addTexCoordInt((int) (textureS + this.texCoords[i*2+0] * texture.atlasOffset), (int) (textureT + this.texCoords[i*2+1] * texture.atlasOffset));
				renderByteBuffer.addColorsSpecial(lightColors, depth * 16);
				renderByteBuffer.addNormalsInt(ChunksRenderer.intifyNormal(this.normals[i*3+0]), ChunksRenderer.intifyNormal(this.normals[i*3+1]), ChunksRenderer.intifyNormal(this.normals[i*3+2]), (byte)0);
				
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
