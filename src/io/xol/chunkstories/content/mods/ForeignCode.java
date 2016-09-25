package io.xol.chunkstories.content.mods;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Foreign content is anything found inside a jar and loaded by the game engine. Security measures applies unless configured otherwise
 */
public class ForeignCode extends URLClassLoader
{
	Mod responsibleMod;
	List<String> classes = new ArrayList<String>();
	
	public ForeignCode(Mod responsibleMod, File file, ClassLoader parentLoader) throws IOException
	{
		super(new URL[] { file.toURI().toURL() }, parentLoader);
		assert parentLoader != null;
		
		this.responsibleMod = responsibleMod;
		
		JarFile jar = new JarFile(file);
		
		//Lists classes to be found in that jarFile
		Enumeration<? extends ZipEntry> e = jar.entries();
		while(e.hasMoreElements())
		{
			ZipEntry entry = e.nextElement();
			if(!entry.isDirectory())
			{
				String className = entry.getName();
				System.out.println("Found class " + className);
				classes.add(className);
			}
		}
		
		jar.close();
	}
	
	public Iterator<String> getClasses()
	{
		return classes.iterator();
	}
	
	public Mod getResponsibleMod()
	{
		return responsibleMod;
	}
}
