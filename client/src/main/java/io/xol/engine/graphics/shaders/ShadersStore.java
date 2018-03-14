//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.engine.graphics.shaders;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import io.xol.chunkstories.api.client.ClientContent;
import io.xol.chunkstories.api.client.ClientContent.ShadersLibrary;

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
		ShaderProgram subject = new ShaderProgram(clientContent.modsManager(), name);
		loadedShaders.put(name, subject);
		return subject.loadOK;
	}
	
	public void preloadShaders()
	{
		//TODO support for external shaders !
		/*File shadersDir = new File("res/shaders/");
		if(shadersDir.exists() && shadersDir.isDirectory())
			for(File f : shadersDir.listFiles())
				if(f.isDirectory())
					loadShader(f.getName());*/
	}
	
	public void reloadShader(String shaderName)
	{
		ShaderProgram s = loadedShaders.get(shaderName);
		if(s != null)
			s.reload();
	}
	
	public void reloadAll()
	{
		for(ShaderProgram s : loadedShaders.values())
			s.reload();
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

	@Override
	public Logger logger() {
		return ShaderProgram.logger();
	}
}
