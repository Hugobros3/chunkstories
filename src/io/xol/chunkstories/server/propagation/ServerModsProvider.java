package io.xol.chunkstories.server.propagation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.xol.chunkstories.api.mods.Asset;
import io.xol.chunkstories.api.mods.Mod;
import io.xol.chunkstories.content.ModsManager;
import io.xol.chunkstories.content.mods.ModFolder;
import io.xol.chunkstories.content.mods.ModZip;
import io.xol.chunkstories.server.Server;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.engine.misc.FoldersUtils;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Provides mods for connected users
 */
public class ServerModsProvider
{
	//The mods string is just the list of md5 hashes of the mods enabled on the server
	File cacheFolder;
	Map<String, File> redistribuables = new HashMap<String, File>();
	String modsString;

	public ServerModsProvider(Server server)
	{
		ChunkStoriesLogger.getInstance().info("Starting to build server mods cache to provide to users");
		cacheFolder = new File("./cache/servermods-" + (int) (Math.random() * 100000) + "/");
		cacheFolder.mkdirs();
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			public void run()
			{
				System.out.println("Deleting servermods cache folder " + cacheFolder);
				FoldersUtils.deleteFolder(cacheFolder);
			}
		});

		//Build the modstring
		modsString = "";
		for (Mod mod : ModsManager.getCurrentlyLoadedMods())
		{
			String hash = mod.getMD5Hash();
			//System.out.println("Mod " + mod + " md5 = " + hash);
			ChunkStoriesLogger.getInstance().info("Building distribuable zipfile for mod " + mod.getModInfo().getName());
			if (mod instanceof ModZip)
			{
				System.out.println("Oh wait it already exists :D");
				redistribuables.put(hash, ((ModZip) mod).getZipFileLocation());
			}
			else if (mod instanceof ModFolder)
			{
				System.out.println("Making it from scratch.");
				File wipZipfile = new File(cacheFolder.getAbsolutePath() + "/" + hash + ".zip");

				try
				{
					FileOutputStream fos = new FileOutputStream(wipZipfile);
					ZipOutputStream zos = new ZipOutputStream(fos);

					byte[] buffer = new byte[4096];

					for (Asset asset : mod.assets())
					{
						ZipEntry entry = new ZipEntry(asset.getName().substring(2));
						zos.putNextEntry(entry);

						InputStream is = asset.read();
						int red;

						while ((red = is.read(buffer)) > 0)
							zos.write(buffer, 0, red);

						is.close();
					}

					zos.closeEntry();
					zos.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				
				redistribuables.put(hash, wipZipfile);
			}

			//Also add it to the string
			modsString += "md5:" + hash + ";";
		}
		if (modsString.length() > 1)
			modsString = modsString.substring(0, modsString.length() - 1);
	}

	public File obtainModRedistribuable(String md5)
	{
		return redistribuables.get(md5);
	}
	
	public String getModsString()
	{
		return modsString;
	}
}
