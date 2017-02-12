package io.xol.chunkstories.voxel;

import io.xol.chunkstories.api.Content;
import io.xol.chunkstories.api.exceptions.content.IllegalVoxelDeclarationException;
import io.xol.chunkstories.api.mods.Asset;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.content.GameContentStore;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.chunkstories.voxel.models.VoxelModelsStore;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class VoxelsStore implements Content.Voxels
{
	private static VoxelsStore self;
	
	public static VoxelsStore get()
	{
		return self;
	}
	
	public VoxelsStore(GameContentStore content)
	{
		this.content = content;
		this.textures = new VoxelTexturesStoreAndAtlaser(this);
		this.models = new VoxelModelsStore(this);
		
		this.reloadVoxelTypes();
	}
	
	private final GameContentStore content;
	private final VoxelTexturesStoreAndAtlaser textures;
	private final VoxelModelsStore models;
	
	public Voxel[] voxels = new Voxel[65536];
	public Set<Integer> attributedIds = new HashSet<Integer>();
	public Map<String, Voxel> voxelsByName = new HashMap<String, Voxel>();
	public int voxelTypes = 0;
	public int lastAllocatedId;

	@Override
	public VoxelTexturesStoreAndAtlaser textures()
	{
		return textures;
	}

	@Override
	public VoxelModelsStore models()
	{
		return models;
	}
	
	public void reload()
	{
		this.textures.buildTextureAtlas();
		this.models.resetAndLoadModels();
		
		this.reloadVoxelTypes();
	}
	
	private void reloadVoxelTypes()
	{
		//Discard previous voxels
		Arrays.fill(voxels, null);
		attributedIds.clear();
		voxelsByName.clear();

		Iterator<Asset> i = content.modsManager().getAllAssetsByExtension("voxels");
		while (i.hasNext())
		{
			Asset f = i.next();
			ChunkStoriesLogger.getInstance().log("Reading voxels definitions in : " + f);
			readVoxelsDefinitions(f);
		}
	}
	
	private void readVoxelsDefinitions(Asset f)
	{
		self = this;
		
		if (f == null)
			return;
		try
		{
			BufferedReader reader = new BufferedReader(f.reader());

			String line = "";

			//Voxel voxel = null;
			int ln = 0;
			int loadedVoxels = 0;
			while ((line = reader.readLine()) != null)
			{
				line = line.replace("\t", "");
				if (line.startsWith("#"))
				{
					// It's a comment, ignore.
				}
				else
				{
					if (line.startsWith("voxel"))
					{
						String splitted[] = line.split(" ");
						
						if(splitted.length < 3)
						{
							ChunkStoriesLogger.getInstance().log("Parse error in file " + f + ", line " + ln + ", malformed voxel tag. Aborting read.", ChunkStoriesLogger.LogType.GAMEMODE, ChunkStoriesLogger.LogLevel.WARN);
							break;
						}
						
						int id = Integer.parseInt(splitted[2]);
						String name = splitted[1];
						
						if (voxels[id] != null)
							ChunkStoriesLogger.getInstance().log("Voxel redefinition in file " + f + ", line " + ln + ", overriding id " + id + " with " + name, ChunkStoriesLogger.LogType.GAMEMODE, ChunkStoriesLogger.LogLevel.WARN);

						try
						{
							VoxelTypeImplementation voxelType = new VoxelTypeImplementation(this, name, id, reader);
							Voxel voxel = voxelType.getVoxelObject();
							
							voxels[voxel.getId()] = voxel;
							attributedIds.add(voxel.getId());
							voxelsByName.put(voxel.getName(), voxel);
							loadedVoxels++;
						}
						catch (IllegalVoxelDeclarationException e)
						{
							e.printStackTrace();
						}
					}
					else if (line.startsWith("end"))
					{
						ChunkStoriesLogger.getInstance().log("Parse error in file " + f + ", line " + ln + ", unexpected 'end' token.", ChunkStoriesLogger.LogType.GAMEMODE, ChunkStoriesLogger.LogLevel.WARN);
					}
				}
				ln++;
			}
			ChunkStoriesLogger.getInstance().log("Debug : Parsed file " + f + " correctly, loading " + loadedVoxels + " voxels.", ChunkStoriesLogger.LogType.GAMEMODE, ChunkStoriesLogger.LogLevel.DEBUG);

			reader.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Get a voxel by it's id
	 * @param voxelId The id of the voxel
	 * @return
	 */
	public Voxel getVoxelById(int voxelId)
	{
		//Sanitize
		voxelId = VoxelFormat.id(voxelId);
		if (voxelId <= 0)
			return voxels[0];
		if (voxelId >= voxels.length)
			return voxels[0];
		Voxel v = voxels[voxelId];
		if (v == null)
		{
			//System.out.println("Asked for unknown id : " + voxelId);
			return voxels[0];
		}
		return v;
	}

	public Voxel getVoxelByName(String voxelName)
	{
		return voxelsByName.get(voxelName);
	}

	public Set<Integer> getAllLoadedVoxelIds()
	{
		return attributedIds;
	}

	@Override
	public Iterator<Voxel> all()
	{
		return voxelsByName.values().iterator();
	}

	@Override
	public GameContentStore parent()
	{
		return content;
	}
}
