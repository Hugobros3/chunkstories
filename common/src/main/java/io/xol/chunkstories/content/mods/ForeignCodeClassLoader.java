//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.content.mods;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.content.mods.Mod;

/**
 * Foreign content is anything found inside a jar and loaded by the game engine. Security measures applies unless configured otherwise
 */
public class ForeignCodeClassLoader extends URLClassLoader
{
	ModImplementation responsibleMod;
	Map<String, Class<?>> classes = new HashMap<String, Class<?>>();
	
	private static final Logger logger = LoggerFactory.getLogger("mods.classloader");

	public ForeignCodeClassLoader(ModImplementation responsibleMod, ClassLoader parentLoader, Collection<File> files) throws IOException
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
						
						logger.debug("Found class " + className + " in jarfile, loading it...");
						
						try
						{
							//OpenJDK weirdness: Unlike Oracle's implementation, OpenJDK loads the referenced classes when it loads one
							//meaning when I call findClass() on every .class I end up with duplicate findClass() calls, and it doesn't like it very much
							
							//The fix is easy: call findLoadedClass() first and check we're not duplicating the request
							Class<?> alreadyLoadedClass = this.findLoadedClass(className);
							Class<?> loadedClass = alreadyLoadedClass != null ? alreadyLoadedClass : this.findClass(className);
							classes.put(className, loadedClass);
						}
						catch (ClassNotFoundException e1)
						{
							responsibleMod.logger().error("Class "+className+" was to be found in .jar file but classloader could not load it.");
							e1.printStackTrace();
							
							continue;
						}
						catch(LinkageError le)
						{
							logger.error("This should not happen!", le);
							//Don't care
						}
						//classes.add(className);
					}
				}
			}
	
			jar.close();
		}
	}

	public Collection<String> classes()
	{
		return classes.keySet();
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
	
	@Override
	protected Class<?> findClass(String arg0) throws ClassNotFoundException {
		return super.findClass(arg0);
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
