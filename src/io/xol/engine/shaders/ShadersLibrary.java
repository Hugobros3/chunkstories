package io.xol.engine.shaders;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import io.xol.chunkstories.client.FastConfig;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ShadersLibrary
{
	static Map<String, ShaderProgram> loadedShaders = new HashMap<String, ShaderProgram>();
	
	public static ShaderProgram getShaderProgram(String name)
	{
		if(!loadedShaders.containsKey(name))
			loadShader(name);
		return loadedShaders.get(name);
	}
	
	static boolean loadShader(String name)
	{
		//TODO support for external shaders !
		String shaderPath = "res/shaders/"+name;
		ShaderProgram subject = new ShaderProgram(shaderPath, FastConfig.getShaderConfig());
		//if(subject.loadOK)
		loadedShaders.put(name, subject);
		return subject.loadOK;
	}
	
	public static void preloadShaders()
	{
		//TODO support for external shaders !
		File shadersDir = new File("res/shaders/");
		if(shadersDir.exists() && shadersDir.isDirectory())
			for(File f : shadersDir.listFiles())
				if(f.isDirectory())
					loadShader(f.getName());
				
	}
	
	public static void reloadShader(String shaderName)
	{
		ShaderProgram s = loadedShaders.get(shaderName);
		if(s != null)
			s.reload(FastConfig.getShaderConfig());
	}
	
	public static void reloadAllShaders()
	{
		for(ShaderProgram s : loadedShaders.values())
			s.reload(FastConfig.getShaderConfig());
	}
	
	public static void cleanup()
	{
		for(ShaderProgram s : loadedShaders.values())
			s.free();
		loadedShaders.clear();
	}
}
