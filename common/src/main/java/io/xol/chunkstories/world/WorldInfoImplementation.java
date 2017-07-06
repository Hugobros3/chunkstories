package io.xol.chunkstories.world;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Random;
import java.util.Map;
import java.util.Map.Entry;

import io.xol.chunkstories.api.world.WorldInfo;
import io.xol.chunkstories.materials.GenericConfigurable;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class WorldInfoImplementation extends GenericConfigurable implements WorldInfo
{
	private final String internalName;
	
	private String name;
	private String seed;
	private String description = "";
	private WorldInfo.WorldSize size;
	private String generatorName;

	//To guess a new seed in case we couldn't
	static Random random = new Random();
	
	/** Either you create one of those from scratch */
	public WorldInfoImplementation(String internalName, String name, String seed, String description, WorldInfo.WorldSize size, String generator)
	{
		super();
		
		this.internalName = internalName;
		if(this.internalName == null)
			throw new UnsupportedOperationException("Internal name can't be null");
		
		this.setProperty("internalName", internalName);
		
		this.setName(name);
		this.setSeed(seed);
		this.setDescription(description);
		this.setSize(size);
		this.setGeneratorName(generator);
	}
	
	/** Or you load'em from a stream of bytes */
	public WorldInfoImplementation(BufferedReader reader) throws IOException {
		super(reader);
		
		this.internalName = this.resolveProperty("internalName", null);
		if(this.internalName == null)
			throw new UnsupportedOperationException("Internal name can't be null");
		
		this.setName(this.resolveProperty("name", internalName));
		this.setSeed(this.resolveProperty("seed", ""+random.nextLong()));
		this.setDescription(this.resolveProperty("description", "A world with no description"));
		
		String sizeName = this.resolveProperty("size", null);
		if(sizeName == null)
			throw new UnsupportedOperationException("This world doesn't specify a file size");
		
		WorldSize size = WorldSize.getWorldSize(sizeName);
		if(size == null)
			throw new UnsupportedOperationException("This world specifies an unrecognised file size: "+sizeName);
		
		this.setSize(size);
		this.setGeneratorName(this.resolveProperty("worldGenerator", this.resolveProperty("worldgen", "blank")));
		
	}
	
	public void save(File file)
	{
		try
		{
			if(!file.getParentFile().exists())
				file.getParentFile().mkdirs();
			
			FileOutputStream output = new FileOutputStream(file);
			
			saveInStream(output);
			
			output.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void saveInStream(OutputStream output) throws IOException {
		Writer out = new BufferedWriter(new OutputStreamWriter(output, "UTF-8"));
		
		for(Entry<String, String> k : properties.entrySet()) {
			out.write(k.getKey()+": "+k.getValue().replace("\n", "\\n") + "\n");
		}
		
		out.flush();
	}
	
	@Override
	public String getInternalName()
	{
		return internalName;
	}

	@Override
	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
		this.setProperty("name", name);
		//this.internalName = name.replaceAll("[^\\w\\s]","_");
	}

	@Override
	public String getSeed()
	{
		return seed;
	}

	public void setSeed(String seed)
	{
		this.seed = seed;
		this.setProperty("seed", seed);
	}

	@Override
	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
		this.setProperty("description", description);
	}

	@Override
	public WorldInfo.WorldSize getSize()
	{
		return size;
	}

	public void setSize(WorldInfo.WorldSize size)
	{
		this.size = size;
		this.setProperty("size", this.size.name());
	}

	@Override
	public String getGeneratorName()
	{
		return generatorName;
	}

	public void setGeneratorName(String generatorName)
	{
		this.generatorName = generatorName;
		this.setProperty("worldGenerator", generatorName);
	}

	protected Map<String, String> getProperties() {
		return properties;
	}
}
