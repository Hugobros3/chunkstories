package io.xol.chunkstories.content.mods;

import java.io.File;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import io.xol.chunkstories.api.mods.Asset;
import io.xol.chunkstories.api.mods.Mod;
import io.xol.chunkstories.api.utils.IterableIterator;
import io.xol.chunkstories.content.mods.exceptions.ModLoadFailureException;
import io.xol.chunkstories.tools.ChunkStoriesLogger;

public class ModZip extends ModImplementation
{
	final File fileLocation;
	final ZipFile zipFile;
	final Map<String, ModZipAsset> assets = new HashMap<String, ModZipAsset>();

	public static void main(String[] a)
	{
		try
		{
			new ModZip(new File("dogez_content.zip"));
		}
		catch (ModLoadFailureException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String toString()
	{
		return "[ModZip: "+fileLocation.getAbsolutePath()+"]";
	}

	public ModZip(File zippedMod) throws ModLoadFailureException
	{
		fileLocation = zippedMod;
		try
		{
			this.zipFile = new ZipFile(zippedMod);

			Enumeration<? extends ZipEntry> e = zipFile.entries();
			while (e.hasMoreElements())
			{
				ZipEntry entry = e.nextElement();
				if (!entry.isDirectory())
				{
					String assetName = "./" + entry.getName();

					//System.out.println("Found asset " + assetName);
					assets.put(assetName, new ModZipAsset(assetName, entry));
				}
			}

			this.modInfo = new ModInfoImplementation(this, getAssetByName("./mod.txt").read());
			//loadModInformation(getAssetByName("./mod.txt"));
		}
		catch (IOException e)
		{
			throw new ModLoadFailureException(this, "Zip file not found or malformed");
		}
	}

	@Override
	public Asset getAssetByName(String name)
	{
		return assets.get(name);
	}

	class ModZipAsset implements Asset
	{

		String assetName;
		ZipEntry entry;

		public ModZipAsset(String assetName, ZipEntry entry)
		{
			this.assetName = assetName;
			this.entry = entry;
		}

		@Override
		public String getName()
		{
			return assetName;
		}

		@Override
		public InputStream read()
		{
			try
			{
				return zipFile.getInputStream(entry);
			}
			catch (IOException e)
			{
				ChunkStoriesLogger.getInstance().warning("Failed to read asset : " + assetName + " from " + ModZip.this);
				e.printStackTrace();
				return null;
			}
		}

		@Override
		public Mod getSource()
		{
			return ModZip.this;
		}
		
		public String toString()
		{
			return "[Asset: "+assetName+" from mod "+ModZip.this+"]";
		}
	}

	@Override
	public void close()
	{
		try
		{
			zipFile.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public IterableIterator<Asset> assets()
	{
		return new IterableIterator<Asset>()
		{
			Iterator<ModZipAsset> iz = assets.values().iterator();

			@Override
			public boolean hasNext()
			{
				return iz.hasNext();
			}

			@Override
			public Asset next()
			{
				return iz.next();
			}
		};
	}

	@Override
	public String getLoadString()
	{
		return fileLocation.getAbsolutePath();
	}
	
	public File getZipFileLocation()
	{
		return fileLocation;
	}
}
