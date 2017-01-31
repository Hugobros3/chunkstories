package io.xol.chunkstories.input;

import java.io.BufferedReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.InputsManager;
import io.xol.chunkstories.api.mods.Asset;
import io.xol.chunkstories.api.mods.ModsManager;
import io.xol.chunkstories.input.lwjgl2.KeyBindImplementation;
import io.xol.chunkstories.input.lwjgl2.Lwjgl2ClientInputsManager;
import io.xol.chunkstories.server.ServerInputsManager;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class KeyBindsLoader
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
	
	public static Iterator<Input> loadKeyBindsIntoManager(InputsManager inputManager, ModsManager modsManager)
	{
		return new Iterator<Input>()
		{
			Iterator<Asset> i = modsManager.getAllAssetsByExtension("inputs");
			Iterator<Input> fileInputsIterator = null;
			Input input = null;

			@Override
			public boolean hasNext()
			{
				if (input != null)
					return true;

				//If there is something to load
				if (fileInputsIterator != null && fileInputsIterator.hasNext())
				{
					input = fileInputsIterator.next();
					return true;
				}

				//Load the next one
				while (i.hasNext())
				{
					fileInputsIterator = loadKeyBindsFile(i.next(), inputManager);

					//If we're done reading the file load another
					if (fileInputsIterator != null && fileInputsIterator.hasNext())
					{
						input = fileInputsIterator.next();
						return true;
					}
				}

				return false;
			}

			@Override
			public Input next()
			{
				if (input == null)
					hasNext();

				Input z = input;
				input = null;
				return z;
			}

		};
	}

	private static Iterator<Input> loadKeyBindsFile(Asset asset, InputsManager inputManager)
	{
		if (asset == null)
			return null;

		return new Iterator<Input>()
		{

			BufferedReader reader = new BufferedReader(asset.reader());

			Input input = null;

			@Override
			public boolean hasNext()
			{
				if (input != null)
					return true;

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
							if (splitted.length >= 3)
							{
								/*
								 * There goes the fun
								 */
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
							}
						}
					}
					reader.close();
					return false;
				}
				catch (IOException e)
				{
					e.printStackTrace();
					System.out.println(asset);
				}

				return false;
			}

			@Override
			public Input next()
			{
				if (input == null)
					hasNext();

				Input z = input;
				input = null;
				return z;
			}

		};
	}
}
