package io.xol.engine.shaders;

import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.engine.base.TexturesHandler;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Matrix4f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL20.*;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ShaderProgram
{
	// This class holds a shader program ( vertex/frag ), loads it, appy it and
	// free it once done.
	String filename;

	int shaderP;
	int vertexS;
	int fragS;

	boolean loadOK = false;

	FloatBuffer matrix4fBuffer = BufferUtils.createFloatBuffer(16);

	Map<String, Integer> uniforms = new HashMap<String, Integer>();
	Map<String, Integer> attributes = new HashMap<String, Integer>();
	
	protected ShaderProgram(String filename)
	{
		this.filename = filename;
		load(null);
	}

	protected ShaderProgram(String filename, String[] parameters)
	{
		this.filename = filename;
		load(parameters);
	}

	private void load(String[] parameters)
	{
		shaderP = glCreateProgram();
		vertexS = glCreateShader(GL_VERTEX_SHADER);
		fragS = glCreateShader(GL_FRAGMENT_SHADER);

		StringBuilder vertexSource = new StringBuilder();
		StringBuilder fragSource = new StringBuilder();
		try
		{
			vertexSource = CustomGLSLReader.loadRecursivly(new File(filename + "/main.vs"), vertexSource, parameters, false);
			fragSource = CustomGLSLReader.loadRecursivly(new File(filename + "/main.fs"), fragSource, parameters, true);
		}
		catch (IOException e)
		{
			ChunkStoriesLogger.getInstance().log("Failed to load shader program " + filename, ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.ERROR);
			// ChunkStoriesLogger.getInstance().log(,
			// ChunkStoriesLogger.LogType.RENDERING,
			// ChunkStoriesLogger.LogLevel.WARN);
			e.printStackTrace();
			return;
		}
		glShaderSource(vertexS, vertexSource);
		glCompileShader(vertexS);

		glShaderSource(fragS, fragSource);
		glCompileShader(fragS);

		if (glGetShaderi(fragS, GL_COMPILE_STATUS) == GL_FALSE)
		{
			ChunkStoriesLogger.getInstance().log("Failed to compile shader program " + filename + " (fragment)", ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.ERROR);
			ChunkStoriesLogger.getInstance().log(fragSource.toString(), ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.INFO);
			ChunkStoriesLogger.getInstance().log(glGetShaderInfoLog(fragS, 5000), ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.WARN);
			return;
		}
		if (glGetShaderi(vertexS, GL_COMPILE_STATUS) == GL_FALSE)
		{
			ChunkStoriesLogger.getInstance().log("Failed to compile shader program " + filename + " (vertex)", ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.ERROR);
			ChunkStoriesLogger.getInstance().log(vertexSource.toString(), ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.INFO);
			ChunkStoriesLogger.getInstance().log(glGetShaderInfoLog(vertexS, 5000), ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.WARN);
			return;
		}
		glAttachShader(shaderP, vertexS);
		glAttachShader(shaderP, fragS);

		glLinkProgram(shaderP);
		glValidateProgram(shaderP);

		ChunkStoriesLogger.getInstance().log("Shader program " + filename + " sucessfully loaded and compiled ! (P:" + shaderP + ")", ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.INFO);

		loadOK = true;
	}

	public void setUniformSampler(int id, String name, String texture)
	{
		setUniformSampler(id, name, TexturesHandler.idTexture(texture));
	}

	public void setUniformSampler(int id, String name, int texId)
	{
		this.setUniformInt(name, id);
		selectTextureUnit(id);
		glBindTexture(GL_TEXTURE_2D, texId);
		glActiveTexture(GL_TEXTURE0);
	}

	public void setUniformSampler1D(int id, String name, int texId)
	{
		this.setUniformInt(name, id);
		selectTextureUnit(id);
		glBindTexture(GL_TEXTURE_1D, texId);
		glActiveTexture(GL_TEXTURE0);
	}

	private void selectTextureUnit(int id)
	{
		if (id >= 16)
			return; // Hardlimit on number of textures
		glActiveTexture(GL_TEXTURE0 + id);
	}

	public void setUniformSamplerCube(int id, String name, int texId)
	{
		this.setUniformInt(name, id);
		selectTextureUnit(id);
		glBindTexture(GL_TEXTURE_CUBE_MAP, texId);
		glActiveTexture(GL_TEXTURE0);
	}

	public int getUniformLocation(String name)
	{
		if (uniforms.containsKey(name))
			return uniforms.get(name);
		else
		{
			int location = glGetUniformLocation(shaderP, name);
			uniforms.put(name, location);
			return location;
		}
		// return glGetUniformLocation(shaderP, name);
	}

	public void setUniformMatrix4f(String name, FloatBuffer fb)
	{
		fb.position(0);
		glUniformMatrix4(getUniformLocation(name), false, fb);
	}

	public void setUniformMatrix4f(String name, Matrix4f matrix4f)
	{
		matrix4f.store(matrix4fBuffer);
		setUniformMatrix4f(name, matrix4fBuffer);
	}

	public void setUniformMatrix3f(String name, FloatBuffer fb)
	{
		fb.position(0);
		glUniformMatrix3(getUniformLocation(name), false, fb);
	}

	public void setUniformFloat(String name, float f)
	{
		glUniform1f(getUniformLocation(name), f);
	}

	public void setUniformFloat3(String name, Vector2f vec2)
	{
		setUniformFloat2(name, vec2.x, vec2.y);
	}

	public void setUniformFloat2(String name, float f, float f2)
	{
		glUniform2f(getUniformLocation(name), f, f2);
	}

	public void setUniformFloat3(String name, Vector3f vec3)
	{
		setUniformFloat3(name, vec3.x, vec3.y, vec3.z);
	}

	public void setUniformFloat3(String name, float f, float f2, float f3)
	{
		glUniform3f(getUniformLocation(name), f, f2, f3);
	}
	
	public void setUniformFloat4(String name, org.lwjgl.util.vector.Vector4f vec4)
	{
		setUniformFloat4(name, vec4.x, vec4.y, vec4.z, vec4.w);
	}

	public void setUniformFloat4(String name, float f, float f2, float f3, float f4)
	{
		glUniform4f(getUniformLocation(name), f, f2, f3, f4);
	}

	public void setUniformInt(String name, int i)
	{
		glUniform1i(getUniformLocation(name), i);
	}

	public int getVertexAttributeLocation(String name)
	{
		if(attributes.containsKey(name))
			return attributes.get(name);
		else
		{
			int location = glGetAttribLocation(shaderP, name);
			attributes.put(name, location);
			return location;
		}
		//return glGetAttribLocation(shaderP, name);
	}

	public void use(boolean b)
	{
		if (b)
			glUseProgram(shaderP);
		else
			glUseProgram(0);
	}

	protected void free()
	{
		glDeleteProgram(shaderP);
		glDeleteShader(vertexS);
		glDeleteShader(fragS);

		uniforms.clear();
		attributes.clear();
	}

	protected void reload(String[] parameters)
	{
		free();
		load(parameters);
	}

	protected void reload()
	{
		free();
		load(null);
	}
}
