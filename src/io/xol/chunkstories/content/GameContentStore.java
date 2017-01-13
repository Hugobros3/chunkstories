package io.xol.chunkstories.content;

import io.xol.chunkstories.api.Content;
import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.mods.Asset;
import io.xol.chunkstories.api.mods.ModsManager;
import io.xol.chunkstories.content.mods.exceptions.NotAllModsLoadedException;
import io.xol.chunkstories.entity.EntityTypesStore;
import io.xol.chunkstories.item.ItemTypesStore;
import io.xol.chunkstories.materials.MaterialsStore;
import io.xol.chunkstories.net.PacketsStore;
import io.xol.chunkstories.particles.ParticlesTypesStore;
import io.xol.chunkstories.voxel.VoxelsStore;
import io.xol.chunkstories.world.generator.WorldGeneratorsStore;

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

	public GameContentStore(GameContext context, String enabledModsLaunchArguments)
	{
		this.context = context;
		this.modsManager = new DefaultModsManager(enabledModsLaunchArguments);

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

		// ! TO REFACTOR

		materials = new MaterialsStore(this);
		items = new ItemTypesStore(this);
		voxels = new VoxelsStore(this);
		entities = new EntityTypesStore(this);
		packets = new PacketsStore(this);
		particles = new ParticlesTypesStore(this);
		generators = new WorldGeneratorsStore(this);
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

	@Override
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
}
