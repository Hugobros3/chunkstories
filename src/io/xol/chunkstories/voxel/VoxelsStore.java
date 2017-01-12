package io.xol.chunkstories.voxel;

import io.xol.chunkstories.api.Content;
import io.xol.chunkstories.api.client.ChunkStories;
import io.xol.chunkstories.api.mods.Asset;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.materials.Materials;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.chunkstories.voxel.models.VoxelModelsStore;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
	
	public VoxelsStore(Content content)
	{
		this.content = content;
		this.context = content.getContext();
		this.textures = new VoxelTexturesStoreAndAtlaser(context, this);
		this.models = new VoxelModelsStore(context, this);
		
		this.reloadVoxelTypes();
	}
	
	private final ChunkStories context;
	private final Content content;
	private final VoxelTexturesStoreAndAtlaser textures;
	private final VoxelModelsStore models;
	
	public Voxel[] voxels = new Voxel[65536];
	public Set<Integer> attributedIds = new HashSet<Integer>();
	public Map<String, Voxel> voxelsByName = new HashMap<String, Voxel>();
	public int voxelTypes = 0;
	public int lastAllocatedId;

	@Override
	public VoxelTextures textures()
	{
		return textures;
	}

	@Override
	public VoxelModels models()
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

		Iterator<Asset> i = context.getContent().modsManager().getAllAssetsByExtension("voxels");
		while (i.hasNext())
		{
			Asset f = i.next();
			ChunkStoriesLogger.getInstance().log("Reading voxels definitions in : " + f);
			readVoxelsDefinitions(f);
		}
	}
	
	@SuppressWarnings("rawtypes")
	private void readVoxelsDefinitions(Asset f)
	{
		self = this;
		
		if (f == null)
			return;
		try
		{
			BufferedReader reader = new BufferedReader(f.reader());

			String line = "";

			Voxel voxel = null;
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
						if (voxel != null)
							ChunkStoriesLogger.getInstance().log("Parse error in file " + f + ", line " + ln + ", unexpected 'voxel' token.", ChunkStoriesLogger.LogType.GAMEMODE, ChunkStoriesLogger.LogLevel.WARN);
						String splitted[] = line.split(" ");
						int id = Integer.parseInt(splitted[2]);
						String name = splitted[1];
						if (voxels[id] != null)
							ChunkStoriesLogger.getInstance().log("Voxel redefinition in file " + f + ", line " + ln + ", overriding id " + id + " with " + name, ChunkStoriesLogger.LogType.GAMEMODE, ChunkStoriesLogger.LogLevel.WARN);

						if (splitted.length == 3)
							voxel = new VoxelDefault(this, id, name);
						else
						{
							try
							{
								Class<?> customVoxelClass = context.getContent().modsManager().getClassByName(splitted[3]); // Class.forName(splitted[3]);
								if (customVoxelClass == null)
								{
									ChunkStoriesLogger.getInstance().warning("Voxel class " + splitted[3] + " does not exist in codebase.");
								}
								else if (!(Voxel.class.isAssignableFrom(customVoxelClass)))
								{
									ChunkStoriesLogger.getInstance().warning("Voxel class " + splitted[3] + " is not extending the Voxel base class.");
								}
								else
								{
									Class[] types = { Content.Voxels.class, Integer.TYPE, String.class };
									Constructor constructor = customVoxelClass.getConstructor(types);

									Object[] parameters = { this, id, name };
									voxel = (Voxel) constructor.newInstance(parameters);
								}
							}
							catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
							{
								e.printStackTrace();
							}
						}
						// Default textures
						if (voxel instanceof VoxelDefault)
						{
							for (int i = 0; i < 6; i++)
								((VoxelDefault) voxel).texture[i] = textures.getVoxelTextureByName(name);
							// Default collision box
							CollisionBox box = new CollisionBox(1, 1, 1);
							box.translate(0.5, 0, 0.5);
							((VoxelDefault) voxel).box = box;
						}
					}
					else if (line.startsWith("end"))
					{
						if (voxel != null)
						{
							voxels[voxel.getId()] = voxel;
							attributedIds.add(voxel.getId());
							voxelsByName.put(voxel.getName(), voxel);
							voxel = null;
							loadedVoxels++;
						}
						else
							ChunkStoriesLogger.getInstance().log("Parse error in file " + f + ", line " + ln + ", unexpected 'end' token.", ChunkStoriesLogger.LogType.GAMEMODE, ChunkStoriesLogger.LogLevel.WARN);
					}
					else if (!line.equals(""))
					{
						if (voxel != null)
						{
							if (voxel instanceof VoxelDefault)
							{
								VoxelDefault voxDefault = (VoxelDefault) voxel;
								// System.out.println("Debug : loading voxel parameter : "+line);
								String splitted[] = (line.replace(" ", "").replace("\t", "")).split(":");
								String parameterName = splitted[0];
								String parameterValue = splitted[1];

								switch (parameterName)
								{
								case "solid":
									voxDefault.solid = Boolean.parseBoolean(parameterValue);
									break;
								case "opaque":
									voxDefault.opaque = Boolean.parseBoolean(parameterValue);
									break;
								case "selfOpaque":
									voxDefault.self_opaque = Boolean.parseBoolean(parameterValue);
									break;
								case "liquid":
									voxDefault.liquid = Boolean.parseBoolean(parameterValue);
									break;
								case "emitting":
									voxDefault.lightLevel = Short.parseShort(parameterValue);
									break;
								case "shading":
									voxDefault.shading = Short.parseShort(parameterValue);
									break;
								case "usesCustomModel":
									voxDefault.custom_model = Boolean.parseBoolean(parameterValue);
									break;
								case "model":
									voxDefault.custom_model = true;
									voxDefault.model = models.getVoxelModelByName(parameterValue.replace("'", "").replace("~", voxel.getName()));
									break;
								case "texture":
									for (int i = 0; i < 6; i++)
									{
										voxDefault.texture[i] = textures.getVoxelTextureByName(parameterValue);
										// System.out.println(textureName);
									}
									break;
								case "textures":
									String sides[] = parameterValue.split(",");
									for (int i = 0; i < sides.length; i++)
									{
										String textureName = sides[i].replace("[", "").replace("]", "").replace("'", "").replace("~", voxel.getName());
										voxDefault.texture[i] = textures.getVoxelTextureByName(textureName);
										// System.out.println(textureName);
									}
									break;
								case "collisionBox":
									// Default collision box
									String sizes[] = (parameterValue.replace("[", "").replace("]", "")).split(",");

									CollisionBox box = new CollisionBox(Float.parseFloat(sizes[3]), Float.parseFloat(sizes[4]), Float.parseFloat(sizes[5]));
									box.translate(0.5, 0, 0.5);
									box.translate(Float.parseFloat(sizes[0]), Float.parseFloat(sizes[1]), Float.parseFloat(sizes[2]));
									voxDefault.box = box;
									break;
								case "affectedByWind":
									voxDefault.affectedByWind = Boolean.parseBoolean(parameterValue);
									break;
								case "billboard":
									voxDefault.billboard = Boolean.parseBoolean(parameterValue);
									break;
								case "material":
									voxDefault.material = Materials.getMaterialByName(parameterValue);
									break;
								default:
									ChunkStoriesLogger.getInstance().log("Parse error in file " + f + ", line " + ln + ", unknown parameter '" + parameterName + "'", ChunkStoriesLogger.LogType.GAMEMODE, ChunkStoriesLogger.LogLevel.WARN);
									break;
								}
							}
							else
								ChunkStoriesLogger.getInstance().log("Warning ! Parse error in file " + f + ", line " + ln + ", unexpected parameter.", ChunkStoriesLogger.LogType.GAMEMODE, ChunkStoriesLogger.LogLevel.WARN);
						}
						else
							ChunkStoriesLogger.getInstance().log("Warning ! Parse error in file " + f + ", line " + ln + ", voxel parameters are reserved to classes extending VoxelDefault !", ChunkStoriesLogger.LogType.GAMEMODE,
									ChunkStoriesLogger.LogLevel.WARN);
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
	 * 
	 * @param voxelId
	 *            The id of the voxel
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
	public Content parent()
	{
		return content;
	}
}
