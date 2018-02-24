//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.engine.misc;



public class OSHelper
{
	public static String getOS()
	{
		String OS = System.getProperty("os.name").toLowerCase();
		if (OS.contains("windows"))
		{
			return "windows";
		}
		if (OS.contains("mac"))
		{
			return "macosx";
		}
		if (OS.contains("linux") || OS.contains("unix"))
		{
			return "linux";
		}
		if (OS.contains("bsd"))
		{
			return "freebsd";
		}
		if (OS.contains("sunos"))
		{
			return "solaris";
		}
		return "unknown";
	}

	public static boolean isWindows()
	{
		return getOS().equals("windows");
	}

	public static boolean isMac()
	{
		return getOS().equals("macosx");
	}

	public static boolean isLinux()
	{
		return getOS().equals("linux");
	}
}
