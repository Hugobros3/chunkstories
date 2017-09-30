package io.xol.chunkstories.content;

import io.xol.chunkstories.api.Content;
import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.exceptions.content.mods.NotAllModsLoadedException;
import io.xol.chunkstories.api.mesh.MeshLibrary;
import io.xol.chunkstories.api.mods.Asset;
import io.xol.chunkstories.api.mods.ModsManager;
import io.xol.chunkstories.api.util.ChunkStoriesLogger;
import io.xol.chunkstories.entity.EntityTypesStore;
import io.xol.chunkstories.item.ItemTypesStore;
import io.xol.chunkstories.materials.MaterialsStore;
import io.xol.chunkstories.mesh.MeshStore;
import io.xol.chunkstories.net.PacketsStore;
import io.xol.chunkstories.particles.ParticlesTypesStore;
import io.xol.chunkstories.voxel.VoxelsStore;
import io.xol.chunkstories.world.generator.WorldGeneratorsStore;
import io.xol.engine.animation.BVHLibrary;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class GameContentStore implements Content
{
	private final GameContext context;
	private final ModsManager modsManager;

	private final MaterialsStore materials;
	private final ItemTypesStore items;
	private final VoxelsStore voxels;
	private final EntityTypesStore entities;
	private final PacketsStore packets;
	private final ParticlesTypesStore particles;
	private final WorldGeneratorsStore generators;
	
	private final BVHLibrary bvhLibrary;
	
	protected final MeshStore meshes;
	
	private final LocalizationManagerActual localizationManager;

	public GameContentStore(GameContext context, String enabledModsLaunchArguments)
	{
		this.context = context;
		this.modsManager = new ModsManagerImplementation(enabledModsLaunchArguments);

		// ! LOADS MODS
		/*try
		{
			modsManager.loadEnabledMods();
		}
		catch (NotAllModsLoadedException e)
		{
			e.printStackTrace();
		}*/

		// ! TO REFACTOR

		materials = new MaterialsStore(this);
		items = new ItemTypesStore(this);
		voxels = new VoxelsStore(this);
		entities = new EntityTypesStore(this);
		packets = new PacketsStore(this);
		particles = new ParticlesTypesStore(this);
		generators = new WorldGeneratorsStore(this);
		
		bvhLibrary = new BVHLibrary(this);
		
		meshes = new MeshStore(this);
		
		localizationManager = new LocalizationManagerActual(this, "en");
	}
	
	public void reload()
	{
		// ! LOADS MODS

		try
		{
			modsManager.loadEnabledMods();
		}
		catch (NotAllModsLoadedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		materials.reload();
		items.reload();
		voxels.reload();
		entities.reload();
		packets.reload();
		particles.reload();
		generators.reload();
		
		bvhLibrary.reload();
		
		meshes.reload();
		
		localizationManager.reload();
	}

	@Override
	public MaterialsStore materials()
	{
		return materials;
	}

	@Override
	public VoxelsStore voxels()
	{
		return voxels;
	}

	@Override
	public ItemTypesStore items()
	{
		return items;
	}

	@Override
	public EntityTypesStore entities()
	{
		return entities;
	}

	@Override
	public ParticlesTypesStore particles()
	{
		return particles;
	}

	@Override
	public PacketsStore packets()
	{
		return packets;
	}

	public GameContext getContext()
	{
		return context;
	}

	@Override
	public ModsManager modsManager()
	{
		return modsManager;
	}

	@Override
	public Asset getAsset(String assetName)
	{
		return modsManager.getAsset(assetName);
	}

	@Override
	public WorldGeneratorsStore generators()
	{
		return generators;
	}

	@Override
	public BVHLibrary getAnimationsLibrary()
	{
		return bvhLibrary;
	}

	@Override
	public LocalizationManager localization()
	{
		return localizationManager;
	}

	@Override
	public MeshLibrary meshes() {
		return meshes;
	}

	@Override
	public ChunkStoriesLogger logger() {
		return context.logger();
	}
}
