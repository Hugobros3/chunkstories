//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.client;

import java.util.ArrayList;
import java.util.List;

import io.xol.chunkstories.api.client.ClientRenderingConfig;



public class RenderingConfig implements ClientRenderingConfig
{
	//Open GL limitations
	public static int gl_MaxTextureUnits;
	public static int gl_MaxTextureArraySize;
	public static boolean gl_IsInstancingSupported;
	public static boolean gl_openGL3Capable = true;
	public static boolean gl_fbExtCapable = false;
	public static boolean gl_InstancedArrays;

	public static boolean DEBUG_OPENGL = false;

	// RENDERING
	public static float viewDistance = 150;
	public static float fogDistance = 100;

	public static boolean dynamicGrass = false;

	public static boolean debugGBuffers = false;
	public static boolean debugPasses = false;

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
	
	public static boolean isDebugAllowed = false;
	
	public static void define()
	{
		viewDistance = Client.getInstance().getConfig().getInteger("viewDistance", 128);

		shadowMapResolutions = Client.getInstance().getConfig().getInteger("shadowMapResolutions", 1024);

		dynamicGrass = Client.getInstance().getConfig().getBoolean("dynamicGrass", true);
		debugGBuffers = Client.getInstance().getConfig().getBoolean("debugGBuffers", false);
		
		hqTerrain = Client.getInstance().getConfig().getBoolean("hqTerrain", true);
		perPixelFresnel = Client.getInstance().getConfig().getBoolean("perPixelFresnel", true);
		doShadows = Client.getInstance().getConfig().getBoolean("doShadows", true);
		doBloom = Client.getInstance().getConfig().getBoolean("doBloom", true);
		ssaoQuality = Client.getInstance().getConfig().getInteger("ssaoQuality", 0);
		doClouds = Client.getInstance().getConfig().getBoolean("doClouds", false);
		doRealtimeReflections = Client.getInstance().getConfig().getBoolean("doRealtimeReflections", true);
		doDynamicCubemaps = Client.getInstance().getConfig().getBoolean("doDynamicCubemaps", true);

		physicsVisualization = Client.getInstance().getConfig().getBoolean("physicsVisualization", false);
		showDebugInfo = Client.getInstance().getConfig().getBoolean("showDebugInfo", true);

		mouseSensitivity = Client.getInstance().getConfig().getFloat("mouseSensitivity", 1f);
		fov = Client.getInstance().getConfig().getFloat("fov", 45f);

		Client.getInstance().getGameWindow().setTargetFPS(Client.getInstance().getConfig().getInteger("framerate", -1));
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

	@Override
	public float getViewDistance() {
		return viewDistance;
	}

	@Override
	public float getFogDistance() {
		return fogDistance;
	}

	@Override
	public boolean isDynamicGrass() {
		return dynamicGrass;
	}

	@Override
	public boolean isHqTerrain() {
		return hqTerrain;
	}

	@Override
	public boolean isPerPixelFresnel() {
		return perPixelFresnel;
	}

	@Override
	public boolean isDoShadows() {
		return doShadows;
	}

	@Override
	public boolean isDoBloom() {
		return doBloom;
	}

	@Override
	public boolean isDoClouds() {
		return doClouds;
	}

	@Override
	public int getShadowMapResolutions() {
		return shadowMapResolutions;
	}

	@Override
	public int getSsaoQuality() {
		return ssaoQuality;
	}

	@Override
	public boolean isDoRealtimeReflections() {
		return doRealtimeReflections;
	}

	@Override
	public boolean isPhysicsVisualization() {
		return physicsVisualization;
	}

	@Override
	public boolean isShowDebugInfo() {
		return showDebugInfo;
	}

	@Override
	public float getMouseSensitivity() {
		return mouseSensitivity;
	}

	@Override
	public float getFov() {
		return fov;
	}

	@Override
	public boolean isDoDynamicCubemaps() {
		return doDynamicCubemaps;
	}

	@Override
	public int getAnimationCacheMaxSize() {
		return animationCacheMaxSize;
	}

	@Override
	public int getAnimationCacheFrameRate() {
		return animationCacheFrameRate;
	}
	
	@Override
	public boolean isDebugAllowed() {
		return isDebugAllowed;
	}
}
