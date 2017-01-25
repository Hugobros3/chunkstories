package io.xol.chunkstories.anvil;

import java.io.File;

import io.xol.chunkstories.anvil.nbt.NBTFile;
import io.xol.chunkstories.anvil.nbt.NBTString;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class MinecraftWorld
{
	public static void main(String[] a)
	{
		MinecraftWorld testWorld = new MinecraftWorld(new File("namalsk-map/"));
	}
	
	private final File folder;
	private final NBTFile nbtFile;
	
	private final String levelName;
	
	public MinecraftWorld(File folder)
	{
		this.folder = folder;
		
		//Tries to read level.dat
		File levelDat = new File(this.folder.getAbsolutePath()+"/level.dat");
		assert levelDat.exists();
		
		nbtFile = new NBTFile(levelDat);
		
		levelName = ((NBTString)nbtFile.getRoot().getTag("Data.LevelName")).getText();
		
		//int spawnX = ((NBTInt) nbtFile.getRoot().getTag("Data.SpawnX")).getData();
		//System.out.println(spawnX);
	}
	
	public String getName()
	{
		return levelName;
	}
	
	public NBTFile getLevelDotDat()
	{
		return nbtFile;
	}
	
	public static int blockToRegionCoordinates(int blockCoordinates)
	{
		if (blockCoordinates >= 0)
		{
			return (int) Math.floor(blockCoordinates / 512f);
		}
		blockCoordinates = -blockCoordinates;
		return -(int) Math.floor(blockCoordinates / 512f) - 1;
	}
	
	public MinecraftRegion getRegion(int regionCoordinateX, int regionCoordinateZ)
	{
		File regionFile = new File(folder.getAbsolutePath() + "/region/r." + regionCoordinateX + "." + regionCoordinateZ + ".mca");
		
		if (regionFile.exists())
		{
			return new MinecraftRegion(regionFile);
		}
		else
		{
			return null;
		}
	}
}
