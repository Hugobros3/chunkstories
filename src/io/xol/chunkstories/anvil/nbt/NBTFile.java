package io.xol.chunkstories.anvil.nbt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class NBTFile
{
	private NBTCompound root;
	
	public NBTFile(File file)
	{
		assert file.exists();
		
		try {
			FileInputStream fis = new FileInputStream(file);
			GZIPInputStream zis = new GZIPInputStream(fis);

			root = (NBTCompound) NBTag.parseInputStream(zis);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		
	}
	
	public NBTCompound getRoot()
	{
		return root;
	}

}
