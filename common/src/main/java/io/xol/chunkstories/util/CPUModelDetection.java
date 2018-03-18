//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CPUModelDetection
{
	public static String detectModel()
	{
		String command = "";

		String cpuName = "";
		String cpuFreq = "unknown";

		if (OSHelper.isLinux())
		{
			command = "cat /proc/cpuinfo";
		}
		else if (OSHelper.isWindows())
		{
			command = "cmd /C WMIC CPU Get /Format:List <NUL";
		}
		else
			return "Mac, not implemented, workarround";
		Process process = null;
		try
		{
			process = Runtime.getRuntime().exec(command);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		int cores = 0;

		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String line;
		try
		{
			while ((line = reader.readLine()) != null)
			{
				//line = reader.readLine();
				
				if(line.startsWith("Name="))
					cpuName = line.split("=")[1];
				if(line.startsWith("model name"))
				{
					cpuName = line.split(":")[1];
					// On linux we count cores
					cores++;
				}
				if(line.startsWith("NumberOfCores="))
					cores += Integer.parseInt(line.split("=")[1]);
				
				if(line.startsWith("CurrentClockSpeed"))
					cpuFreq = line.split("=")[1];
				if(line.startsWith("cpu MHz"))
					cpuFreq = line.split(":")[1];
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return "Crashed during detection";
		}

		return cores + "x " + cpuName + " @ " + cpuFreq + "MHz";
	}

}
