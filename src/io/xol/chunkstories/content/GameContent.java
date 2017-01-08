package io.xol.chunkstories.content;

import io.xol.chunkstories.api.Content;
import io.xol.chunkstories.api.client.ChunkStories;
import io.xol.chunkstories.content.mods.exceptions.NotAllModsLoadedException;
import io.xol.chunkstories.entity.Entities;
import io.xol.chunkstories.entity.EntityComponents;
import io.xol.chunkstories.item.ItemTypes;
import io.xol.chunkstories.materials.Materials;
import io.xol.chunkstories.net.packets.PacketsProcessor;
import io.xol.chunkstories.particles.ParticleTypes;
import io.xol.chunkstories.voxel.VoxelsStore;
import io.xol.chunkstories.world.generator.WorldGenerators;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class GameContent implements Content
{
	private final ChunkStories context;

	private final VoxelsStore voxels;

	public GameContent(ChunkStories context)
	{
		this.context = context;

		// ! LOADS MODS

		try
		{
			ModsManager.loadEnabledMods();
		}
		catch (NotAllModsLoadedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// ! TO REFACTOR

		io.xol.chunkstories.materials.Materials.reload();
		ItemTypes.reload();

		voxels = new VoxelsStore(this);

		Entities.reload();

		EntityComponents.reload();

		PacketsProcessor.loadPacketsTypes();

		WorldGenerators.loadWorldGenerators();

		io.xol.chunkstories.particles.ParticleTypes.reload();
	}

	public void reload()
	{
		// ! LOADS MODS

		try
		{
			ModsManager.loadEnabledMods();
		}
		catch (NotAllModsLoadedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		io.xol.chunkstories.materials.Materials.reload();
		ItemTypes.reload();

		voxels.reload();

		Entities.reload();

		EntityComponents.reload();

		PacketsProcessor.loadPacketsTypes();

		WorldGenerators.loadWorldGenerators();

		io.xol.chunkstories.particles.ParticleTypes.reload();
	}

	@Override
	public Materials materials()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Voxels voxels()
	{
		return voxels;
	}

	@Override
	public ItemsTypes items()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public EntityTypes entities()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ParticleTypes particles()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ChunkStories getContext()
	{
		return context;
	}

}
