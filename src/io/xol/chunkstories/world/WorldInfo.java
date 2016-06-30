package io.xol.chunkstories.world;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

import io.xol.chunkstories.api.world.WorldGenerator;
import io.xol.chunkstories.net.packets.PacketWorldInfo;
import io.xol.chunkstories.server.net.ServerClient;
import io.xol.chunkstories.world.generator.WorldGenerators;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class WorldInfo
{
	File folder;

	private String internalName = "";
	private String name;
	private String seed;
	private String description = "";
	private WorldSize size;
	private String generatorName;

	WorldInfo()
	{
		
	}
	
	public WorldInfo(String name, String seed, String description, WorldSize size, String generator)
	{
		super();
		this.setName(name);
		this.setSeed(seed);
		this.setDescription(description);
		this.setSize(size);
		this.setGeneratorName(generator);
	}


	public WorldInfo(String fileContents, String internalName)
	{
		this.internalName = internalName;
		for (String line : fileContents.split("\n"))
			readLine(line);
	}

	public WorldInfo(File file, String internalName)
	{
		try
		{
			this.internalName = internalName;

			//FileReader fileReader = new FileReader(file);
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
			String line = "";
			while ((line = reader.readLine()) != null)
			{
				readLine(line);
			}
			reader.close();
			folder = file.getParentFile();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	void readLine(String line)
	{
		if (!line.startsWith("#"))
		{
			if(!line.contains(":"))
				return;
			String[] splitted = line.split(": ");
			String parameterName = splitted[0];
			String parameterValue = splitted[1];
			switch (parameterName)
			{
			case "name":
				setName(parameterValue);
				break;
			case "seed":
				setSeed(parameterValue);
				break;
			case "worldgen":
				setGeneratorName(parameterValue);
				break;
			case "size":
				setSize(WorldSize.getWorldSize(parameterValue));
				break;
			case "description":
				setDescription(parameterValue);
				break;
			default:
				break;
			}
		}
	}

	public void save(File file)
	{
		try
		{
			if(!file.getParentFile().exists())
				file.getParentFile().mkdirs();
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
			for (String line : saveText())
				out.write(line + "\n");
			out.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public String[] saveText()
	{
		return new String[] { "name: " + getName(), "seed: " + getSeed(), "worldgen: " + getGeneratorName(), "size: " + getSize().name() };
	}

	public WorldGenerator getGenerator()
	{
		WorldGenerator generator = WorldGenerators.getWorldGenerator(this.getGeneratorName());
		return generator;
	}

	/**
	 * Will send a packet containing this object information to the user
	 * @param sender
	 */
	public void sendInfo(ServerClient user)
	{
		PacketWorldInfo packet = new PacketWorldInfo(false);
		packet.info = this;
		user.pushPacket(packet);
	}
	
	public String getInternalName()
	{
		return internalName;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
		//this.internalName = name.replaceAll("[^\\w\\s]","_");
	}

	public String getSeed()
	{
		return seed;
	}

	public void setSeed(String seed)
	{
		this.seed = seed;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public WorldSize getSize()
	{
		return size;
	}

	public void setSize(WorldSize size)
	{
		this.size = size;
	}

	public String getGeneratorName()
	{
		return generatorName;
	}

	public void setGeneratorName(String generatorName)
	{
		this.generatorName = generatorName;
	}

	public enum WorldSize
	{
		TINY(32, "1x1km"), SMALL(64, "2x2km"), MEDIUM(128, "4x4km"), LARGE(512, "16x16km"), HUGE(2048, "64x64km");

		// Warning : this can be VERY ressource intensive as it will make a
		// 4294km2 map,
		// leading to enormous map sizes ( in the order of 10Gbs to 100Gbs )
		// when fully explored.

		WorldSize(int s, String n)
		{
			sizeInChunks = s;
			name = n;
		}

		public int sizeInChunks;
		public int heightInChunks = 32;
		public String name;

		public static String getAllSizes()
		{
			String sizes = "";
			for (WorldSize s : WorldSize.values())
			{
				sizes = sizes + s.name() + ", " + s.name + " ";
			}
			return sizes;
		}

		public static WorldSize getWorldSize(String name)
		{
			name.toUpperCase();
			for (WorldSize s : WorldSize.values())
			{
				if (s.name().equals(name))
					return s;
			}
			return null;
		}
	}
}
