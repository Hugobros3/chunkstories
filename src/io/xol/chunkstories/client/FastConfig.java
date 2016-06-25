package io.xol.chunkstories.client;

import io.xol.engine.base.GameWindowOpenGL;

import java.util.ArrayList;
import java.util.List;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class FastConfig
{
	// This class hold often-used values

	// RENDERING
	public static float viewDistance = 150;
	public static float fogDistance = 100;

	public static boolean openGL3Capable = true;
	public static boolean fbExtCapable = false;

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

	public static void define()
	{
		viewDistance = Client.getConfig().getIntProp("viewDistance", 128);

		shadowMapResolutions = Client.getConfig().getIntProp("shadowMapResolutions", 1024);

		dynamicGrass = Client.getConfig().getBooleanProp("dynamicGrass", true);
		debugGBuffers = Client.getConfig().getBooleanProp("debugGBuffers", false);
		
		hqTerrain = Client.getConfig().getBooleanProp("hqTerrain", true);
		perPixelFresnel = Client.getConfig().getBooleanProp("perPixelFresnel", true);
		doShadows = Client.getConfig().getBooleanProp("doShadows", true);
		doBloom = Client.getConfig().getBooleanProp("doBloom", true);
		ssaoQuality = Client.getConfig().getIntProp("ssaoQuality", 0);
		doClouds = Client.getConfig().getBooleanProp("doClouds", false);
		doRealtimeReflections = Client.getConfig().getBooleanProp("doRealtimeReflections", true);
		doDynamicCubemaps = Client.getConfig().getBooleanProp("doDynamicCubemaps", true);

		physicsVisualization = Client.getConfig().getBooleanProp("physicsVisualization", false);
		showDebugInfo = Client.getConfig().getBooleanProp("showDebugInfo", true);

		mouseSensitivity = Client.getConfig().getFloatProp("mouseSensitivity", 1f);
		fov = Client.getConfig().getFloatProp("fov", 45f);

		GameWindowOpenGL.setTargetFPS(Client.getConfig().getIntProp("framerate", -1));
	}

	public static float mouseSensitivity = 1f;
	public static float fov = 1f;
	
	public static boolean doDynamicCubemaps = true;
	public static boolean ignoreObsoleteHardware = false;

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
