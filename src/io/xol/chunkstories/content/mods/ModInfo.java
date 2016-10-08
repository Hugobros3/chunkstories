package io.xol.chunkstories.content.mods;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import io.xol.chunkstories.content.mods.exceptions.MalformedModTxtException;

public class ModInfo
{
	private String name;
	private String version = "undefined";
	private String description = "No description given";
	
	public String getName()
	{
		return name;
	}
	
	public String getVersion()
	{
		return version;
	}
	
	public ModInfo(InputStream inputStream) throws MalformedModTxtException
	{
		if(inputStream == null)
			throw new MalformedModTxtException(this);
		
		//Try to read the ressource
		try
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
		}
		
		//Requires a name to be set, at least
		//TODO change it, require more
		if(this.name == null)
			throw new MalformedModTxtException(this);
		
	}

	public String getDescription()
	{
		return description;
	}
}
