//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.content;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.chunkstories.api.GameContext;
import xyz.chunkstories.api.content.Asset;
import xyz.chunkstories.content.mods.ModFolderAsset;
import xyz.chunkstories.content.mods.ModZipAsset;
import xyz.chunkstories.util.FoldersUtils;

/**
 * A class to help dumb parsers/loaders that don't have support for loading file
 * through our virtual FS
 */
public class AssetAsFileHelper {

	private static Logger logger = LoggerFactory.getLogger("content.cachehelper");

	static AtomicBoolean createCacheFolder = new AtomicBoolean(false);
	static File cacheFolder = null;

	public static File cacheAsset(Asset asset, GameContext context) {
		// Mod folders: we just pass the file
		if (asset instanceof ModFolderAsset) {
			return ((ModFolderAsset) asset).getFile();
		} else if (asset instanceof ModZipAsset) {
			File cacheFolder = getCacheFolder();

			File extracted = extractAssert(asset, cacheFolder);
			// Hack on hack: obj files will require some stuff next to them
			if (asset.getName().endsWith(".obj")) {
				Asset materialFileAsset = context.getContent()
						.getAsset(asset.getName().substring(0, asset.getName().length() - 4) + ".mtl");
				if (materialFileAsset != null)
					extractAssert(materialFileAsset, cacheFolder);
			}

			return extracted;
		} else
			throw new UnsupportedOperationException("What type is this asset ? " + asset);
	}

	private static File extractAssert(Asset asset, File cacheFolder) {
		logger.debug("Extract asset " + asset.getName() + " to " + cacheFolder);
		try {
			File extractTo = new File(cacheFolder.getAbsolutePath() + "/" + asset.getName());
			FileOutputStream fos = new FileOutputStream(extractTo);
			InputStream is = asset.read();
			byte[] buffer = new byte[4096];
			while (is.available() > 0) {
				int r = is.read(buffer);
				fos.write(buffer, 0, r);
			}
			is.close();
			fos.close();

			return extractTo;
		} catch (IOException e) {
			throw new RuntimeException("");
		}
	}

	private static File getCacheFolder() {
		// Obtain a cache folder
		if (createCacheFolder.compareAndSet(false, true)) {
			cacheFolder = new File(
					GameDirectory.getGameFolderPath() + "/cache/assimp/" + ((int) (Math.random() * 10000)));
			cacheFolder.mkdirs();
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					System.out.println("Deleting cache folder " + cacheFolder);
					FoldersUtils.deleteFolder(cacheFolder);
				}
			});
		}

		return cacheFolder;
	}
}
