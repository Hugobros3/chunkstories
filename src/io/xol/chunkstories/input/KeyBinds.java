package io.xol.chunkstories.input;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.input.KeyBind;
import io.xol.chunkstories.content.GameData;
import io.xol.chunkstories.tools.ChunkStoriesLogger;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class KeyBinds
{
	ClientInterface client;
	
	public KeyBinds(ClientInterface client)
	{
		this.client = client;
	}
	
	static Set<KeyBind> keyBinds = new HashSet<KeyBind>();
	
	public static Set<KeyBind> getKeyBinds()
	{
		return keyBinds;
	}

	/**
	 * Returns null or a KeyBind matching the name
	 * @param keyCode
	 * @return
	 */
	public static KeyBind getKeyBind(String bindName)
	{
		for(KeyBind keyBind : keyBinds)
		{
			if(keyBind.getName().equals(bindName))
				return keyBind;
		}
		return null;
	}
	
	/**
	 * Returns null or a KeyBind matching the pressed key
	 * @param keyCode
	 * @return
	 */
	public static KeyBind getKeyBindForLWJGL2xKey(int keyCode)
	{
		for(KeyBind keyBind : keyBinds)
		{
			if(keyBind instanceof KeyBindImplementation && ((KeyBindImplementation)keyBind).getLWJGL2xKey() == keyCode)
				return keyBind;
		}
		return null;
	}
	
	public static void loadKeyBinds()
	{
		keyBinds.clear();
		Deque<File> keyBindsFiles = GameData.getAllFileInstances("./res/data/keyBinds.txt");
		for (File f : keyBindsFiles)
		{
			loadKeyBindsFile(f);
		}
	}

	private static void loadKeyBindsFile(File f)
	{
		if (!f.exists())
			return;
		try (FileReader fileReader = new FileReader(f); BufferedReader reader = new BufferedReader(fileReader);)
		{
			String line = "";
			while ((line = reader.readLine()) != null)
			{
				if (line.startsWith("#"))
				{
					// It's a comment, ignore.
				}
				else
				{
					String splitted[] = line.split(" ");
					if(splitted.length >= 2)
					{
						KeyBind keyBind = new KeyBindImplementation(splitted[0], splitted[1]);
						keyBinds.add(keyBind);
					}
					System.out.println("added"+splitted[0]);
				}
			}
			//reader.close();
		}
		catch (IOException e)
		{
			ChunkStoriesLogger.getInstance().warning(e.getMessage());
		}
	}
	
	public static void reloadKeysFromConfig()
	{
		for(KeyBind keyBind : keyBinds)
		{
			if(keyBind instanceof KeyBindImplementation)
				((KeyBindImplementation)keyBind).reload();
		}
	}
}
