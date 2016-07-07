package io.xol.chunkstories.input;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.InputsManager;
import io.xol.chunkstories.client.ClientInputsManager;
import io.xol.chunkstories.content.GameData;
import io.xol.chunkstories.server.ServerInputsManager;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class Inputs
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
	
	public static Iterator<Input> loadKeyBindsIntoManager(InputsManager inputManager)
	{
		return new Iterator<Input>()
		{

			Iterator<File> i = GameData.getAllFilesByExtension("inputs");
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

	private static Iterator<Input> loadKeyBindsFile(File f, InputsManager inputManager)
	{
		if (!f.exists())
			return null;

		System.out.println("Reading " + f);

		try
		{
			return new Iterator<Input>()
			{

				FileReader fileReader = new FileReader(f);
				BufferedReader reader = new BufferedReader(fileReader);

				/*String line = "";
				while ((line = reader.readLine()) != null)
				{
					if (line.startsWith("#"))
					{
						// It's a comment, ignore.
					}
					else
					{
						String splitted[] = line.split(" ");
						if (splitted.length >= 2)
						{
							KeyBindImplementation keyBind = new KeyBindImplementation(splitted[0], splitted[1]);
				
							//inputs.add(keyBind);
							//inputsMap.put(keyBind.getHash(), keyBind);
						}
						//System.out.println("added" + splitted[0]);
					}
				}*/

				Input input = null;

				//reader.close();
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
									if (inputManager instanceof ClientInputsManager)
									{
										if (splitted[0].equals("keyBind"))
										{
											input = new KeyBindImplementation(splitted[1], splitted[2]);
											return true;
										}
									}
									else if(inputManager instanceof ServerInputsManager)
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
						System.out.println(f);
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
		catch (FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	/*public static void main(String[] a)
	{
		try
		{
			MessageDigest md = MessageDigest.getInstance("MD5");
			String msg = "bind.caca";
			byte[] digested = md.digest(msg.getBytes());
			System.out.println(digested.length);
	
			long digestedLong = 0L;
			digestedLong = (digestedLong & 0x0FFFFFFFFFFFFFFFL) | (((long) digested[0] & 0xF) << 60);
			digestedLong = (digestedLong & 0xF0FFFFFFFFFFFFFFL) | (((long) digested[1] & 0xF) << 56);
			digestedLong = (digestedLong & 0xFF0FFFFFFFFFFFFFL) | (((long) digested[2] & 0xF) << 52);
			digestedLong = (digestedLong & 0xFFF0FFFFFFFFFFFFL) | (((long) digested[3] & 0xF) << 48);
			digestedLong = (digestedLong & 0xFFFF0FFFFFFFFFFFL) | (((long) digested[4] & 0xF) << 44);
			digestedLong = (digestedLong & 0xFFFFF0FFFFFFFFFFL) | (((long) digested[5] & 0xF) << 40);
			digestedLong = (digestedLong & 0xFFFFFF0FFFFFFFFFL) | (((long) digested[6] & 0xF) << 36);
			digestedLong = (digestedLong & 0xFFFFFFF0FFFFFFFFL) | (((long) digested[7] & 0xF) << 32);
			digestedLong = (digestedLong & 0xFFFFFFFF0FFFFFFFL) | (((long) digested[8] & 0xF) << 28);
			digestedLong = (digestedLong & 0xFFFFFFFFF0FFFFFFL) | (((long) digested[9] & 0xF) << 24);
			digestedLong = (digestedLong & 0xFFFFFFFFFF0FFFFFL) | (((long) digested[10] & 0xF) << 20);
			digestedLong = (digestedLong & 0xFFFFFFFFFFF0FFFFL) | (((long) digested[11] & 0xF) << 16);
			digestedLong = (digestedLong & 0xFFFFFFFFFFFF0FFFL) | (((long) digested[12] & 0xF) << 12);
			digestedLong = (digestedLong & 0xFFFFFFFFFFFFF0FFL) | (((long) digested[13] & 0xF) << 8);
			digestedLong = (digestedLong & 0xFFFFFFFFFFFFFF0FL) | (((long) digested[14] & 0xF) << 4);
			digestedLong = (digestedLong & 0xFFFFFFFFFFFFFFF0L) | (((long) digested[15] & 0xF) << 0);
	
			long digestedLong2 = 0L;
			digestedLong2 = (digestedLong2 & 0x0FFFFFFFFFFFFFFFL) | ((((long) digested[0] & 0xF0) >> 4) << 60);
			digestedLong2 = (digestedLong2 & 0xF0FFFFFFFFFFFFFFL) | ((((long) digested[1] & 0xF0) >> 4) << 56);
			digestedLong2 = (digestedLong2 & 0xFF0FFFFFFFFFFFFFL) | ((((long) digested[2] & 0xF0) >> 4) << 52);
			digestedLong2 = (digestedLong2 & 0xFFF0FFFFFFFFFFFFL) | ((((long) digested[3] & 0xF0) >> 4) << 48);
			digestedLong2 = (digestedLong2 & 0xFFFF0FFFFFFFFFFFL) | ((((long) digested[4] & 0xF0) >> 4) << 44);
			digestedLong2 = (digestedLong2 & 0xFFFFF0FFFFFFFFFFL) | ((((long) digested[5] & 0xF0) >> 4) << 40);
			digestedLong2 = (digestedLong2 & 0xFFFFFF0FFFFFFFFFL) | ((((long) digested[6] & 0xF0) >> 4) << 36);
			digestedLong2 = (digestedLong2 & 0xFFFFFFF0FFFFFFFFL) | ((((long) digested[7] & 0xF0) >> 4) << 32);
			digestedLong2 = (digestedLong2 & 0xFFFFFFFF0FFFFFFFL) | ((((long) digested[8] & 0xF0) >> 4) << 28);
			digestedLong2 = (digestedLong2 & 0xFFFFFFFFF0FFFFFFL) | ((((long) digested[9] & 0xF0) >> 4) << 24);
			digestedLong2 = (digestedLong2 & 0xFFFFFFFFFF0FFFFFL) | ((((long) digested[10] & 0xF0) >> 4) << 20);
			digestedLong2 = (digestedLong2 & 0xFFFFFFFFFFF0FFFFL) | ((((long) digested[11] & 0xF0) >> 4) << 16);
			digestedLong2 = (digestedLong2 & 0xFFFFFFFFFFFF0FFFL) | ((((long) digested[12] & 0xF0) >> 4) << 12);
			digestedLong2 = (digestedLong2 & 0xFFFFFFFFFFFFF0FFL) | ((((long) digested[13] & 0xF0) >> 4) << 8);
			digestedLong2 = (digestedLong2 & 0xFFFFFFFFFFFFFF0FL) | ((((long) digested[14] & 0xF0) >> 4) << 4);
			digestedLong2 = (digestedLong2 & 0xFFFFFFFFFFFFFFF0L) | ((((long) digested[15] & 0xF0) >> 4) << 0);
			System.out.println("Long" + digestedLong);
			System.out.println("Long2" + digestedLong2);
		}
		catch (NoSuchAlgorithmException e)
		{
			
		}
	}*/
}
