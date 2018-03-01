//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories;

public class VersionInfo
{
	public static String version = autoVersion();
	public static int networkProtocolVersion = 37;
	
	public static short csfFormatVersion = 0x2d;

	private static String autoVersion() {
		
		//If compiled jar, it has this
		String ver = VersionInfo.class.getPackage().getImplementationVersion();
		if(ver != null)
			return ver;
		
		return "Debug build";
	}
}