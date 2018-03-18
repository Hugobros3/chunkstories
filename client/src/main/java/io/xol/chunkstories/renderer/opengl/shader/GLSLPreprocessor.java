//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.renderer.opengl.shader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.api.content.mods.ModsManager;
import io.xol.chunkstories.api.util.Configuration.Option;
import io.xol.chunkstories.api.util.Configuration.OptionBoolean;
import io.xol.chunkstories.client.Client;

/** Processes shaders 'include' statements and appends them a bunch of defines matching the client.rendering options */
public class GLSLPreprocessor
{
	public static void loadRecursivly(ModsManager modsManager, Asset asset, StringBuilder into, boolean type, Set<String> alreadyIncluded) throws IOException
	{
		if(alreadyIncluded == null)
			alreadyIncluded = new HashSet<String>();
		
		Reader fileReader = asset.reader();
		BufferedReader reader = new BufferedReader(fileReader);
		
		//List<String> blockingDef = new ArrayList<String>();
		String l;
		while ((l = reader.readLine()) != null)
		{
			String strippedLine = l.replace("	", "");
			if (strippedLine.startsWith("#"))
			{
				String[] line = strippedLine.substring(1).replace(">", "").replace("<", "").split(" ");
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
							
							lowerHalf = lowerHalf.substring(0, lowerHalf.lastIndexOf("/"));
							
							shaderInclude = lowerHalf + upperHalf;
						}
						
						Asset asset2include = modsManager.getAsset(shaderInclude);
						
						if(asset2include == null) {
							System.out.println("Couldn't find shader include: " + shaderInclude);
							continue;
						}
						
						//Prevents including same file twice and retarded loops
						if(alreadyIncluded.add(asset2include.getName()))
							loadRecursivly(modsManager, asset2include, into, type, alreadyIncluded);
						else
							System.out.println("Shader include : "+asset2include.getName()+" already included, skipping");
						
						continue;
					} else if(line[0].equals("version")) {
						into.append(l).append("\n");
						
						defineConfiguration(into); //<- append the defines stuff after the version tag
						continue;
					}
				}
			}
			
			into.append(l).append("\n");
		}
		reader.close();
		//return into;
	}
	
	private static void defineConfiguration(StringBuilder shaderSource) {
		for(Option option : Client.getInstance().getConfiguration().allOptions()) {
			String fullname = option.getName();
			if(fullname.startsWith("client.rendering")) {
				String abridgedName = fullname.substring("client.rendering.".length());
				
				abridgedName = abridgedName.replace(".", "_");

				if(option instanceof OptionBoolean) {
					if(option.getValue().equals("true"))
						shaderSource.append("#define " + abridgedName + " " + option.getValue().toString() + "\n");
					//shaderSource.append("#define " + abridgedName + " " + (option.getValue().equals("true") ? 1 : 0) + "\n");
				} else
					shaderSource.append("#define " + abridgedName + " " + option.getValue().toString() + "\n");
				//System.out.println("#define " + abridgedName + " " + option.getValue().toString());
			}
			
		}
	}
}
