//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.content.mods;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.LoggerFactory;

import xyz.chunkstories.api.content.Asset;
import xyz.chunkstories.api.content.mods.Mod;
import xyz.chunkstories.api.exceptions.content.mods.ModLoadFailureException;
import xyz.chunkstories.api.util.IterableIterator;

public class ModZip extends ModImplementation {
	final File fileLocation;
	final ZipFile zipFile;
	final Map<String, ModZipAsset> assets = new HashMap<String, ModZipAsset>();

	public static void main(String[] a) {
		try {
			new ModZip(new File("dogez_content.zip"));
		} catch (ModLoadFailureException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String toString() {
		return "[ModZip: " + fileLocation.getAbsolutePath() + "]";
	}

	public ModZip(File zippedMod) throws ModLoadFailureException {
		fileLocation = zippedMod;
		try {
			this.zipFile = new ZipFile(zippedMod);

			Enumeration<? extends ZipEntry> e = zipFile.entries();
			while (e.hasMoreElements()) {
				ZipEntry entry = e.nextElement();
				if (!entry.isDirectory()) {
					String assetName = entry.getName();
					assets.put(assetName, new ModZipAsset(this, assetName, entry));
				}
			}

			this.setModInfo(ModInfoLoaderKt.loadModInfo(getAssetByName("modInfo.json").reader()));
		} catch (IOException e) {
			throw new ModLoadFailureException(this, "Zip file not found or malformed");
		}

		setLogger(LoggerFactory.getLogger("mod." + this.getModInfo().getInternalName()));
	}

	@Override
	public Asset getAssetByName(String name) {
		return assets.get(name);
	}



	@Override
	public void close() {
		try {
			zipFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public IterableIterator<Asset> assets() {
		return new IterableIterator<Asset>() {
			Iterator<ModZipAsset> iz = assets.values().iterator();

			@Override
			public boolean hasNext() {
				return iz.hasNext();
			}

			@Override
			public Asset next() {
				return iz.next();
			}
		};
	}

	@Override
	public String getLoadString() {
		return fileLocation.getAbsolutePath();
	}

	public File getZipFileLocation() {
		return fileLocation;
	}
}
