package io.xol.chunkstories;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class VersionInfo
{
	public static String version = autoVersion();
	public static int networkProtocolVersion = 36;
	
	//public static short csfFormatVersion = 0x2b;

	private static String autoVersion() {
		
		//If compiled jar, it has this
		String ver = VersionInfo.class.getPackage().getImplementationVersion();
		if(ver != null)
			return ver;
		
		return "Debug build";
	}
}