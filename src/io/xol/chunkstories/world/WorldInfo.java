package io.xol.chunkstories.world;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import io.xol.chunkstories.api.world.WorldGenerator;
import io.xol.chunkstories.net.packets.PacketWorldInfo;
import io.xol.chunkstories.server.net.ServerClient;
import io.xol.chunkstories.world.World.WorldSize;
import io.xol.chunkstories.world.generator.BlankWorldAccessor;
import io.xol.chunkstories.world.generator.FlatGenerator;
import io.xol.chunkstories.world.generator.HorizonGenerator;
import io.xol.chunkstories.world.generator.PerlinWorldAccessor;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class WorldInfo
{
	File folder;

	public String internalName = "";
	public String name;
	public String seed;
	public String description = "";
	public WorldSize size;
	public String generator;

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

			FileReader fileReader = new FileReader(file);
			BufferedReader reader = new BufferedReader(fileReader);
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
				name = parameterValue;
				break;
			case "seed":
				seed = parameterValue;
				break;
			case "worldgen":
				generator = parameterValue;
				break;
			case "size":
				size = WorldSize.getWorldSize(parameterValue);
				break;
			case "description":
				description = parameterValue;
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
		return new String[] { "name: " + name, "seed: " + seed, "worldgen: " + generator, "size: " + size.name() };
	}

	public WorldGenerator getGenerator()
	{
		WorldGenerator accessor = null;
		if (generator.equals("blank"))
			accessor = new BlankWorldAccessor();
		else if (generator.equals("perlin"))
			accessor = new PerlinWorldAccessor();
		else if (generator.equals("horizon"))
			accessor = new HorizonGenerator();
		else if (generator.equals("flat"))
			accessor = new FlatGenerator();
		return accessor;
	}

	public void sendInfo(ServerClient sender)
	{
		PacketWorldInfo packet = new PacketWorldInfo(false);
		packet.info = this;
		sender.sendPacket(packet);
	}
}
