package io.xol.engine.graphics.shaders;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import io.xol.chunkstories.api.client.ClientContent;
import io.xol.chunkstories.api.client.ClientContent.ShadersLibrary;
import io.xol.chunkstories.client.RenderingConfig;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ShadersStore implements ShadersLibrary
{
	final ClientContent clientContent;
	final Map<String, ShaderProgram> loadedShaders = new HashMap<String, ShaderProgram>();
	
	public ShadersStore(ClientContent clientContent) {
		this.clientContent = clientContent;
	}
	
	public ShaderProgram getShaderProgram(String name)
	{
		if(!loadedShaders.containsKey(name))
			loadShader(name);
		return loadedShaders.get(name);
	}
	
	boolean loadShader(String name)
	{
		ShaderProgram subject = new ShaderProgram(clientContent.modsManager(), name, RenderingConfig.getShaderConfig());
		loadedShaders.put(name, subject);
		return subject.loadOK;
	}
	
	public void preloadShaders()
	{
		//TODO support for external shaders !
		File shadersDir = new File("res/shaders/");
		if(shadersDir.exists() && shadersDir.isDirectory())
			for(File f : shadersDir.listFiles())
				if(f.isDirectory())
					loadShader(f.getName());
	}
	
	public void reloadShader(String shaderName)
	{
		ShaderProgram s = loadedShaders.get(shaderName);
		if(s != null)
			s.reload(RenderingConfig.getShaderConfig());
	}
	
	public void reloadAllShaders()
	{
		for(ShaderProgram s : loadedShaders.values())
			s.reload(RenderingConfig.getShaderConfig());
	}
	
	public void destroy()
	{
		for(ShaderProgram s : loadedShaders.values())
			s.free();
		loadedShaders.clear();
	}

	@Override
	public ClientContent parent() {
		return clientContent;
	}
}
