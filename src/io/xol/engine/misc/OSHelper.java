package io.xol.engine.misc;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

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
