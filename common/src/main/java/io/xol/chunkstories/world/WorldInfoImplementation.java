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

import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.world.WorldInfo;
import io.xol.chunkstories.net.packets.PacketSendWorldInfo;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class WorldInfoImplementation implements WorldInfo
{
	private String internalName = "";
	private String name;
	private String seed;
	private String description = "";
	private WorldInfo.WorldSize size;
	private String generatorName;
	
	public WorldInfoImplementation(String name, String seed, String description, WorldInfo.WorldSize size, String generator)
	{
		this.internalName = name;
		this.name = name;
		this.setSeed(seed);
		this.setDescription(description);
		this.setSize(size);
		this.setGeneratorName(generator);
	}

	public WorldInfoImplementation(String fileContents, String internalName)
	{
		this.internalName = internalName;
		for (String line : fileContents.split("\n"))
			readLine(line);
	}

	public WorldInfoImplementation(File file, String internalName)
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
			
			//folder = file.getParentFile();
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
				setSize(WorldInfo.WorldSize.getWorldSize(parameterValue));
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

	/**
	 * Will send a packet containing this object information to the user
	 * @param sender
	 */
	public void sendInfo(PacketDestinator user)
	{
		PacketSendWorldInfo packet = new PacketSendWorldInfo();
		packet.info = this;
		user.pushPacket(packet);
	}
	
	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInfo#getInternalName()
	 */
	@Override
	public String getInternalName()
	{
		return internalName;
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInfo#getName()
	 */
	@Override
	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
		//this.internalName = name.replaceAll("[^\\w\\s]","_");
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInfo#getSeed()
	 */
	@Override
	public String getSeed()
	{
		return seed;
	}

	public void setSeed(String seed)
	{
		this.seed = seed;
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInfo#getDescription()
	 */
	@Override
	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInfo#getSize()
	 */
	@Override
	public WorldInfo.WorldSize getSize()
	{
		return size;
	}

	public void setSize(WorldInfo.WorldSize size)
	{
		this.size = size;
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInfo#getGeneratorName()
	 */
	@Override
	public String getGeneratorName()
	{
		return generatorName;
	}

	public void setGeneratorName(String generatorName)
	{
		this.generatorName = generatorName;
	}
}
