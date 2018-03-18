//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.client;

public class ClientLimitations
{
	//Open GL limitations
	public static int gl_MaxTextureUnits;
	public static int gl_MaxTextureArraySize;
	public static boolean gl_IsInstancingSupported;
	public static boolean gl_openGL3Capable = true;
	public static boolean gl_fbExtCapable = false;
	public static boolean gl_InstancedArrays;

	public static boolean debugOpenGL = false;
	public static boolean ignoreObsoleteHardware = false;
	
	public static boolean isDebugAllowed = false;
}
