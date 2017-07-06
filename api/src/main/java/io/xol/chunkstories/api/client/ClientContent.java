package io.xol.chunkstories.api.client;

import java.util.Iterator;

import io.xol.chunkstories.api.Content;
import io.xol.chunkstories.api.rendering.mesh.ClientMeshLibrary;
import io.xol.chunkstories.api.rendering.textures.Cubemap;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.api.rendering.textures.TextureFormat;
import io.xol.chunkstories.api.voxel.textures.VoxelTexture;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface ClientContent extends Content
{
	public ClientInterface getClient();
	
	public ClientMeshLibrary meshes();
	
	public TexturesLibrary textures();
	
	public ClientVoxels voxels();
	public interface ClientVoxels extends Voxels {
		
		public ClientVoxelTextures textures();
		public interface ClientVoxelTextures extends VoxelTextures {
			
			public Texture2D getDiffuseAtlasTexture();
			
			public Texture2D getNormalAtlasTexture();
			
			public Texture2D getMaterialAtlasTexture();
			
			public VoxelTexture getVoxelTextureByName(String voxelTextureName);
			
			public Iterator<VoxelTexture> all();

			public Voxels parent();
		}
	}
	
	public interface TexturesLibrary {

		public Texture2D nullTexture();
		
		public Texture2D getTexture(String assetName);
		
		public Texture2D newTexture2D(TextureFormat type, int width, int height);
		
		public Cubemap getCubemap(String cubemapName);
		
		public ClientContent parent();
	}
}
