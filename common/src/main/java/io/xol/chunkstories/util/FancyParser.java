//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class FancyParser
{
	public static Map<String, String> parseArguments(String[] args)
	{
		Map<String, String> parsedArguments = new HashMap<String, String>();
		
		String completeArgumentsString = " ";
		for(int i = 0; i < args.length; i++)
		{
			System.out.println("Procession : "+args[i]);
			completeArgumentsString += args[i];
			if(i < args.length - 1)
				completeArgumentsString += " ";
		}
		
		System.out.println(completeArgumentsString);
		
		while(completeArgumentsString.indexOf("-") != -1)
		{
			//Get rid of spaces one by one, skipping over malformed arguments
			while(completeArgumentsString.indexOf(" ") != -1 && completeArgumentsString.indexOf(" ") < completeArgumentsString.indexOf("-"))
				completeArgumentsString = completeArgumentsString.substring(completeArgumentsString.indexOf(" ") + 1);
			
			System.out.println(completeArgumentsString);
			
			//When it does find a proper argument
			if(completeArgumentsString.startsWith("-"))
			{
				//Extracts the argument substring
				int str_end = completeArgumentsString.indexOf(" ") + 1;
				if(str_end == 0)
					str_end = completeArgumentsString.length();
				
				if(completeArgumentsString.contains("="))
				{
					//Special case when double-quotes are used, we have to ignore them by pair
					if(completeArgumentsString.charAt(completeArgumentsString.indexOf('=') + 2) == '\"')
					{
						System.out.println("Found quote");
						int quotesEnd = completeArgumentsString.indexOf('\"', completeArgumentsString.indexOf('\"') + 1);
						str_end = quotesEnd;
					}
					
					
					String argumentString = completeArgumentsString.substring(completeArgumentsString.indexOf('-'), str_end);
					
					//Trim the arguments string
					completeArgumentsString = completeArgumentsString.substring(str_end);
					
					System.out.println("Isolated arg:" + argumentString);
				}
				else
				{
					String argumentString = completeArgumentsString.substring(completeArgumentsString.indexOf('-'), str_end);
					
					//Trim the arguments string
					completeArgumentsString = completeArgumentsString.substring(str_end);
					
					System.out.println("Isolated arg:" + argumentString);
				}
			}
			
		}
		
		return parsedArguments;
	}
	
	public static void main(String[] a)
	{
		for(Entry<String, String> parsed : parseArguments(a).entrySet())
			System.out.println(">>"+parsed.getKey()+" >> "+parsed.getValue());
	}
}
