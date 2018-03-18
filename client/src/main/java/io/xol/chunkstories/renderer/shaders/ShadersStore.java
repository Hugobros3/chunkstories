//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.renderer.shaders;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import io.xol.chunkstories.api.client.ClientContent;
import io.xol.chunkstories.api.client.ClientContent.ShadersLibrary;
import io.xol.chunkstories.renderer.opengl.shader.ShaderGL;

public class ShadersStore implements ShadersLibrary {
	final ClientContent clientContent;
	final Map<String, ShaderGL> loadedShaders = new HashMap<String, ShaderGL>();

	public ShadersStore(ClientContent clientContent) {
		this.clientContent = clientContent;
	}

	public ShaderGL getShaderProgram(String name) {
		if (!loadedShaders.containsKey(name))
			loadShader(name);
		return loadedShaders.get(name);
	}

	boolean loadShader(String name) {
		ShaderGL subject = new ShaderGL(clientContent.modsManager(), name);
		loadedShaders.put(name, subject);
		return subject.isLoadedCorrectly();
	}

	public void preloadShaders() {
		// TODO support for external shaders !
		/*
		 * File shadersDir = new File("res/shaders/"); if(shadersDir.exists() &&
		 * shadersDir.isDirectory()) for(File f : shadersDir.listFiles())
		 * if(f.isDirectory()) loadShader(f.getName());
		 */
	}

	public void reloadShader(String shaderName) {
		ShaderGL shader = loadedShaders.get(shaderName);
		if (shader != null)
			shader.reload();
	}

	public void reloadAll() {
		for (ShaderGL s : loadedShaders.values())
			s.reload();
	}

	public void destroy() {
		for (ShaderGL shader : loadedShaders.values())
			shader.destroy();
		loadedShaders.clear();
	}

	@Override
	public ClientContent parent() {
		return clientContent;
	}

	@Override
	public Logger logger() {
		return ShaderGL.logger();
	}
}
