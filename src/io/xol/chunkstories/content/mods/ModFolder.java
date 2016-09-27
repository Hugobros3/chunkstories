package io.xol.chunkstories.content.mods;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.xol.chunkstories.api.utils.IterableIterator;
import io.xol.chunkstories.content.mods.exceptions.ModLoadFailureException;
import io.xol.chunkstories.tools.ChunkStoriesLogger;

public class ModFolder extends Mod
{
	final File folder;
	final Map<String, ModFolderAsset> assets = new HashMap<String, ModFolderAsset>();

	public static void main(String[] a)
	{
		try
		{
			new ModFolder(new File("res/"));
		}
		catch (ModLoadFailureException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public ModFolder(File folder) throws ModLoadFailureException
	{
		this.folder = folder;
		
		recursiveFolderRead(folder);
		
		loadModInformation(getAssetByName("./mod.txt"));
	}
	
	private void recursiveFolderRead(File file)
	{
		if(file.isDirectory())
		{
			for(File f : file.listFiles())
				recursiveFolderRead(f);
		}
		else
		{
			String fileName = file.getAbsolutePath().substring(folder.getAbsolutePath().length() + 1, file.getAbsolutePath().length());
			fileName = fileName.replace('\\', '/');
			String assetName = "./" + fileName;
			
			//System.out.println(assetName);
			assets.put(assetName, new ModFolderAsset(assetName, file));
		}
	}

	@Override
	public Asset getAssetByName(String name)
	{
		return assets.get(name);
	}

	class ModFolderAsset implements Asset
	{
		String assetName;
		File file;

		public ModFolderAsset(String assetName, File file)
		{
			this.assetName = assetName;
			this.file = file;
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
				return new FileInputStream(file);
			}
			catch (IOException e)
			{
				ChunkStoriesLogger.getInstance().warning("Failed to read asset : " + assetName + " from " + ModFolder.this);
				e.printStackTrace();
				return null;
			}
		}

		@Override
		public Mod getSource()
		{
			return ModFolder.this;
		}
		
		public String toString()
		{
			return "[Asset: "+assetName+" from mod "+ModFolder.this+"]";
		}
	}
	@Override
	public String getMD5Hash()
	{
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public void close()
	{
	}

	@Override
	public IterableIterator<Asset> assets()
	{
		return new IterableIterator<Asset>() {
			Iterator<ModFolderAsset> iz = assets.values().iterator();

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

}
