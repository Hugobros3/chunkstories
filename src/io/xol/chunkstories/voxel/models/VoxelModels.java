package io.xol.chunkstories.voxel.models;

import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.engine.math.lalgb.Vector3f;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class VoxelModels
{
	public static Map<String, VoxelModel> models = new HashMap<String, VoxelModel>();

	public static void resetAndLoadModels()
	{
		models.clear();
		File vanillaFolder = new File("./" + "res/voxels/blockmodels/");
		for (File f : vanillaFolder.listFiles())
		{
			if (!f.isDirectory() && f.getName().endsWith(".model"))
			{
				ChunkStoriesLogger.getInstance().log("Loading custom models file : " + f.getAbsolutePath(), ChunkStoriesLogger.LogType.GAMEMODE, ChunkStoriesLogger.LogLevel.INFO);
				readBlockModel(f);
			}
		}

	}

	private static void readBlockModel(File f)
	{
		if (!f.exists())
			return;
		try
		{
			FileReader fileReader = new FileReader(f);
			BufferedReader reader = new BufferedReader(fileReader);

			String line = "";

			VoxelModel model = null;
			int ln = 0;
			//int loadedBM = 0;
			List<float[]> vertices = new ArrayList<float[]>();
			List<float[]> texcoord = new ArrayList<float[]>();
			List<float[]> normal = new ArrayList<float[]>();
			List<boolean[]> culling = new ArrayList<boolean[]>();
			
			String currentTexture = "~";
			List<String> texturesNames = new ArrayList<String>();
			List<Integer> texturesOffsets = new ArrayList<Integer>();
			
			boolean[] currentCull = new boolean[6];
			int c = 0;
			while ((line = reader.readLine()) != null)
			{
				line = line.replace("\t", "");
				if (line.startsWith("#"))
				{
					// It's a comment, ignore.
				}
				else
				{
					if (model == null && !line.equals(""))
					{
						String bmName = f.getName().replace(".model", "");
						if (!line.equals("default"))
							bmName += "." + line;
						model = new VoxelModel(bmName);
						//Textures calculator
						currentTexture = "~";
					}
					else if (line.startsWith("end"))
					{
						if (model != null)
						{
							//Add last used texture
							texturesNames.add(currentTexture);
							texturesOffsets.add(vertices.size());
							
							//Build list of them with offsets
							model.texturesNames = new String[texturesNames.size()];
							model.texturesOffsets = new int[texturesNames.size()];
							int ti = 0;
							for(String textureName : texturesNames)
							{
								model.texturesNames[ti] = textureName;
								model.texturesOffsets[ti] = texturesOffsets.get(ti);
								
								ti++;
							}
							
							model.vertices = new float[vertices.size() * 3];
							model.texCoords = new float[vertices.size() * 2];
							model.normals = new float[vertices.size() * 3];
							model.culling = new boolean[vertices.size()][6];
							for (int i = 0; i < vertices.size(); i++)
							{
								model.vertices[i * 3 + 0] = vertices.get(i)[0];
								model.vertices[i * 3 + 1] = vertices.get(i)[1];
								model.vertices[i * 3 + 2] = vertices.get(i)[2];
								model.texCoords[i * 2 + 0] = texcoord.get(i)[0];
								model.texCoords[i * 2 + 1] = texcoord.get(i)[1];
								model.normals[i * 3 + 0] = normal.get(i)[0];
								model.normals[i * 3 + 1] = normal.get(i)[1];
								model.normals[i * 3 + 2] = normal.get(i)[2];
							}
							for (int i = 0; i < vertices.size() / 3; i++)
							{
								model.culling[i] = culling.get(i);
							}
							models.put(model.name, model);
							// System.out.println(vertices.size()+" in "+model.name);
							vertices.clear();
							texcoord.clear();
							normal.clear();
							culling.clear();
							texturesNames.clear();
							texturesOffsets.clear();
							currentCull = new boolean[6];
							//loadedBM++;
							model = null;
						}
						else
							ChunkStoriesLogger.getInstance().log("Warning ! Parse error in file " + f + ", line " + ln + ", unexpected 'end' token.", ChunkStoriesLogger.LogType.GAMEMODE, ChunkStoriesLogger.LogLevel.WARN);
					}
					else if (line.startsWith("texture"))
					{
						if(model == null)
							continue;

						String[] splitted = line.split(" ");
						String newTextureName = splitted[1].replace("\'", "");
						//It can't crash from here so we can safely add the textures
						texturesNames.add(currentTexture);
						texturesOffsets.add(vertices.size());
						
						currentTexture = newTextureName;
					}
					else if (line.startsWith("jitter"))
					{
						String[] splitted = line.split(" ");
						if (model == null)
							continue;
						model.jitterX = Float.parseFloat(splitted[1]);
						model.jitterY = Float.parseFloat(splitted[2]);
						model.jitterZ = Float.parseFloat(splitted[3]);
					}
					else if (line.startsWith("v"))
					{
						if (model != null)
						{
							// System.out.println("vv"+vertices.size());
							String[] splitted = line.split(" ");
							String[] vert = splitted[1].split(",");
							String[] tex = splitted[2].split(",");
							String[] nor = splitted[3].split(",");
							vertices.add(new float[] { Float.parseFloat(vert[0]), Float.parseFloat(vert[1]), Float.parseFloat(vert[2]) });
							texcoord.add(new float[] { Float.parseFloat(tex[0]), Float.parseFloat(tex[1]) });
							Vector3f normalizeMe = new Vector3f(Float.parseFloat(nor[0]), Float.parseFloat(nor[1]), Float.parseFloat(nor[2]));
							normalizeMe.normalize();
							normal.add(new float[] { normalizeMe.x, normalizeMe.y, normalizeMe.z });
							c++;
							if (c >= 3)
							{
								culling.add(currentCull);
								c = 0;
							}
						}
						else
							ChunkStoriesLogger.getInstance().log("Warning ! Parse error in file " + f + ", line " + ln + ", unexpected parameter.", ChunkStoriesLogger.LogType.GAMEMODE, ChunkStoriesLogger.LogLevel.WARN);
					}
					else if (line.startsWith("cull"))
					{
						currentCull = new boolean[6];
						for (String face : line.split(" "))
						{
							switch (face)
							{
							case "bottom":
								//System.out.println("bottom"+f);
								currentCull[Face.BOTTOM] = true;
								break;
							case "top":
								currentCull[Face.TOP] = true;
								break;
							case "left":
								currentCull[Face.LEFT] = true;
								break;
							case "right":
								currentCull[Face.RIGHT] = true;
								break;
							case "front":
								currentCull[Face.FRONT] = true;
								break;
							case "back":
								currentCull[Face.BACK] = true;
								break;
							}
						}
					}
					else if (line.startsWith("require"))
					{
						if (model != null)
						{
							String[] splitted = line.split(" ");
							if (splitted.length == 2)
							{
								String toInclude = splitted[1];
								toInclude = toInclude.replace("~", model.name.contains(".") ? model.name.split("\\.")[0] : model.name);
								VoxelModel includeMeh = getVoxelModel(toInclude);
								if (includeMeh != null)
								{
									for (int i = 0; i < includeMeh.vertices.length / 3; i++)
									{
										vertices.add(new float[] { includeMeh.vertices[i * 3 + 0], includeMeh.vertices[i * 3 + 1], includeMeh.vertices[i * 3 + 2] });
										texcoord.add(new float[] { includeMeh.texCoords[i * 2 + 0], includeMeh.texCoords[i * 2 + 1] });
										normal.add(new float[] { includeMeh.normals[i * 3 + 0], includeMeh.normals[i * 3 + 1], includeMeh.normals[i * 3 + 2] });
									}
									/*for(float v[] : includeMeh.vertices)
										vertices.add(includeMeh.vertices);
									//for(float t[] : includeMeh.texCoords)
										texcoord.add(includeMeh.texCoords);
									//for(float n[] : includeMeh.normals)
										normal.add(includeMeh.normals);*/
									for (boolean cul[] : includeMeh.culling)
										culling.add(cul);
								}
								else
									ChunkStoriesLogger.getInstance().log("Warning ! Can't require '" + toInclude + "'", ChunkStoriesLogger.LogType.GAMEMODE, ChunkStoriesLogger.LogLevel.WARN);

							}
							else
							{
							}
						}
						else
							ChunkStoriesLogger.getInstance().log("Warning ! Parse error in file " + f + ", line " + ln + ", unexpected parameter.", ChunkStoriesLogger.LogType.GAMEMODE, ChunkStoriesLogger.LogLevel.WARN);
					}
				}
				ln++;
			}
			//ChunkStoriesLogger.getInstance().log("Debug : Parsed file " + f + " correctly, loading " + loadedBM + " blockmodels.", ChunkStoriesLogger.LogType.GAMEMODE, ChunkStoriesLogger.LogLevel.DEBUG);

			reader.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static VoxelModel getVoxelModel(String name)
	{
		if (name.endsWith(".default"))
			name = name.substring(0, name.length() - 8);
		if (models.containsKey(name))
			return models.get(name);
		ChunkStoriesLogger.getInstance().log("Couldn't serve voxel model : " + name, ChunkStoriesLogger.LogType.GAMEMODE, ChunkStoriesLogger.LogLevel.ERROR);
		return null;
	}
}
