package io.xol.chunkstories.tools;

import io.xol.chunkstories.content.GameData;
import io.xol.chunkstories.world.WorldInfo;
import io.xol.chunkstories.world.generator.core.PerlinWorldGenerator;

import java.awt.Color;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class WorldMappingTool
{

	public static void main(String[] args)
	{
		try
		{
			GameData.reload();
			String seed = "lahaine";
			WorldInfo.WorldSize worldSize = WorldInfo.WorldSize.MEDIUM;
			System.out.println("Generating " + worldSize.name
					+ " worldmap for seed : " + seed);

			PerlinWorldGenerator pwa = new PerlinWorldGenerator();

			BufferedImage texture = new BufferedImage(
					worldSize.sizeInChunks * 32, worldSize.sizeInChunks * 32,
					Transparency.TRANSLUCENT);
			File outputFile = new File("wmt-output.png");
			if (!outputFile.exists())
				outputFile.createNewFile();

			int toProcessChunks = worldSize.sizeInChunks
					* worldSize.sizeInChunks;
			int processedChunks = 0;
			int percentage = 0;

			long shortestChunkGen = System.currentTimeMillis();
			long longestChunkGen = 0;
			long totalChunkGen = 0;
			long lastStartChunkGen = 0;

			totalChunkGen = System.currentTimeMillis();
			for (int cx = 0; cx < worldSize.sizeInChunks; cx++)
			{
				for (int cz = 0; cz < worldSize.sizeInChunks; cz++)
				{
					lastStartChunkGen = System.currentTimeMillis();
					// CubicChunk c = pwa.loadChunk(cx, 0, cz);
					for (int a = 0; a < 32; a++)
						for (int b = 0; b < 32; b++)
						{
							int height = pwa.getHeightAt(cx * 32 + a, cz * 32
									+ b);
							/*
							 * while((c.getDataAt(a, height, b) & 0xFF) != 0 &&
							 * height < 255) { height++; }
							 */
							Color color = new Color(height, height, height);

							// texture.setRGB(cx*32+a, cz*32+b, height*255*2
							// -255*255*255);
							texture.setRGB(cx * 32 + a, cz * 32 + b,
									color.getRGB());
							// System.out.println(color.getRGB()+":"+height);
						}
					// Chunk timing
					long currentTimeTook = System.currentTimeMillis()
							- lastStartChunkGen;
					if (currentTimeTook < shortestChunkGen)
						shortestChunkGen = currentTimeTook;
					if (currentTimeTook > longestChunkGen)
						longestChunkGen = currentTimeTook;

					processedChunks++;
					int newPercentage = processedChunks * 100 / toProcessChunks;
					if (newPercentage > percentage)
					{
						percentage = newPercentage;
						if (percentage % 10 == 0)
							System.out.println("Working ... " + processedChunks
									+ " out of " + toProcessChunks + " ("
									+ percentage + "%)");
					}
				}
			}
			totalChunkGen = System.currentTimeMillis() - totalChunkGen;

			ImageIO.write(texture, "PNG", outputFile);
			System.out.println("Done !");
			System.out.println("Total time (s) : " + totalChunkGen / 1000f);
			System.out.println("Average chunk time (ms) : " + totalChunkGen
					/ processedChunks);
			System.out.println("Min chunk time (ms) :" + shortestChunkGen);
			System.out.println("Max chunk time (ms) :" + longestChunkGen);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
