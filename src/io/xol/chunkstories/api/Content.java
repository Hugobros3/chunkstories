package io.xol.chunkstories.api;

import java.util.Collection;
import java.util.Iterator;

import io.xol.chunkstories.api.entity.EntityType;
import io.xol.chunkstories.api.item.ItemType;
import io.xol.chunkstories.api.material.Material;
import io.xol.chunkstories.api.mods.Asset;
import io.xol.chunkstories.api.mods.ModsManager;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.particles.ParticleType;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.models.VoxelModel;
import io.xol.chunkstories.api.voxel.textures.VoxelTexture;
import io.xol.chunkstories.api.world.WorldGenerator;
import io.xol.chunkstories.net.packets.IllegalPacketException;
import io.xol.chunkstories.net.packets.UnknowPacketException;
import io.xol.engine.animation.BVHLibrary;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Encapsulates all the user-definable content available
 */
public interface Content
{
	/** Returns which context is this content relevant to */
	//public GameContext getContext();
	
	public ModsManager modsManager();

	/** Obtains an Asset using it's name string 
	 *  More advanced options for obtaining assets are avaible using the ModsManager class */
	public Asset getAsset(String assetName);
	
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
		
		public EntityType getEntityTypeById(short entityId);
		
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
	
	public ParticlesTypes particles();
	public interface ParticlesTypes {
		
		public ParticleType getParticleTypeByName(String string);
		
		public ParticleType getParticleTypeById(int id);
		
		public Iterator<ParticleType> all();

		public Content parent();
	}
	
	public PacketTypes packets();
	public interface PacketTypes {
		
		public Packet createPacketById(int packedID) throws IllegalPacketException;
		
		public short getPacketId(Packet packet) throws UnknowPacketException;
		
		public Content parent();
	}
	
	public WorldGenerators generators();
	public interface WorldGenerators {
		
		public WorldGeneratorType getWorldGenerator(String name);
		
		public String getWorldGeneratorName(WorldGenerator generator);
		
		public Iterator<WorldGeneratorType> all();
		
		public interface WorldGeneratorType {
			public String getName();
			
			public WorldGenerator instanciate();
		}

		public Content parent();
		
		
	}
	
	public LocalizationManager localization();
	public interface LocalizationManager extends Translation {

		//public Translation getTranslation(String abrigedName);
		
		public Collection<String> listTranslations();
		
		public Content parent();

		public void reload();

		public void loadTranslation(String translationCode);
	}
	public interface Translation {

		public String getLocalizedString(String stringName);
		
		public String localize(String text);
	}
	
	public BVHLibrary getAnimationsLibrary();
}
