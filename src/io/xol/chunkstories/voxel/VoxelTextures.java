package io.xol.chunkstories.voxel;

import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import io.xol.chunkstories.GameData;
import io.xol.chunkstories.tools.ChunkStoriesLogger;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class VoxelTextures
{

	static Map<String, VoxelTexture> texMap = new HashMap<String, VoxelTexture>();
	static Map<String, Vector4f> colors = new HashMap<String, Vector4f>();

	public static int BLOCK_ATLAS_SIZE;
	public static int BLOCK_ATLAS_FACTOR;

	public static void buildTextureAtlas()
	{
		try
		{
			// Clear previous values
			texMap.clear();
			colors.clear();
			// Compute all sizes first.
			int totalSurfacedNeeded = 0;
			//File folder = new File("./res/voxels/textures/");
			// TODO fix for gamemodes
			// Get all sizes :
			List<VoxelTexture> sizes = new ArrayList<VoxelTexture>();

			//for (File f : folder.listFiles())
			Iterator<Entry<String, File>> allFiles = GameData.getAllUniqueEntries();
			Entry<String, File> entry;
			File f;
			while (allFiles.hasNext())
			{
				entry = allFiles.next();
				if (entry.getKey().startsWith("./res/voxels/textures/"))
				{
					String name = entry.getKey().replace("./res/voxels/textures/", "");
					if(name.contains("/"))
						continue;
					f = entry.getValue();
					if (!f.isDirectory() && f.exists() && f.getName().endsWith(".png"))
					{
						String textureName = f.getName().replace(".png", "");
						//System.out.println("texName:"+textureName+" "+entry.getKey());
						if (!texMap.containsKey(textureName))
						{
							VoxelTexture vt = new VoxelTexture(textureName);
							vt.imageFileDimensions = getImageSize(f);

							sizes.add(vt);
							// texMap.put(textureName, vt);
							totalSurfacedNeeded += vt.imageFileDimensions * vt.imageFileDimensions;
						}
					}
				}
			}
			//TODO do also the mods/ dir
			// Sort.
			Collections.sort(sizes, new Comparator<VoxelTexture>()
			{
				@Override
				public int compare(VoxelTexture a, VoxelTexture b)
				{
					return Integer.compare(b.imageFileDimensions, a.imageFileDimensions);
				}
			});
			for (VoxelTexture vt : sizes)
			{
				// System.out.println(vt.imageFileDimensions);
				texMap.put(vt.name, vt);
			}
			// Make an appropriately sized texture atlas
			int sizeRequired = 16;
			for (int i = 4; i < 14; i++)
			{
				int iSize = (int) Math.pow(2, i);
				if (iSize * iSize >= totalSurfacedNeeded)
				{
					sizeRequired = iSize;
					break;
				}
			}
			ChunkStoriesLogger.getInstance().info("At least " + sizeRequired + " by " + sizeRequired + " for TextureAtlas (surfacedNeeded : " + totalSurfacedNeeded + ")");
			// Delete previous atlases
			File diffuseTextureFile = new File("./res/textures/tiles_merged_diffuse.png");
			if (diffuseTextureFile.exists())
				diffuseTextureFile.delete();

			File normalTextureFile = new File("./res/textures/tiles_merged_normal.png");
			if (normalTextureFile.exists())
				normalTextureFile.delete();

			File materialTextureFile = new File("./res/textures/tiles_merged_material.png");
			if (materialTextureFile.exists())
				materialTextureFile.delete();
			// Build the new one
			boolean loadedOK = false;
			while (!loadedOK && sizeRequired <= 4096) // Security to prevend
														// HUGE-ASS textures
			{
				// We need this
				BLOCK_ATLAS_SIZE = sizeRequired;
				BLOCK_ATLAS_FACTOR = 32768 / BLOCK_ATLAS_SIZE;
				loadedOK = true;
				// Create boolean bitfield
				boolean[][] used = new boolean[sizeRequired / 16][sizeRequired / 16];

				BufferedImage diffuseTexture = new BufferedImage(sizeRequired, sizeRequired, Transparency.TRANSLUCENT);
				BufferedImage normalTexture = new BufferedImage(sizeRequired, sizeRequired, Transparency.TRANSLUCENT);
				BufferedImage materialTexture = new BufferedImage(sizeRequired, sizeRequired, Transparency.TRANSLUCENT);

				BufferedImage imageBuffer;

				for (VoxelTexture vt : sizes)
				{
					// Find a free spot on the atlas
					boolean foundSpot = false;
					int spotX = 0, spotY = 0;
					for (int a = 0; (a < sizeRequired / 16 && !foundSpot); a++)
						for (int b = 0; (b < sizeRequired / 16 && !foundSpot); b++)
						{
							if (used[a][b] == false && a + vt.imageFileDimensions / 16 <= sizeRequired / 16 && b + vt.imageFileDimensions / 16 <= sizeRequired / 16) // Unused
							{
								boolean usedAlready = false;
								// Not pretty loops that do clamped space checks
								for (int i = 0; (i < vt.imageFileDimensions / 16 && a + i < sizeRequired / 16); i++)
									for (int j = 0; (j < vt.imageFileDimensions / 16 && b + j < sizeRequired / 16); j++)
										if (used[a + i][b + j] == true) // Well
																		// fuck
																		// it
											usedAlready = true;
								if (!usedAlready)
								{
									spotX = a * 16;
									spotY = b * 16;
									vt.atlasS = spotX * BLOCK_ATLAS_FACTOR;
									vt.atlasT = spotY * BLOCK_ATLAS_FACTOR;
									vt.atlasOffset = vt.imageFileDimensions * BLOCK_ATLAS_FACTOR;
									foundSpot = true;
									for (int i = 0; (i < vt.imageFileDimensions / 16 && a + i < sizeRequired / 16); i++)
										for (int j = 0; (j < vt.imageFileDimensions / 16 && b + j < sizeRequired / 16); j++)
											used[a + i][b + j] = true;
								}
							}
						}
					if (!foundSpot)
					{
						System.out.println("Failed to find a space to place the texture in. Retrying with a larger atlas.");
						loadedOK = false;
						break;
					}

					imageBuffer = ImageIO.read(GameData.getTextureFileLocation("./res/voxels/textures/" + vt.name + ".png"));

					float alphaTotal = 0;
					int nonNullPixels = 0;
					Vector3f color = new Vector3f();
					for (int x = 0; x < vt.imageFileDimensions; x++)
					{
						for (int y = 0; y < vt.imageFileDimensions; y++)
						{
							int rgb = imageBuffer.getRGB(x, y);
							diffuseTexture.setRGB(spotX + x, spotY + y, rgb);
							float alpha = ((rgb & 0xFF000000) >>> 24) / 255f;
							// System.out.println("a:"+alpha);
							alphaTotal += alpha;
							if (alpha > 0)
								nonNullPixels++;
							float red = ((rgb & 0xFF0000) >> 16) / 255f * alpha;
							float green = ((rgb & 0x00FF00) >> 8) / 255f * alpha;
							float blue = (rgb & 0x0000FF) / 255f * alpha;
							Vector3f.add(color, new Vector3f(red, green, blue), color);
						}
					}

					color.scale(1f / alphaTotal);
					if (nonNullPixels > 0)
						alphaTotal /= nonNullPixels;

					colors.put(vt.name, new Vector4f(color.x, color.y, color.z, alphaTotal));
					// Do also the normal maps !
					File normalMap = GameData.getTextureFileLocation("./res/voxels/textures/normal/" + vt.name + ".png");
					if (normalMap == null || !normalMap.exists())
						normalMap = GameData.getTextureFileLocation("./res/voxels/textures/normal/notex.png");

					imageBuffer = ImageIO.read(normalMap);
					for (int x = 0; x < vt.imageFileDimensions; x++)
					{
						for (int y = 0; y < vt.imageFileDimensions; y++)
						{
							int rgb = imageBuffer.getRGB(x % imageBuffer.getWidth(), y % imageBuffer.getHeight());
							normalTexture.setRGB(spotX + x, spotY + y, rgb);
						}
					}
					// And the materials !
					File materialMap = GameData.getTextureFileLocation("./res/voxels/textures/material/" + vt.name + ".png");
					if (materialMap == null || !materialMap.exists())
						materialMap = GameData.getTextureFileLocation("./res/voxels/textures/material/notex.png");

					imageBuffer = ImageIO.read(materialMap);
					for (int x = 0; x < vt.imageFileDimensions; x++)
					{
						for (int y = 0; y < vt.imageFileDimensions; y++)
						{
							int rgb = imageBuffer.getRGB(x % imageBuffer.getWidth(), y % imageBuffer.getHeight());
							materialTexture.setRGB(spotX + x, spotY + y, rgb);
						}
					}
				}
				if (loadedOK)
				{
					// save it son
					ImageIO.write(diffuseTexture, "PNG", diffuseTextureFile);
					ImageIO.write(normalTexture, "PNG", normalTextureFile);
					ImageIO.write(materialTexture, "PNG", materialTextureFile);
				}
				else
					// It's too small, initial estimation was wrong !
					sizeRequired *= 2;
			}
			// Read textures metadata
			readTexturesMeta(GameData.getFileLocation("./res/voxels/textures/meta.txt"));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private static void readTexturesMeta(File f)
	{
		if (!f.exists())
			return;
		try
		{
			FileReader fileReader = new FileReader(f);
			BufferedReader reader = new BufferedReader(fileReader);

			String line = "";

			VoxelTexture vt = null;
			int ln = 0;
			while ((line = reader.readLine()) != null)
			{
				if (line.startsWith("#"))
				{
					// It's a comment, ignore.
				}
				else
				{
					if (line.startsWith("texture"))
					{
						if (vt != null)
							System.out.println("Warning ! Parse error in file " + f + ", line " + ln + ", unexpected 'texture' token.");
						String splitted[] = line.split(" ");
						String name = splitted[1];

						vt = texMap.get(name);
					}
					else if (line.startsWith("end"))
					{
						if (vt != null)
						{
							vt = null;
						}
						else
							System.out.println("Warning ! Parse error in file " + f + ", line " + ln + ", unexpected 'end' token.");
					}
					else if (line.startsWith("\t"))
					{
						if (vt != null)
						{
							// System.out.println("Debug : loading voxel parameter : "+line);
							String splitted[] = (line.replace(" ", "").replace("\t", "")).split(":");
							String parameterName = splitted[0];
							String parameterValue = splitted[1];
							switch (parameterName)
							{
							case "textureScale":
								vt.textureScale = Integer.parseInt(parameterValue);
								break;
							default:
								System.out.println("Warning ! Parse error in file " + f + ", line " + ln + ", unknown parameter '" + parameterName + "'");
								break;
							}
						}
						else
							System.out.println("Warning ! Parse error in file " + f + ", line " + ln + ", unexpected parameter.");
					}
				}
				ln++;
			}
			reader.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static int getImageSize(File file)
	{
		try
		{
			ImageReader reader = ImageIO.getImageReadersBySuffix("png").next();
			ImageInputStream stream = new FileImageInputStream(file);
			reader.setInput(stream);
			int size = reader.getWidth(reader.getMinIndex());
			return size;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return 0;
		}
	}

	public static VoxelTexture getVoxelTexture(String textureName)
	{
		if (texMap.containsKey(textureName))
			return texMap.get(textureName);
		return texMap.get("notex");
		// return new VoxelTexture(null, "notex");
	}

	public static Vector3f getTextureColorAVG(String name)
	{
		Vector4f colorAlpha = getTextureColorAlphaAVG(name);
		return new Vector3f(colorAlpha.x, colorAlpha.y, colorAlpha.z);
	}

	public static Vector4f getTextureColorAlphaAVG(String name)
	{
		if (colors.containsKey(name))
			return colors.get(name);
		return colors.get("notex");
	}
}
