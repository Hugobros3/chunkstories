package io.xol.chunkstories.materials;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class GenericConfigurable {

	protected Map<String, String> properties = new HashMap<String, String>();
	
	public GenericConfigurable() {
		
	}
	
	public GenericConfigurable(BufferedReader reader) throws IOException {
		load(reader);
	}
	
	/*public GenericConfigurable(InputStream in) throws IOException {
		load(in);
	}*/
	
	protected void load(BufferedReader reader) throws IOException {
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
				if(s.length > 1)
					this.setProperty(s[0], s[1]);
			}
		}
	}
	
	public void setProperty(String propertyName, String propertyValue)
	{
		//if(properties.containsKey(propertyName))
		//	properties.replace(propertyName, propertyValue);
		//else
		properties.put(propertyName, propertyValue);
	}
	
	private static Random random = new Random();
	
	public String resolveProperty(String propertyName)
	{
		String resolved = properties.get(propertyName);
		if(resolved == null)
			return null;
		
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

	public String resolveProperty(String propertyName, String defaultValue)
	{
		String r = resolveProperty(propertyName);
		return r != null ? r : defaultValue;
	}
}
