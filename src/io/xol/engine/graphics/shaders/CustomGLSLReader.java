package io.xol.engine.graphics.shaders;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class CustomGLSLReader
{

	
	
	public static StringBuilder loadRecursivly(File file, StringBuilder into, String[] parameters, boolean type, Set<String> alreadyIncluded) throws IOException
	{
		if(alreadyIncluded == null)
			alreadyIncluded = new HashSet<String>();
		
		//type : false = vertex, true = frag
		FileReader fileReader = new FileReader(file);
		BufferedReader reader = new BufferedReader(fileReader);
		List<String> blockingDef = new ArrayList<String>();
		String l;
		while ((l = reader.readLine()) != null)
		{
			String strippedLine = l.replace("	", "");
			if (strippedLine.startsWith("<"))
			{
				String[] line = strippedLine.replace(">", "").replace("<", "").split(" ");
				if (line.length == 2)
				{
					if (line[0].equals("include"))
					{
						String fn = line[1];
						File file2include = new File(file.getParentFile().getAbsoluteFile() + "/" + fn);
						
						//Prevents including same file twice and retarded loops
						if(alreadyIncluded.add(file2include.getName()))
							loadRecursivly(file2include, into, parameters, type, alreadyIncluded);
						else
							System.out.println("file : "+file2include.getAbsolutePath()+" already included, skipping");
					}
					else if (line[0].equals("ifdef"))
					{
						String def = line[1];
						boolean shouldFind = true;
						if(def.startsWith("!"))
						{
							def = def.replace("!", "");	
							shouldFind = false;
						}
						boolean found = false;
						if (parameters != null)
						{
							for (String a : parameters)
								if (a.equals(def))
									found = true;
						}
						if ((shouldFind && !found) || !shouldFind && found)
						{
							blockingDef.add(def);
						}
					}
					else if (line[0].equals("endif"))
					{
						String def = line[1];
						if(def.startsWith("!"))
							def = def.replace("!", "");	
						blockingDef.remove(def);
					}
				}
				else
				{
					if(line[0].equals("vertex-only") && type)
						blockingDef.add("vertex-only");
					if(line[0].equals("/vertex-only") && type)
						blockingDef.remove("vertex-only");
				}
			}
			else
			{
				if (blockingDef.size() == 0)
					into.append(l).append("\n");
			}
		}
		reader.close();
		return into;
	}
}
