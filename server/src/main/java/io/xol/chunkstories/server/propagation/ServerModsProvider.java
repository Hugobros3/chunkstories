//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.server.propagation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.api.content.mods.Mod;
import io.xol.chunkstories.content.mods.ModFolder;
import io.xol.chunkstories.content.mods.ModZip;
import io.xol.chunkstories.server.DedicatedServer;
import io.xol.chunkstories.util.FoldersUtils;

/**
 * Provides mods for connected users
 */
public class ServerModsProvider {
	// The mods string is just the list of md5 hashes of the mods enabled on the
	// server
	File cacheFolder;
	Map<String, File> redistribuables = new HashMap<String, File>();
	String modsString;

	public ServerModsProvider(DedicatedServer server) {
		server.logger().info("Starting to build server mods cache to provide to users");
		cacheFolder = new File("./cache/servermods-" + (int) (Math.random() * 100000) + "/");
		cacheFolder.mkdirs();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				System.out.println("Deleting servermods cache folder " + cacheFolder);
				FoldersUtils.deleteFolder(cacheFolder);
			}
		});

		// Build the modstring
		modsString = "";
		for (Mod mod : server.getContent().modsManager().getCurrentlyLoadedMods()) {
			String hash = mod.getMD5Hash();
			long size;

			server.logger().info("Building distribuable zipfile for mod " + mod.getModInfo().getName());
			if (mod instanceof ModZip) {
				server.logger().info("Nevermind, that mod is already in a .zip format, moving on");
				redistribuables.put(hash, ((ModZip) mod).getZipFileLocation());
				size = ((ModZip) mod).getZipFileLocation().length();
			} else if (mod instanceof ModFolder) {
				server.logger().info("Making it from scratch.");
				File wipZipfile = new File(cacheFolder.getAbsolutePath() + "/" + hash + ".zip");

				try {
					FileOutputStream fos = new FileOutputStream(wipZipfile);
					ZipOutputStream zos = new ZipOutputStream(fos);

					byte[] buffer = new byte[4096];

					for (Asset asset : mod.assets()) {
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
				} catch (IOException e) {
					e.printStackTrace();
				}

				redistribuables.put(hash, wipZipfile);
				size = wipZipfile.length();
			} else
				throw new UnsupportedOperationException("Mods can't be anything but a .zip or a folder");

			// Also add it to the string
			modsString += mod.getModInfo().getInternalName() + ":" + hash + ":" + size + ";";
		}

		// Remove the last ;
		if (modsString.length() > 1)
			modsString = modsString.substring(0, modsString.length() - 1);
	}

	public File obtainModRedistribuable(String md5) {
		return redistribuables.get(md5);
	}

	public String getModsString() {
		return modsString;
	}
}
