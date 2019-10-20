//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.util;

public class VersionInfo {
	public static String version = autoVersion();
	public static int networkProtocolVersion = 37;

	private static String autoVersion() {

		// If compiled jar, it has this
		String ver = VersionInfo.class.getPackage().getImplementationVersion();
		if (ver != null)
			return ver;

		return "Debug build";
	}
}