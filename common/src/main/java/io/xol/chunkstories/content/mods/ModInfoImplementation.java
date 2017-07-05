package io.xol.chunkstories.content.mods;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import io.xol.chunkstories.api.exceptions.content.mods.MalformedModTxtException;
import io.xol.chunkstories.api.mods.Mod;
import io.xol.chunkstories.api.mods.ModInfo;
import io.xol.chunkstories.materials.GenericConfigurable;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ModInfoImplementation extends GenericConfigurable implements ModInfo
{
	private Mod mod;
	
	private final String internalName;
	private String name;
	private String version;// = "undefined";
	private String description;// = "No description given";
	
	public Mod getMod()
	{
		return mod;
	}
	
	@Override
	public String getName()
	{
		return name;
	}
	
	@Override
	public String getVersion()
	{
		return version;
	}

	@Override
	public String getDescription()
	{
		return description;
	}
	
	public ModInfoImplementation(Mod mod, InputStream inputStream) throws MalformedModTxtException
	{
		super();
		
		if(inputStream == null)
			throw new MalformedModTxtException(this);
		
		try {
			load(new BufferedReader(new InputStreamReader(inputStream)));
		}
		catch(IOException e) {
			throw new MalformedModTxtException(this);
		}
		
		this.internalName = this.resolveProperty("internalName", null);
		this.name = this.resolveProperty("name", "<internalName");
		this.version = this.resolveProperty("version", "1.0");
		this.description = this.resolveProperty("description", "Please provide a description in your mod.txt");

		//Requires a name to be set, at least
		if(this.internalName == null)
			throw new MalformedModTxtException(this);
		
		//Try to read the ressource
		/*try
		{
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
			
			String line;
			while((line = reader.readLine()) != null)
			{
				if(line.contains("="))
				{
					String left = line.substring(0, line.indexOf('='));
					String right = line.substring(line.indexOf('=') + 1, line.length());
					//System.out.println(left+"="+right);
					
					if(left.equals("name"))
						this.name = right;
					else if(left.equals("version"))
						this.version = right;
					else if(left.equals("description"))
						this.description = right.replace("\\n", "\n");
				}
			}
			reader.close();
		}
		catch (IOException e)
		{
			throw new MalformedModTxtException(this);
		}*/
		
		
	}

	@Override
	public String getInternalName() {
		return internalName;
	}
}
