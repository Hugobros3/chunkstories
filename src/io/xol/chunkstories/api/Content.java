package io.xol.chunkstories.api;

import java.util.Iterator;

import io.xol.chunkstories.api.client.ChunkStories;
import io.xol.chunkstories.api.entity.EntityType;
import io.xol.chunkstories.api.item.ItemType;
import io.xol.chunkstories.api.material.Material;
import io.xol.chunkstories.api.mods.ModsManager;
import io.xol.chunkstories.api.particles.ParticleType;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.voxel.VoxelTexture;
import io.xol.chunkstories.voxel.models.VoxelModel;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Encapsulates all the user-definable content available
 */
public interface Content
{
	public ChunkStories getContext();
	
	public ModsManager modsManager();
	
	public Materials materials();
	public interface Materials {
		
		public Material getMaterialByName(String materialName);
		
		public Iterator<Material> all();

		public Content parent();
	}
	
	public Voxels voxels();
	public interface Voxels {
		
		public Voxel getVoxelById(int voxelId_orVoxelData);
		
		public Voxel getVoxelByName(String voxelName);
		
		public Iterator<Voxel> all();

		public Content parent();
		
		public VoxelTextures textures();
		public interface VoxelTextures {
			
			public VoxelTexture getVoxelTextureByName(String voxelTextureName);
			
			public Iterator<VoxelTexture> all();

			public Voxels parent();
		}
		
		public VoxelModels models();
		public interface VoxelModels {
			
			public VoxelModel getVoxelModelByName(String voxelModelName);
			
			public Iterator<VoxelModel> all();

			public Voxels parent();
		}
	}
	
	public ItemsTypes items();
	public interface ItemsTypes {
		
		public ItemType getItemTypeById(int itemId);
		
		public ItemType getItemTypeByName(String itemName);
		
		public Iterator<ItemType> all();

		public Content parent();
	}
	
	public EntityTypes entities();
	public interface EntityTypes {
		
		public EntityType getEntityTypeById(int entityId);
		
		public EntityType getEntityTypeByName(String entityName);
		
		public EntityType getEntityTypeByClassname(String className);
		
		public short getEntityIdByClassname(String className);
		
		public Iterator<EntityType> all();

		public Content parent();
		
		public EntityComponents components();
		public interface EntityComponents {
			
			public int getIdForClass(String className);
		}
	}
	
	public ParticleTypes particles();
	public interface ParticleTypes {
		
		public ParticleType getParticleTypeByName(String string);
		
		public ParticleType getParticleTypeById(int id);
		
		public Iterator<ParticleType> all();

		public Content parent();
	}
}
