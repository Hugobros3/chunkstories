package io.xol.chunkstories.content.mods;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import io.xol.chunkstories.api.mods.Mod;
import io.xol.chunkstories.tools.ChunkStoriesLoggerImplementation;

/**
 * Foreign content is anything found inside a jar and loaded by the game engine. Security measures applies unless configured otherwise
 */
public class ForeignCodeClassLoader extends URLClassLoader
{
	Mod responsibleMod;
	Map<String, Class<?>> classes = new HashMap<String, Class<?>>();

	public ForeignCodeClassLoader(Mod responsibleMod, ClassLoader parentLoader, Collection<File> files) throws IOException
	{
		super(urlHelper(files), parentLoader);
		
		assert parentLoader != null;

		this.responsibleMod = responsibleMod;

		for(File file : files) {
			JarFile jar = new JarFile(file);
	
			//Lists classes to be found in that jarFile
			Enumeration<? extends ZipEntry> e = jar.entries();
			while (e.hasMoreElements())
			{
				ZipEntry entry = e.nextElement();
				if (!entry.isDirectory())
				{
					if (entry.getName().endsWith(".class"))
					{
						String className = entry.getName().replace('/', '.');
						className = className.substring(0, className.length() - 6);
						
						//Skip subclasses
						if(className.contains("$"))
							continue;
						
						System.out.println("Found class " + className + " in jarfile, loading it...");
						
						try
						{
							Class<?> loadedClass = this.findClass(className);
							classes.put(className, loadedClass);
						}
						catch (ClassNotFoundException e1)
						{
							ChunkStoriesLoggerImplementation.getInstance().error("Class "+className+" was to be found in .jar file but classloader could not load it.");
							e1.printStackTrace();
							
							continue;
						}
						catch(LinkageError le)
						{
							//Don't care
						}
						//classes.add(className);
					}
				}
			}
	
			jar.close();
		}
	}

	private static URL[] urlHelper(Collection<File> files) throws MalformedURLException {
		URL[] urls = new URL[files.size()];
		Iterator<File> i = files.iterator();
		int j = 0;
		while(i.hasNext()) {
			File file = i.next();
			urls[j] = file.toURI().toURL();
			j++;
		}
		return urls;
	}

	public Collection<String> classes()
	{
		return classes.keySet();
	}
	
	public Class<?> obtainClass(String className)
	{
		return classes.get(className);
	}

	public Mod getResponsibleMod()
	{
		return responsibleMod;
	}
}
