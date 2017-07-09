package io.xol.chunkstories.api;

import java.util.Collection;
import java.util.Iterator;

import io.xol.chunkstories.api.animation.SkeletalAnimation;
import io.xol.chunkstories.api.content.NamedWithProperties;
import io.xol.chunkstories.api.entity.EntityType;
import io.xol.chunkstories.api.exceptions.net.UnknowPacketException;
import io.xol.chunkstories.api.item.ItemType;
import io.xol.chunkstories.api.material.Material;
import io.xol.chunkstories.api.mesh.MeshLibrary;
import io.xol.chunkstories.api.mods.Asset;
import io.xol.chunkstories.api.mods.ModsManager;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.particles.ParticleTypeHandler;
import io.xol.chunkstories.api.util.ChunkStoriesLogger;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.models.VoxelModel;
import io.xol.chunkstories.api.voxel.textures.VoxelTexture;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldGenerator;

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
		
		//public EntityType getEntityTypeByClassname(String className);
		
		//public short getEntityIdByClassname(String className);
		
		public Iterator<EntityType> all();

		public Content parent();
		
		/*public EntityComponents components();
		public interface EntityComponents {
			
			public int getIdForClass(String className);
		}*/
	}
	
	public ParticlesTypes particles();
	public interface ParticlesTypes {
		
		public ParticleTypeHandler getParticleTypeHandlerByName(String string);
		
		public ParticleTypeHandler getParticleTypeHandlerById(int id);
		
		public Iterator<ParticleTypeHandler> all();

		public Content parent();
	}
	
	public PacketTypes packets();
	public interface PacketTypes {
		
		public PacketType getPacketTypeById(int packetID);
		
		public PacketType getPacketTypeByName(String name);
		
		public PacketType getPacketType(Packet packet) throws UnknowPacketException;
		
		interface PacketType {
			public String getName();
			
			public int getID();
			
			public AllowedFrom allowedFrom();
			
			public enum AllowedFrom {
				ALL,
				CLIENT,
				SERVER;
			}
		}
		
		public Content parent();
	}
	
	public WorldGenerators generators();
	public interface WorldGenerators {
		
		public WorldGeneratorType getWorldGenerator(String name);
		
		public String getWorldGeneratorName(WorldGenerator generator);
		
		public Iterator<WorldGeneratorType> all();
		
		public interface WorldGeneratorType extends NamedWithProperties {
			public String getName();
			
			public WorldGenerator createForWorld(World world);
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
	
	//TODO API-ize this
	public AnimationsLibrary getAnimationsLibrary();
	public interface AnimationsLibrary {
		public SkeletalAnimation getAnimation(String name);
		
		public Content parent();
	}
	
	public MeshLibrary meshes();
	
	public ChunkStoriesLogger logger();
}
