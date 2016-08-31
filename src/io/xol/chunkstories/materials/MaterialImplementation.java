package io.xol.chunkstories.materials;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import io.xol.chunkstories.api.material.Material;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class MaterialImplementation implements Material
{
	private static Random random = new Random();
	
	private String name;
	private Map<String, String> properties = new HashMap<String, String>();
	
	public MaterialImplementation(String name)
	{
		this.name = name;
		
		this.setProperty("sounds", "sounds/materials/<matname>/");
		this.setProperty("walkingSounds", "sounds/footsteps/generic[1-3].ogg");
		this.setProperty("runningSounds", "<walkingSounds>");
		this.setProperty("jumpingSounds", "sounds/footsteps/jump.ogg");
		this.setProperty("landingSounds", "sounds/footsteps/land.ogg");
	}
	
	@Override
	public String getName()
	{
		return this.name;
	}
	
	public void setProperty(String propertyName, String propertyValue)
	{
		//if(properties.containsKey(propertyName))
		//	properties.replace(propertyName, propertyValue);
		//else
		properties.put(propertyName, propertyValue);
	}
	
	@Override
	public String resolveProperty(String propertyName)
	{
		String resolved = properties.get(propertyName);
		if(resolved == null)
			return null;
	
		//Replace material name
		resolved = resolved.replace("<matname>", name);
		
		//Resolves inclusions
		while(resolved.indexOf("<") != -1)
		{
			String propertyToInclude = resolved.substring(resolved.indexOf("<") + 1, resolved.indexOf(">"));
			
			//Prevents resolving itself
			String propertyResolved = propertyToInclude.equals(propertyName) ? "" : resolveProperty(propertyToInclude);
			
			//Removes resolved name from string
			resolved = resolved.substring(0, resolved.indexOf("<")) + propertyResolved + resolved.substring(resolved.indexOf(">") + 1, resolved.length());
		}
		
		//Resolves random numbers
		while(resolved.indexOf("[") != -1)
		{
			String range = resolved.substring(resolved.indexOf("[") + 1, resolved.indexOf("]"));
			
			String resolvedRandom;
			
			//Resolves doubles
			if(range.contains("."))
			{
				double minBound = Double.parseDouble(range.split("-")[0]);
				double maxBound = Double.parseDouble(range.split("-")[1]);
				
				resolvedRandom = "" + (random.nextDouble() * (maxBound - minBound) + minBound);
			}
			//Resolve integers
			else
			{
				int minBound = Integer.parseInt(range.split("-")[0]);
				int maxBound = Integer.parseInt(range.split("-")[1]);
				
				resolvedRandom = "" + (random.nextInt(maxBound - minBound + 1) + minBound);
			}
			
			//Removes resolved name from string
			resolved = resolved.substring(0, resolved.indexOf("[")) + resolvedRandom + resolved.substring(resolved.indexOf("]") + 1, resolved.length());
		}
		
		return resolved;
	}
	
	public static void main(String a[])
	{
		MaterialImplementation nextGen = new MaterialImplementation("nextGen");
		nextGen.setProperty("memes", "4chan.org [0.0-2016.0]");
		nextGen.setProperty("cuck", "<matname>/Alexix200 giving [1-5] keks looking at <memes>");
		
		System.out.println(nextGen.resolveProperty("cuck"));
	}
}
