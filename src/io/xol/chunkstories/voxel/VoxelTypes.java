package io.xol.chunkstories.voxel;

import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.chunkstories.voxel.models.VoxelModels;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class VoxelTypes
{
	public static Voxel[] voxels = new Voxel[65536];
	public static int voxelTypes = 0;
	public static int lastAllocatedId;

	@SuppressWarnings("rawtypes")
	private static void readVoxelsDefinitions(File f)
	{
		if (!f.exists())
			return;
		try
		{
			FileReader fileReader = new FileReader(f);
			BufferedReader reader = new BufferedReader(fileReader);

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
							voxel = new Voxel(id, name);
						else
						{
							try
							{
								Class<?> customVoxelClass = Class.forName(splitted[3]);

								Class[] types = { Integer.TYPE, String.class };
								Constructor constructor = customVoxelClass.getConstructor(types);

								Object[] parameters = { id, name };
								voxel = (Voxel) constructor.newInstance(parameters);

							}
							catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
							{
								e.printStackTrace();
							}
						}
						// Default textures
						for (int i = 0; i < 6; i++)
							voxel.texture[i] = VoxelTextures.getVoxelTexture(name);
						// Default collision box
						CollisionBox box = new CollisionBox(1, 1, 1);
						box.translate(0.5, -1, 0.5);
						voxel.box = box;
					}
					else if (line.startsWith("end"))
					{
						if (voxel != null)
						{
							voxels[voxel.voxelID] = voxel;
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
							// System.out.println("Debug : loading voxel parameter : "+line);
							String splitted[] = (line.replace(" ", "").replace("\t", "")).split(":");
							String parameterName = splitted[0];
							String parameterValue = splitted[1];
							switch (parameterName)
							{
							case "solid":
								voxel.solid = Boolean.parseBoolean(parameterValue);
								break;
							case "opaque":
								voxel.opaque = Boolean.parseBoolean(parameterValue);
								break;
							case "selfOpaque":
								voxel.self_opaque = Boolean.parseBoolean(parameterValue);
								break;
							case "liquid":
								voxel.liquid = Boolean.parseBoolean(parameterValue);
								break;
							case "emitting":
								voxel.lightLevel = Short.parseShort(parameterValue);
								break;
							case "isProp":
								voxel.prop = Boolean.parseBoolean(parameterValue);
								break;
							case "model":
								voxel.prop = true;
								voxel.model = VoxelModels.getVoxelModel(parameterValue.replace("'", "").replace("~", voxel.voxelName));
								break;
							case "texture":
								for (int i = 0; i < 6; i++)
								{
									voxel.texture[i] = VoxelTextures.getVoxelTexture(parameterValue);
									// System.out.println(textureName);
								}
								break;
							case "textures":
								String sides[] = parameterValue.split(",");
								for (int i = 0; i < sides.length; i++)
								{
									String textureName = sides[i].replace("[", "").replace("]", "").replace("'", "").replace("~", voxel.voxelName);
									voxel.texture[i] = VoxelTextures.getVoxelTexture(textureName);
									// System.out.println(textureName);
								}
								break;
							case "collisionBox":
								// Default collision box
								String sizes[] = (parameterValue.replace("[", "").replace("]", "")).split(",");

								CollisionBox box = new CollisionBox(Float.parseFloat(sizes[3]), Float.parseFloat(sizes[4]), Float.parseFloat(sizes[5]));
								box.translate(0.5, -1, 0.5);
								box.translate(Float.parseFloat(sizes[0]), Float.parseFloat(sizes[1]), Float.parseFloat(sizes[2]));
								voxel.box = box;
								break;
							case "affectedByWind":
								voxel.affectedByWind = Boolean.parseBoolean(parameterValue);
								break;
							case "billboard":
								voxel.billboard = Boolean.parseBoolean(parameterValue);
								break;
							default:
								ChunkStoriesLogger.getInstance().log("Parse error in file " + f + ", line " + ln + ", unknown parameter '" + parameterName + "'", ChunkStoriesLogger.LogType.GAMEMODE, ChunkStoriesLogger.LogLevel.WARN);
								break;
							}
						}
						else
							ChunkStoriesLogger.getInstance().log("Warning ! Parse error in file " + f + ", line " + ln + ", unexpected parameter.", ChunkStoriesLogger.LogType.GAMEMODE, ChunkStoriesLogger.LogLevel.WARN);
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

	public static void loadVoxelTypes()
	{
		// Load .voxels files
		// From vanilla
		File vanillaFolder = new File("./" + "res/voxels/");
		for (File f : vanillaFolder.listFiles())
		{
			if (!f.isDirectory() && f.getName().endsWith(".voxels"))
			{
				ChunkStoriesLogger.getInstance().log("Reading voxels definitions in : " + f.getAbsolutePath());
				readVoxelsDefinitions(f);
			}
		}

		for (Voxel vo : voxels)
		{
			if (vo != null)
			{
				//vo.color = VoxelTextures.getTextureColorAVG(vo.getVoxelTexture(0, new BlockRenderInfo()).name);
			}
		}
		/*
		 * addVoxel(new VoxelAir(0,"air").solid(false).opaque(false));
		 * addVoxel(new Voxel(1,"stone")); addVoxel(new Voxel(2,"grass"));
		 * addVoxel(new Voxel(3,"dirt")); addVoxel(new Voxel(4,"wood"));
		 * addVoxel(new Voxel(5,"leaves").opaque(false).selfOpaque(false));
		 * addVoxel(new Voxel(6,"forestgrass")); addVoxel(new
		 * Voxel(7,"drygrass"));
		 * 
		 * addVoxel(new Voxel(8,"pinewood")); addVoxel(new
		 * Voxel(9,"pineleaves").opaque(false).selfOpaque(false));
		 * 
		 * addVoxel(new Voxel(10,"bricks")); addVoxel(new Voxel(11,"gravel"));
		 * addVoxel(new Voxel(12,"sand"));
		 * 
		 * addVoxel(new Voxel(13,"cactus"));
		 * 
		 * addVoxel(new Voxel(14,"planks")); addVoxel(new
		 * Voxel(15,"glass").opaque(false).selfOpaque(true));
		 * 
		 * addVoxel(new Voxel(16,"glowing").emitting(15)); addVoxel(new
		 * Voxel(17,"snow"));
		 * 
		 * addVoxel(new
		 * Voxel(64,"grass_prop").solid(false).prop(true).opaque(false));
		 * addVoxel(new
		 * Voxel(65,"forestgrass_prop").solid(false).prop(true).opaque(false));
		 * 
		 * addVoxel(new
		 * Voxel(128,"water").liquid(true).solid(false).opaque(false));
		 */
	}

	/*
	 * private static void addVoxel(Voxel voxel) { if(voxel.voxelID >
	 * lastAllocatedId) lastAllocatedId = voxel.voxelID; voxels[voxel.voxelID] =
	 * voxel; voxelTypes++; }
	 */

	/*
	 * public static void initVoxelTextures() { for(Voxel vo : voxels) { if(vo
	 * != null) { for(int i = 0; i < 6; i++) vo.texture[i].legacyId =
	 * VoxelTiles.getVoxelTexture(vo.getVoxelTexture(i)); //Color summary
	 * vo.color = VoxelTiles.getTextureColorAVG(vo.getVoxelTexture(0)); } } }
	 */

	public static Voxel get(int voxelId)
	{
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

	public static int getNextValidVoxelId(int id)
	{
		Voxel v = null;
		while (v == null)
		{
			id++;
			if (id >= voxels.length)
				return 0;
			v = voxels[id];
		}
		return id;
	}

	public static int getPreviousValidVoxelId(int id)
	{
		Voxel v = null;
		while (v == null)
		{
			id--;
			if (id <= 0)
				return 0;
			v = voxels[id];
		}
		return id;
	}
}
