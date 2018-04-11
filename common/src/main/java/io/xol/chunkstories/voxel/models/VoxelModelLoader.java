//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.voxel.models;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.api.content.Content;
import io.xol.chunkstories.api.content.Content.Voxels.VoxelModels;
import io.xol.chunkstories.api.voxel.VoxelSide;
import io.xol.chunkstories.api.voxel.models.VoxelModel;

public class VoxelModelLoader {
	private final Content.Voxels.VoxelModels store;
	
	public VoxelModelLoader(VoxelModels store) {
		this.store = store;
	}

	private final static Logger logger = LoggerFactory.getLogger("content.voxels.models");
	
	public CustomVoxelModel readBlockModel(Asset asset)
	{
		CustomVoxelModel voxelModel = null;
		
		logger().debug("Loading custom models file : " + asset);
		try
		{
			/*FileReader fileReader = new FileReader(voxelModelFile);*/
			Reader fileReader = asset.reader();
			BufferedReader reader = new BufferedReader(fileReader);

			String line = "";

			int ln = 0;

			String voxelModelName = null;

			List<float[]> verticesTemp = new ArrayList<float[]>();
			List<float[]> texcoordsTemp = new ArrayList<float[]>();
			List<float[]> normalsTemp = new ArrayList<float[]>();

			List<Byte> extrasTemps = new ArrayList<Byte>();

			List<boolean[]> cullingTemp = new ArrayList<boolean[]>();

			List<String> texturesNamesTemp = new ArrayList<String>();
			List<Integer> texturesOffsetsTemp = new ArrayList<Integer>();

			String currentTexture = "_top";
			boolean[] currentCull = new boolean[6];
			float jitterX = 0;
			float jitterY = 0;
			float jitterZ = 0;

			int verticesCounter = 0;
			while ((line = reader.readLine()) != null)
			{
				line = line.replace("\t", "");
				if (line.startsWith("#"))
				{
					// It's a comment, ignore.
				}
				else
				{
					if (voxelModelName == null && !line.equals(""))
					{
						voxelModelName = asset.getName().substring(asset.getName().lastIndexOf("/") + 1).replace(".model", "");
						if (!line.equals("default"))
							voxelModelName += "." + line;

						//Textures calculator
						currentTexture = "_top";
					}
					else if (line.startsWith("end"))
					{
						if (voxelModelName != null)
						{
							//Security:
							if (verticesTemp.size() % 3 != 0)
							{
								System.out.println(voxelModelName+" -> "+verticesTemp.size());
								System.exit(-1);
							}

							//Add last used texture
							texturesNamesTemp.add(currentTexture);
							texturesOffsetsTemp.add(verticesTemp.size());

							//Build list of them with offsets
							String[] texturesNames = new String[texturesNamesTemp.size()];
							int[] texturesOffsets = new int[texturesNamesTemp.size()];

							int indexInTextures = 0;
							for (String textureName : texturesNamesTemp)
							{
								texturesNames[indexInTextures] = textureName;
								texturesOffsets[indexInTextures] = texturesOffsetsTemp.get(indexInTextures);

								indexInTextures++;
							}

							float[] vertices = new float[verticesTemp.size() * 3];
							float[] texCoords = new float[verticesTemp.size() * 2];
							float[] normals = new float[verticesTemp.size() * 3];
							byte[] extras = new byte[extrasTemps.size()];

							if (verticesTemp.size() != extrasTemps.size())
							{
								System.out.println("FUCK OFF" + verticesTemp.size() + "+" + extrasTemps.size());
								System.exit(-111);
							}

							boolean[][] culling = new boolean[verticesTemp.size()][6];
							for (int i = 0; i < verticesTemp.size(); i++)
							{
								vertices[i * 3 + 0] = verticesTemp.get(i)[0];
								vertices[i * 3 + 1] = verticesTemp.get(i)[1];
								vertices[i * 3 + 2] = verticesTemp.get(i)[2];
								texCoords[i * 2 + 0] = texcoordsTemp.get(i)[0];
								texCoords[i * 2 + 1] = texcoordsTemp.get(i)[1];
								normals[i * 3 + 0] = normalsTemp.get(i)[0];
								normals[i * 3 + 1] = normalsTemp.get(i)[1];
								normals[i * 3 + 2] = normalsTemp.get(i)[2];

								extras[i] = extrasTemps.get(i);
							}
							for (int i = 0; i < verticesTemp.size() / 3; i++)
							{
								culling[i] = cullingTemp.get(i);
							}

							voxelModel = new CustomVoxelModel(store, voxelModelName, vertices, texCoords, texturesNames, texturesOffsets, normals, extras, culling, jitterX, jitterY, jitterZ);
							//models.put(voxelModelName, voxelModel);

							//Resets data accumulators
							verticesTemp.clear();
							texcoordsTemp.clear();
							normalsTemp.clear();
							cullingTemp.clear();
							extrasTemps.clear();

							//Resets textures
							texturesNamesTemp.clear();
							texturesOffsetsTemp.clear();

							//Resets culling engine
							currentCull = new boolean[6];

							//Reset fields
							jitterX = 0;
							jitterY = 0;
							jitterZ = 0;
						}
						else
							logger().warn("Parse error in asset " + asset + ", line " + ln + ", unexpected 'end' token.");

						voxelModelName = null;
					}
					else if (line.startsWith("texture"))
					{
						if (voxelModelName == null)
							continue;

						String[] splitted = line.split(" ");
						String newTextureName = splitted[1].replace("\'", "");
						//It can't crash from here so we can safely add the textures
						texturesNamesTemp.add(currentTexture);
						texturesOffsetsTemp.add(verticesTemp.size());

						currentTexture = newTextureName;
					}
					else if (line.startsWith("jitter"))
					{
						if (voxelModelName == null)
							continue;
						String[] splitted = line.split(" ");

						jitterX = Float.parseFloat(splitted[1]);
						jitterY = Float.parseFloat(splitted[2]);
						jitterZ = Float.parseFloat(splitted[3]);
					}
					else if (line.startsWith("v"))
					{
						if (voxelModelName != null)
						{
							// System.out.println("vv"+vertices.size());
							String[] splitted = line.split(" ");
							String[] vert = splitted[1].split(",");
							String[] tex = splitted[2].split(",");
							String[] nor = splitted[3].split(",");
							verticesTemp.add(new float[] { Float.parseFloat(vert[0]), Float.parseFloat(vert[1]), Float.parseFloat(vert[2]) });
							texcoordsTemp.add(new float[] { Float.parseFloat(tex[0]), Float.parseFloat(tex[1]) });

							//Normalizes normal at loading time
							Vector3f normalizeMe = new Vector3f(Float.parseFloat(nor[0]), Float.parseFloat(nor[1]), Float.parseFloat(nor[2]));
							normalizeMe.normalize();

							normalsTemp.add(new float[] { normalizeMe.x(), normalizeMe.y(), normalizeMe.z() });

							byte extra = 0;
							if (splitted.length >= 5)
								extra = Byte.parseByte(splitted[4]);

							extrasTemps.add(extra);

							verticesCounter++;
							if (verticesCounter >= 3)
							{
								cullingTemp.add(currentCull);
								verticesCounter = 0;
							}
						}
						else
							logger().warn("Parse error in asset " + asset + ", line " + ln + ", unexpected parameter.");
					}
					else if (line.startsWith("cull"))
					{
						currentCull = new boolean[6];
						for (String face : line.split(" "))
						{
							switch (face)
							{
							case "bottom":
								currentCull[VoxelSide.BOTTOM.ordinal()] = true;
								break;
							case "top":
								currentCull[VoxelSide.TOP.ordinal()] = true;
								break;
							case "left":
								currentCull[VoxelSide.LEFT.ordinal()] = true;
								break;
							case "right":
								currentCull[VoxelSide.RIGHT.ordinal()] = true;
								break;
							case "front":
								currentCull[VoxelSide.FRONT.ordinal()] = true;
								break;
							case "back":
								currentCull[VoxelSide.BACK.ordinal()] = true;
								break;
							}
						}
					}
					else if (line.startsWith("require"))
					{
						if (voxelModelName != null)
						{
							String[] splitted = line.split(" ");
							if (splitted.length == 2)
							{
								String toInclude = splitted[1];
								toInclude = toInclude.replace("~", voxelModelName.contains(".") ? voxelModelName.split("\\.")[0] : voxelModelName);
								VoxelModel includeMeh = store.getVoxelModel(toInclude);
								if (includeMeh != null)
								{
									//Iterates over included model
									for (int i = 0; i < includeMeh.getSizeInVertices(); i++)
									{
										verticesTemp.add(new float[] { includeMeh.getVertices()[i * 3 + 0], includeMeh.getVertices()[i * 3 + 1], includeMeh.getVertices()[i * 3 + 2] });
										texcoordsTemp.add(new float[] { includeMeh.getTexCoords()[i * 2 + 0], includeMeh.getTexCoords()[i * 2 + 1] });
										normalsTemp.add(new float[] { includeMeh.getNormals()[i * 3 + 0], includeMeh.getNormals()[i * 3 + 1], includeMeh.getNormals()[i * 3 + 2] });
										extrasTemps.add(includeMeh.getExtra()[i]);
									}

									//TODO it doesn't import their textures settings !
									for (boolean cul[] : includeMeh.getCulling())
										cullingTemp.add(cul);
								}
								else
									logger().warn("Can't require '" + toInclude + "'");

							}
						}
						else
							logger().warn("Parse error in asset " + asset + ", line " + ln + ", unexpected parameter.");
					}
				}
				ln++;
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return voxelModel;
	}

	public Logger logger() {
		return logger;
	}
}
