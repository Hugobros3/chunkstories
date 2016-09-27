package io.xol.chunkstories.content.mods;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import io.xol.chunkstories.api.utils.IterableIterator;
import io.xol.chunkstories.content.mods.exceptions.MalformedModTxtException;
import io.xol.chunkstories.content.mods.exceptions.ModLoadFailureException;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class Mod
{
	private String name;
	private String version = "undefined";
	
	Mod() throws ModLoadFailureException
	{
		
	}
	
	void loadModInformation(Asset modTxt) throws MalformedModTxtException
	{
		if(modTxt == null || !modTxt.getName().endsWith("mod.txt"))
			throw new MalformedModTxtException(this);
		
		//Try to read the ressource
		try
		{
			BufferedReader reader = new BufferedReader(new InputStreamReader(modTxt.read(), "UTF-8"));
			
			String line;
			while((line = reader.readLine()) != null)
			{
				if(line.contains("="))
				{
					String left = line.substring(0, line.indexOf('='));
					String right = line.substring(line.indexOf('=') + 1, line.length());
					System.out.println(left+"="+right);
					
					if(left.equals("name"))
						this.name = right;
					else if(left.equals("version"))
						this.version = right;
				}
			}
			reader.close();
		}
		catch (IOException e)
		{
			throw new MalformedModTxtException(this);
		}
		
		//Requires a name to be set, at least
		//TODO change it, require more
		if(this.name == null)
			throw new MalformedModTxtException(this);
		
	}
	
	public String getName()
	{
		return name;
	}
	
	public String getVersion()
	{
		return version;
	}
	
	public abstract Asset getAssetByName(String name);
	
	public abstract IterableIterator<Asset> assets();
	
	public void loadClasses()
	{
		
	}
	
	public abstract String getMD5Hash();
	
	public abstract void close();
}
