package io.xol.chunkstories.client;

import io.xol.engine.base.GameWindowOpenGL;

import java.util.ArrayList;
import java.util.List;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class RenderingConfig
{
	//Open GL limitations
	public static int gl_MaxTextureUnits;
	public static boolean gl_IsInstancingSupported;
	public static boolean gl_openGL3Capable = true;
	public static boolean gl_fbExtCapable = false;

	// RENDERING
	public static float viewDistance = 150;
	public static float fogDistance = 100;

	public static boolean dynamicGrass = false;

	public static boolean debugGBuffers = false;

	public static boolean hqTerrain = true;
	public static boolean perPixelFresnel = false;
	public static boolean doShadows = true;
	public static boolean doBloom = true;
	public static boolean doClouds = true;
	public static int shadowMapResolutions = 1024;
	public static int ssaoQuality = 1;
	public static boolean doRealtimeReflections = false;

	public static boolean physicsVisualization = false;
	public static boolean showDebugInfo = false;

	public static float mouseSensitivity = 1f;
	public static float fov = 1f;
	
	public static boolean doDynamicCubemaps = true;
	public static boolean ignoreObsoleteHardware = false;
	
	public static int animationCacheMaxSize = 128 * 1024; // 128kb max per animation
	public static int animationCacheFrameRate = 60;
	
	public static void define()
	{
		viewDistance = Client.getConfig().getInteger("viewDistance", 128);

		shadowMapResolutions = Client.getConfig().getInteger("shadowMapResolutions", 1024);

		dynamicGrass = Client.getConfig().getBoolean("dynamicGrass", true);
		debugGBuffers = Client.getConfig().getBoolean("debugGBuffers", false);
		
		hqTerrain = Client.getConfig().getBoolean("hqTerrain", true);
		perPixelFresnel = Client.getConfig().getBoolean("perPixelFresnel", true);
		doShadows = Client.getConfig().getBoolean("doShadows", true);
		doBloom = Client.getConfig().getBoolean("doBloom", true);
		ssaoQuality = Client.getConfig().getInteger("ssaoQuality", 0);
		doClouds = Client.getConfig().getBoolean("doClouds", false);
		doRealtimeReflections = Client.getConfig().getBoolean("doRealtimeReflections", true);
		doDynamicCubemaps = Client.getConfig().getBoolean("doDynamicCubemaps", true);

		physicsVisualization = Client.getConfig().getBoolean("physicsVisualization", false);
		showDebugInfo = Client.getConfig().getBoolean("showDebugInfo", true);

		mouseSensitivity = Client.getConfig().getFloat("mouseSensitivity", 1f);
		fov = Client.getConfig().getFloat("fov", 45f);

		GameWindowOpenGL.setTargetFPS(Client.getConfig().getInteger("framerate", -1));
	}

	public static String[] getShaderConfig()
	{
		List<String> parameters = new ArrayList<String>();
		if (doShadows)
			parameters.add("shadows");
		if (dynamicGrass)
			parameters.add("dynamicGrass");
		if (debugGBuffers)
			parameters.add("debugGBuffers");
		if (doBloom)
			parameters.add("doBloom");
		if (doClouds)
			parameters.add("doClouds");
		if (hqTerrain)
			parameters.add("hqTerrain");
		if (perPixelFresnel)
			parameters.add("perPixelFresnel");
		if (ssaoQuality > 0)
			parameters.add("ssao");
		if (doRealtimeReflections)
			parameters.add("doRealtimeReflections");
		if (doDynamicCubemaps)
			parameters.add("doDynamicCubemaps");

		String[] array = new String[parameters.size()];
		for (int i = 0; i < array.length; i++)
			array[i] = parameters.get(i);
		return array;
	}
}
