//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.content;

import java.io.BufferedReader;
import java.io.IOException;

import io.xol.chunkstories.api.content.Definition;


public class GenericNamedConfigurable extends GenericConfigurable implements Definition
{
	protected final String name;
	
	public GenericNamedConfigurable(String name)
	{
		this.name = name;
		this.setProperty("name", name);
	}
	
	/** Alternative version that automatically reads what it needs to */
	public GenericNamedConfigurable(String name, BufferedReader reader) throws IOException
	{
		this.name = name;
		
		String line;
		while ((line = reader.readLine()) != null)
		{
			line = line.replace("\t", "");
			if (line.startsWith("#"))
			{
				// It's a comment, ignore.
			}
			else if (line.startsWith("end"))
			{
				break;
			}
			else if(line.contains(":"))
			{
				String[] s = line.split(": ");
				if(s.length == 1)
					this.setProperty(s[0], "");
				else
					this.setProperty(s[0], s[1]);
			}
		}
		
		this.setProperty("name", name);
	}
	
	public String getName()
	{
		return this.name;
	}
	
	public String resolveProperty(String propertyName)
	{
		propertyName = propertyName.toLowerCase();
		
		String resolved = super.resolveProperty(propertyName);
		if(resolved == null)
			return null;
	
		//Replace material name
		resolved = resolved.replace("<name>", name);
		
		//Alternative syntax
		resolved = resolved.replace("~", name);
		
		return resolved;
	}
	
	public static void main(String a[])
	{
		GenericNamedConfigurable nextGen = new GenericNamedConfigurable("nextGen");
		nextGen.setProperty("memes", "4chan.org [0.0-2016.0]");
		nextGen.setProperty("cuck", "<name>/Alexix200 giving [1-5] keks looking at <memes>");
		
		System.out.println(nextGen.resolveProperty("cuck"));
	}
}
