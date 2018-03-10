//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.input;

import java.io.BufferedReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.api.content.mods.ModsManager;

/** Eases internal classes from the burden of dealing with the file formats */
public class InputsLoaderHelper
{
	public static MessageDigest md;

	static
	{
		try
		{
			md = MessageDigest.getInstance("MD5");
		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
	}
	
	public static void loadKeyBindsIntoManager(InputsManagerLoader inputManager, ModsManager modsManager)
	{
		Iterator<Asset> i = modsManager.getAllAssetsByExtension("inputs");
		//Load the next one
		while (i.hasNext())
		{
			loadKeyBindsFile(i.next(), inputManager);
		}
	}

	private static void loadKeyBindsFile(Asset asset, InputsManagerLoader inputManager)
	{
		if (asset == null)
			return;
		
		BufferedReader reader = new BufferedReader(asset.reader());
	
		List<String> arguments = new ArrayList<String>();
		String inputName;
		String inputValue;
		String inputType;
		
		//Read until we get a good one
		String line = "";
		try
		{
			while ((line = reader.readLine()) != null)
			{
				//System.out.println("Reading " + line);
				if (line.startsWith("#"))
				{
					// It's a comment, ignore.
				}
				else
				{
					String splitted[] = line.split(" ");
					if(splitted.length >= 2)
					{
						arguments.clear();
						inputValue = null;
						
						inputType = splitted[0];
						inputName = splitted[1];
						if (splitted.length >= 3)
						{
							inputValue = splitted[2];
							for(int i = 3; i < splitted.length; i++)
								arguments.add(splitted[i]);
						}
						
						inputManager.insertInput(inputType, inputName, inputValue, arguments);
					}
					
					/*if (splitted.length >= 3)
					{
						if (inputManager instanceof Lwjgl2ClientInputsManager)
						{
							if (splitted[0].equals("keyBind"))
							{
								input = new KeyBindImplementation(splitted[1], splitted[2]);
								for(int i = 3; i < splitted.length; i++)
								{
									if(splitted[i].equals("hidden"))
										((KeyBindImplementation) input).setEditable(false);
								}
								return true;
							}
						}
						else if(inputManager instanceof ServerInputsManager)
						{
							input = new InputVirtual(splitted[1]);
							return true;
						}
					}
					else if(splitted.length >= 2)
					{
						if (splitted[0].equals("virtual"))
						{
							input = new InputVirtual(splitted[1]);
							return true;
						}
					}*/
				}
			}
			reader.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			System.out.println(asset);
		}
	}
}
