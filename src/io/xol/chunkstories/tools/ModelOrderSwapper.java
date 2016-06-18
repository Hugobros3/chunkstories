package io.xol.chunkstories.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Reverses the winding order of a model in .model
 */
public class ModelOrderSwapper
{
	public static void main(String a[])
	{
		for(File f : new File(".").listFiles())
		{
			if(f.getName().endsWith(".model"))
			{
				try
				{
					swapModel(new BufferedReader(new FileReader(f)), new BufferedWriter(new FileWriter(new File("2/"+f.getName()))));
				}
				catch (FileNotFoundException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	private static void swapModel(BufferedReader reader, BufferedWriter writer) throws IOException
	{
		int v = 0;
		String line = null;
		String saved = null;
		while(reader != null && (line = reader.readLine()) != null)
		{
			String strippedLine = line;
			strippedLine = strippedLine.replace("\t", "");
			
			if(strippedLine.startsWith("v"))
			{
				v++;
				if(v == 1)
					writer.write(line+"\n");
				if(v == 2)
					saved = line;
				if(v == 3)
				{
					writer.write(line+"\n");
					writer.write(saved+"\n");
					v = 0;
				}
			}
			else
				writer.write(line+"\n");
			
		}
		writer.close();
	}
}
