//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.engine.graphics.shaders;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.api.content.mods.ModsManager;



public class CustomGLSLReader
{
	public static void loadRecursivly(ModsManager modsManager, Asset asset, StringBuilder into, String[] parameters, boolean type, Set<String> alreadyIncluded) throws IOException
	{
		if(alreadyIncluded == null)
			alreadyIncluded = new HashSet<String>();
		
		//type : false = vertex, true = frag
		//FileReader fileReader = new FileReader(file);
		//BufferedReader reader = new BufferedReader(fileReader);
		
		Reader fileReader = asset.reader();
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
						String shaderInclude = asset.getName().substring(0, asset.getName().lastIndexOf("/") + 1) + fn;
						
						//System.out.println(shaderInclude);
						
						while(shaderInclude.indexOf("..") != -1) {
							int indexOf = shaderInclude.indexOf("..");
							
							if(indexOf <= 0) {
								System.out.println("Irresolvable path; going too far back in parents folder; "+shaderInclude);
								break;
							}
							
							String lowerHalf = shaderInclude.substring(0, indexOf - 1);
							String upperHalf = shaderInclude.substring(indexOf + 2);

							//System.out.println("lh0:"+lowerHalf);
							
							lowerHalf = lowerHalf.substring(0, lowerHalf.lastIndexOf("/"));
							
							//System.out.println("lh1:"+lowerHalf);
							
							shaderInclude = lowerHalf + upperHalf;
						}
						
						//System.out.println("done: "+shaderInclude);
						
						Asset asset2include = modsManager.getAsset(shaderInclude);
						
						if(asset2include == null) {
							System.out.println("Couldn't find shader include: " + shaderInclude);
							continue;
						}
						
						//Prevents including same file twice and retarded loops
						if(alreadyIncluded.add(asset2include.getName()))
							loadRecursivly(modsManager, asset2include, into, parameters, type, alreadyIncluded);
						else
							System.out.println("Shader include : "+asset2include.getName()+" already included, skipping");
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
		//return into;
	}
}
