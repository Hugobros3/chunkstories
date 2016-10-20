package io.xol.chunkstories.voxel;

import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import io.xol.engine.math.lalgb.Vector3f;
import io.xol.engine.math.lalgb.Vector4f;
import io.xol.chunkstories.content.Mods;
import io.xol.chunkstories.content.Mods.AssetHierarchy;
import io.xol.chunkstories.content.mods.Asset;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class VoxelTextures
{
	static Map<String, VoxelTexture> texMap = new HashMap<String, VoxelTexture>();
	static int uniquesIds = 0;
	//static Map<String, Vector4f> colors = new HashMap<String, Vector4f>();

	public static int BLOCK_ATLAS_SIZE;
	public static int BLOCK_ATLAS_FACTOR;

	public static void buildTextureAtlas()
	{
		try
		{
			// Clear previous values
			texMap.clear();
			//colors.clear();
			
			// Compute all sizes first.
			int totalSurfacedNeeded = 0;
			//File folder = new File("./res/voxels/textures/");
			// Get all sizes :
			List<VoxelTexture> voxelTexturesSortedBySize = new ArrayList<VoxelTexture>();

			//for (File f : folder.listFiles())
			
			Iterator<AssetHierarchy> allFiles = Mods.getAllUniqueEntries();
			//Iterator<Entry<String, Deque<File>>> allFiles = GameContent.getAllUniqueEntries();
			AssetHierarchy entry;
			Asset f;
			while (allFiles.hasNext())
			{
				entry = allFiles.next();
				if (entry.getName().startsWith("./voxels/textures/"))
				{
					String name = entry.getName().replace("./voxels/textures/", "");
					if(name.contains("/"))
						continue;
					f = entry.topInstance();
					if (f.getName().endsWith(".png"))
					{
						String textureName = name.replace(".png", "");
						//System.out.println("texName:"+textureName+" "+entry.getKey());
						if (!texMap.containsKey(textureName))
						{
							VoxelTexture voxelTexture = new VoxelTexture(textureName, uniquesIds);
							uniquesIds++;
							
							voxelTexture.imageFileDimensions = getImageSize(f);

							voxelTexturesSortedBySize.add(voxelTexture);
							totalSurfacedNeeded += voxelTexture.imageFileDimensions * voxelTexture.imageFileDimensions;
						}
					}
				}
			}
			// Sort them by size
			Collections.sort(voxelTexturesSortedBySize, new Comparator<VoxelTexture>()
			{
				@Override
				public int compare(VoxelTexture a, VoxelTexture b)
				{
					return Integer.compare(b.imageFileDimensions, a.imageFileDimensions);
				}
			});
			for (VoxelTexture voxelTexture : voxelTexturesSortedBySize)
			{
				// System.out.println(vt.imageFileDimensions);
				texMap.put(voxelTexture.name, voxelTexture);
			}
			
			// Estimates the required texture atlas size by surface
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
			
			//ChunkStoriesLogger.getInstance().info("At least " + sizeRequired + " by " + sizeRequired + " for TextureAtlas (surfacedNeeded : " + totalSurfacedNeeded + ")");
			
			// Delete previous atlases
			File diffuseTextureFile = new File("./res/textures/tiles_merged_albedo.png");
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
			
			while (!loadedOK && sizeRequired <= 8192) // Security to prevend
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

				for (VoxelTexture vt : voxelTexturesSortedBySize)
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

					imageBuffer = ImageIO.read(Mods.getAsset("./voxels/textures/" + vt.name + ".png").read());
					//imageBuffer = ImageIO.read(GameContent.getTextureFileLocation());

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

					vt.color = new Vector4f(color.x, color.y, color.z, alphaTotal);
					
					//colors.put(vt.name, new Vector4f(color.x, color.y, color.z, alphaTotal));
					// Do also the normal maps !
					Asset normalMap = Mods.getAsset("./voxels/textures/normal/" + vt.name + ".png");
					if (normalMap == null)
						normalMap = Mods.getAsset("./voxels/textures/normal/notex.png");

					imageBuffer = ImageIO.read(normalMap.read());
					for (int x = 0; x < vt.imageFileDimensions; x++)
					{
						for (int y = 0; y < vt.imageFileDimensions; y++)
						{
							int rgb = imageBuffer.getRGB(x % imageBuffer.getWidth(), y % imageBuffer.getHeight());
							normalTexture.setRGB(spotX + x, spotY + y, rgb);
						}
					}
					// And the materials !
					Asset materialMap = Mods.getAsset("./voxels/textures/material/" + vt.name + ".png");
					if (materialMap == null)
						materialMap = Mods.getAsset("./voxels/textures/material/notex.png");

					imageBuffer = ImageIO.read(materialMap.read());
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
			//TODO read all overrides in priority
			readTexturesMeta(Mods.getAsset("./voxels/textures/meta.txt"));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private static void readTexturesMeta(Asset asset)
	{
		if (asset == null)
			return;
		try
		{
			BufferedReader reader = new BufferedReader(new InputStreamReader(asset.read()));

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
							System.out.println("Warning ! Parse error in file " + asset + ", line " + ln + ", unexpected 'texture' token.");
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
							System.out.println("Warning ! Parse error in file " + asset + ", line " + ln + ", unexpected 'end' token.");
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
								System.out.println("Warning ! Parse error in file " + asset + ", line " + ln + ", unknown parameter '" + parameterName + "'");
								break;
							}
						}
						else
							System.out.println("Warning ! Parse error in file " + asset + ", line " + ln + ", unexpected parameter.");
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

	public static int getImageSize(Asset asset)
	{
		try
		{
			ImageReader reader = ImageIO.getImageReadersBySuffix("png").next();
			ImageInputStream stream = ImageIO.createImageInputStream(asset.read());
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
		//textureName = "kek";
		
		if (texMap.containsKey(textureName))
			return texMap.get(textureName);
		return texMap.get("notex");
		// return new VoxelTexture(null, "notex");
	}
	
	public static Iterator<VoxelTexture> getAllVoxelTextures()
	{
		return texMap.values().iterator();
	}

	/*public static Vector3f getTextureColorAVG(String name)
	{
		Vector4f colorAlpha = getTextureColorAlphaAVG(name);
		return new Vector3f(colorAlpha.x, colorAlpha.y, colorAlpha.z);
	}*/

	/*public static Vector4f getTextureColorAlphaAVG(String name)
	{
		if (colors.containsKey(name))
			return colors.get(name);
		return colors.get("notex");
	}*/
}
