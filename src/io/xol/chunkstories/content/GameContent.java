package io.xol.chunkstories.content;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.xol.chunkstories.entity.Entities;
import io.xol.chunkstories.entity.EntityComponents;
import io.xol.chunkstories.item.ItemTypes;
import io.xol.chunkstories.materials.Materials;
import io.xol.chunkstories.net.packets.PacketsProcessor;
import io.xol.chunkstories.particles.ParticleTypes;
import io.xol.chunkstories.voxel.VoxelTextures;
import io.xol.chunkstories.voxel.Voxels;
import io.xol.chunkstories.voxel.models.VoxelModels;
import io.xol.chunkstories.world.generator.WorldGenerators;
import io.xol.engine.animation.BVHLibrary;
import io.xol.engine.graphics.shaders.ShadersLibrary;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.model.ModelLibrary;
import io.xol.engine.sound.library.SoundsLibrary;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class GameContent
{
	public static void reload()
	{
		long total = System.nanoTime();
		long part = System.nanoTime();
		
		buildModsFileSystem();
		System.out.println("fs reload took "+Math.floor(((System.nanoTime() - part) / 1000L) / 100f) / 10f + "ms ");
		part = System.nanoTime();
		
		VoxelTextures.buildTextureAtlas();
		System.out.println("texture atlas reload took "+Math.floor(((System.nanoTime() - part) / 1000L) / 100f) / 10f + "ms ");
		part = System.nanoTime();
		
		Materials.reload();
		System.out.println("materials reload took "+Math.floor(((System.nanoTime() - part) / 1000L) / 100f) / 10f + "ms ");
		part = System.nanoTime();
		
		VoxelModels.resetAndLoadModels();
		System.out.println("voxel models reload took "+Math.floor(((System.nanoTime() - part) / 1000L) / 100f) / 10f + "ms ");
		part = System.nanoTime();
		
		ItemTypes.reload();
		System.out.println("items reload took "+Math.floor(((System.nanoTime() - part) / 1000L) / 100f) / 10f + "ms ");
		part = System.nanoTime();
		
		Voxels.loadVoxelTypes();
		System.out.println("voxels reload took "+Math.floor(((System.nanoTime() - part) / 1000L) / 100f) / 10f + "ms ");
		part = System.nanoTime();
		
		Entities.reload();
		System.out.println("entities reload took "+Math.floor(((System.nanoTime() - part) / 1000L) / 100f) / 10f + "ms ");
		part = System.nanoTime();
		
		EntityComponents.reload();
		System.out.println("components reload took "+Math.floor(((System.nanoTime() - part) / 1000L) / 100f) / 10f + "ms ");
		part = System.nanoTime();
		
		PacketsProcessor.loadPacketsTypes();
		System.out.println("packets reload took "+Math.floor(((System.nanoTime() - part) / 1000L) / 100f) / 10f + "ms ");
		part = System.nanoTime();
		
		WorldGenerators.loadWorldGenerators();
		System.out.println("generators reload took "+Math.floor(((System.nanoTime() - part) / 1000L) / 100f) / 10f + "ms ");
		part = System.nanoTime();
		
		ParticleTypes.reload();
		System.out.println("particles reload took "+Math.floor(((System.nanoTime() - part) / 1000L) / 100f) / 10f + "ms ");
		part = System.nanoTime();
		
		//Total
		System.out.println("Assets reload took "+Math.floor(((System.nanoTime() - part) / 1000L) / 100f) / 10f + "ms ");
		
		//Inputs.loadKeyBindsClient();
	}

	public static void reloadClientContent()
	{
		TexturesHandler.reloadAll();
		SoundsLibrary.clean();
		ModelLibrary.reloadAllModels();
		BVHLibrary.reloadAllAnimations();
		ShadersLibrary.reloadAllShaders();
	}
	
	static ConcurrentHashMap<String, Deque<File>> fileSystem = new ConcurrentHashMap<String, Deque<File>>();

	static Set<String> mods = new HashSet<String>();
	static boolean allModsEnabled = false;
	
	public static void setEnabledMods(String... modsEnabled)
	{
		//Build a set of required mods
		mods.clear();
		allModsEnabled = false;
		for (String s : modsEnabled)
		{
			System.out.println("MODS"+s);
			if(s.equals("*"))
				allModsEnabled = true;
			else
				mods.add(s);
		}
		//System.out.println("MODS"+allModsEnabled);
	}
	
	/**
	 * Creates and fill the fileSystem hashmap of deque of files
	 * @param modsEnabled
	 */
	private static void buildModsFileSystem()
	{
		fileSystem.clear();
		
		//Get the mods/ dir
		File modsDir = new File(GameDirectory.getGameFolderPath() + "/mods/");
		if (!modsDir.exists())
			modsDir.mkdirs();
		
		//Enable external mods
		//Set<String> alreadyEnabledMods = new HashSet<String>();
		for(String modName : mods)
		{
			modName = modName.replace('\\', '/');
			if(modName.contains("/"))
			{
				System.out.println("External mod:"+modsDir.getAbsolutePath()+""+modName);
				File f = new File(modName);
				if (f.isDirectory())
					recursiveScan(f, f);
			}
			else
			{
				File f = new File(modsDir.getAbsolutePath()+""+modName);
				System.out.println("Local mod:"+modsDir.getAbsolutePath()+""+modName);
				if (f.isDirectory())
					recursiveScan(f, f);
			}
		}
		//Load needed mods by order of priority
		for (File f : modsDir.listFiles())
		{
			if (allModsEnabled)// && !alreadyEnabledMods.contains(f.getName()))
			{
				if (f.isDirectory())
					recursiveScan(f, f);
			}
		}
		//Load vanilla ressources (lowest priority)
		for(File f : new File(GameDirectory.getGameFolderPath() + "/res/").listFiles())
				recursiveScan(f, new File(GameDirectory.getGameFolderPath() + "/res/"));
	}

	private static void recursiveScan(File initialFile, File modRootDirectory)
	{
		//Special case for initial res/ folder
		if(!initialFile.isDirectory())
		{
			File f = initialFile;
			String filteredName = f.getAbsolutePath();
			//Remove game dir path
			filteredName = "."+filteredName.replace(modRootDirectory.getAbsolutePath(), "");
			filteredName = filteredName.replace('\\', '/');
			//Remove mod path
			if(!fileSystem.containsKey(filteredName))
			{
				fileSystem.remove(filteredName);
				fileSystem.put(filteredName, new ArrayDeque<File>());
				//System.out.println("Found override for ressource : " + filteredName + " in modDir : " + modsDir);
			}
			fileSystem.get(filteredName).addLast(f);
			//fileSystem.put(filteredName, f);
			return;
		}
		//We just list dem files
		for (File f : initialFile.listFiles())
		{
			if (f.isDirectory())
				recursiveScan(f, modRootDirectory);
			else
			{
				String filteredName = f.getAbsolutePath();
				//Remove game dir path
				filteredName = "."+filteredName.replace(modRootDirectory.getAbsolutePath(), "");
				filteredName = filteredName.replace('\\', '/');
				//Remove mod path
				if(!fileSystem.containsKey(filteredName))
				{
					fileSystem.remove(filteredName);
					fileSystem.put(filteredName, new ArrayDeque<File>());
					//System.out.println("Found override for ressource : " + filteredName + " in modDir : " + modsDir);
				}
				fileSystem.get(filteredName).addLast(f);
				//System.out.println("Found ressocurce "+filteredName);
				//fileSystem.put(filteredName, f);
			}
		}
	}

	public static Iterator<Entry<String, Deque<File>>> getAllUniqueEntries()
	{
		return fileSystem.entrySet().iterator();
	}
	
	public static Iterator<Deque<File>> getAllUniqueFilesLocations()
	{
		return fileSystem.values().iterator();
	}

	/**
	 * Gets the location of a certain texture file ( checks mods/ first and then various directories )
	 * @param textureName
	 * @return hopefully a valid file ( null if it doesn't seem to exist )
	 */
	public static File getTextureFileLocation(String textureName)
	{
		if(fileSystem.containsKey(textureName))
			return fileSystem.get(textureName).getFirst();
		
		if (textureName.endsWith(".png"))
			textureName = textureName.substring(0, textureName.length() - 4);
		File checkTexturesFolder = new File("./res/textures/" + textureName + ".png");
		if (checkTexturesFolder.exists())
			return checkTexturesFolder;
		File checkResFolder = new File("./res/" + textureName + ".png");
		if (checkResFolder.exists())
			return checkResFolder;
		File checkRootFolder = new File("./" + textureName + ".png");
		if (checkRootFolder.exists())
			return checkRootFolder;
		File checkGameFolder = new File(GameDirectory.getGameFolderPath() + "/" + textureName + ".png");
		if (checkGameFolder.exists())
			return checkGameFolder;
		return null;
	}

	/**
	 * Gets the most prioritary instance of the file this file system has (or null if it doesn't exist)
	 * @param fileName
	 * @return
	 */
	public static File getFileLocation(String fileName)
	{
		if(!fileName.startsWith("./"))
			fileName = "./"+fileName;
		
		if(fileSystem.containsKey(fileName))
			return fileSystem.get(fileName).getFirst();
		
		
		File checkRootFolder = new File("./" + fileName);
		if (checkRootFolder.exists())
			return checkRootFolder;
		File checkGameFolder = new File(GameDirectory.getGameFolderPath() + "/" + fileName);
		if (checkGameFolder.exists())
			return checkGameFolder;
		return null;
	}

	/**
	 * Returns all the instances of a file in the various mods, sorted by mod priority
	 * @return
	 */
	public static Deque<File> getAllFileInstances(String fileName)
	{
		if(fileSystem.containsKey(fileName))
			return fileSystem.get(fileName);
		return null;
	}

	public static Iterator<File> getAllFilesByExtension(String extension)
	{
		return new Iterator<File>() {

			Iterator<Entry<String, Deque<File>>> base = getAllUniqueEntries();
			
			File next = null;
			
			@Override
			public boolean hasNext()
			{
				if(next != null)
					return true;
				//If next == null, try to set it
				while(base.hasNext())
				{
					Entry<String, Deque<File>> entry = base.next();
					if(entry.getKey().endsWith(extension))
					{
						next = entry.getValue().getFirst();
						break;
					}
				}
				//Did we suceed etc
				return next != null;
			}

			@Override
			public File next()
			{
				//Try loading
				if(next == null)
					hasNext();
				//Null out reference and return it
				File ret = next;
				next = null;
				return ret;
			}
			
		};
	}

	public static Class<?> getClassByName(String className)
	{
		//First try to load it from classpath
		try
		{
			Class<?> inBaseClasspath = Class.forName(className);
			if(inBaseClasspath != null)
				return inBaseClasspath;
		}
		catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		}
		//If this fails, try to obtain it from one of the loaded mods
		
		//If all fail, return null
		return null;
	}
}
