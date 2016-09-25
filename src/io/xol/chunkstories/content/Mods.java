package io.xol.chunkstories.content;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.xol.chunkstories.content.mods.Mod;
import io.xol.chunkstories.content.mods.ModFolder;
import io.xol.chunkstories.content.mods.ModZip;
import io.xol.chunkstories.content.mods.exceptions.ModLoadFailureException;
import io.xol.chunkstories.content.mods.exceptions.ModNotFoundException;
import io.xol.chunkstories.content.mods.exceptions.NotAllModsLoadedException;
import io.xol.engine.concurrency.UniqueList;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class Mods
{
	private static UniqueList<Mod> enabledMods = new UniqueList<Mod>();
	
	public static void main(String a[])
	{
		try
		{
			setEnabledMods("dogez_content", "modInZip", "OveriddenModInZip", "md5:df9f7c813fdc72029b41758ef8dbb528", "md5:7f46165474d11ee5836777d85df2cdab:http://xol.io");
		}
		catch (NotAllModsLoadedException e)
		{
			System.out.print(e.getMessage());
		}
		System.out.println("Done");
	}
	
	public static void setEnabledMods(String... modsEnabled) throws NotAllModsLoadedException
	{
		enabledMods.clear();
		List<ModLoadFailureException> modLoadExceptions = new ArrayList<ModLoadFailureException>();
		
		//Creates mods dir if it needs to
		File modsDir = new File(GameDirectory.getGameFolderPath() + "/mods");
		if (!modsDir.exists())
			modsDir.mkdirs();
		
		for (String name : modsEnabled)
		{
			try
			{
				Mod mod = null;
				
				//Servers give a md5 hash for their required mods
				if (name.startsWith("md5:"))
				{
					//Look for a mod with that md5 hash
					String hash = name.substring(4, name.length());
					String url = null;
					//If the hash is bundled with an url, split'em
					if(hash.contains(":"))
					{
						int i = hash.indexOf(":");
						url = hash.substring(i + 1);
						hash = hash.substring(0, i);
					}
					System.out.println("Looking for hashed mod "+hash +" (url = "+url+")");
					
					//Look for the mod zip in local fs first.
					File zippedMod = new File(modsDir.getAbsolutePath() + "/" + hash + ".zip");
					if(zippedMod.exists())
					{
						//Awesome we found it !
						mod = new ModZip(zippedMod);
					}
					else if(url != null)
					{
						//TODO download and hanle files from server
					}
					else
					{
						//We failed. Mod won't be loaded
					}
				}
				else
				{
					System.out.println("Looking for mod "+name+" on the local filesystem");
					
					//First look for it in the directory section
					File modDirectory = new File(modsDir.getAbsolutePath() + "/" + name);
					if(modDirectory.exists())
					{
						mod = new ModFolder(modDirectory);
						System.out.println("Found mod in directory : "+modDirectory);
					}
					else
					{
						//Then look for a .zip file in the same directory
						File zippedMod = new File(modsDir.getAbsolutePath() + "/" + name + ".zip");
						if(zippedMod.exists())
						{
							mod = new ModZip(zippedMod);
							System.out.println("Found mod in zipfile : "+zippedMod);
						}
						else
						{
							//Finally just look for it in the global os path
							if(name.endsWith(".zip"))
							{
								zippedMod = new File(name);
								if(zippedMod.exists())
								{
									mod = new ModZip(zippedMod);
									System.out.println("Found mod in global zipfile : "+zippedMod);
								}
							}
							else
							{
								modDirectory = new File(name);
								if(modDirectory.exists())
								{
									mod = new ModFolder(modDirectory);
									System.out.println("Found mod in global directory : "+modDirectory);
								}
							}
						}
					}
				}
				
				//Did we manage it ?
				if(mod != null)
				{
					if(!enabledMods.add(mod))
					{
						//Somehow we added a mod twice and it's now conflicting.
						throw new ModLoadFailureException(mod, "Conflicting mod, another mod with the same name or hash is already loaded.");
					}
				}
				else
					throw new ModNotFoundException(name);
			}
			catch (ModLoadFailureException exception)
			{
				modLoadExceptions.add(exception);
			}
		}
		
		buildModsFileSystem();
		
		//Return an exception if some mods failed to load.
		if(modLoadExceptions.size() > 0)
			throw new NotAllModsLoadedException(modLoadExceptions);
	}

	private static void buildModsFileSystem()
	{

	}

	public static void reload()
	{

	}
}
