package io.xol.chunkstories.content;

import io.xol.chunkstories.api.Content;
import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.PacketsStore;
import io.xol.chunkstories.api.mods.Asset;
import io.xol.chunkstories.api.mods.ModsManager;
import io.xol.chunkstories.content.mods.exceptions.NotAllModsLoadedException;
import io.xol.chunkstories.entity.EntityTypesStore;
import io.xol.chunkstories.item.ItemTypesStore;
import io.xol.chunkstories.materials.MaterialsStore;
import io.xol.chunkstories.particles.ParticlesTypesStore;
import io.xol.chunkstories.voxel.VoxelsStore;
import io.xol.chunkstories.world.generator.WorldGenerators;

//(c) 2015-2016 XolioWare Interactive
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

		WorldGenerators.loadWorldGenerators();
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

		WorldGenerators.loadWorldGenerators();
	}

	@Override
	public Materials materials()
	{
		return materials;
	}

	@Override
	public Voxels voxels()
	{
		return voxels;
	}

	@Override
	public ItemsTypes items()
	{
		return items;
	}

	@Override
	public EntityTypes entities()
	{
		return entities;
	}

	@Override
	public ParticlesTypes particles()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PacketTypes packets()
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
}
