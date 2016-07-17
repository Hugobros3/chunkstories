package io.xol.engine.graphics.shaders;

import io.xol.chunkstories.tools.ChunkStoriesLogger;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import io.xol.engine.graphics.textures.Cubemap;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.math.lalgb.Matrix3f;
import io.xol.engine.math.lalgb.Vector2f;
import io.xol.engine.math.lalgb.Vector3d;
import io.xol.engine.math.lalgb.Vector3f;
import org.lwjgl.BufferUtils;
import io.xol.engine.math.lalgb.Matrix4f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL20.*;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

/**
 * Handles a GLSL shader
 */
public class ShaderProgram
{
	String filename;

	int shaderP;
	int vertexS;
	int fragS;

	boolean loadOK = false;

	FloatBuffer matrix4fBuffer = BufferUtils.createFloatBuffer(16);
	FloatBuffer matrix3fBuffer = BufferUtils.createFloatBuffer(9);

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

		String shaderName = filename;
		if(filename.lastIndexOf("/") != -1)
			shaderName = filename.substring(filename.lastIndexOf("/"), filename.length());
		
		StringBuilder vertexSource = new StringBuilder();
		StringBuilder fragSource = new StringBuilder();
		try
		{
			vertexSource = CustomGLSLReader.loadRecursivly(new File(filename + "/"+shaderName+".vs"), vertexSource, parameters, false);
			fragSource = CustomGLSLReader.loadRecursivly(new File(filename + "/"+shaderName+".fs"), fragSource, parameters, true);
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
			
			String errorsSource = glGetShaderInfoLog(fragS, 5000);
			
			String[] errorsLines = errorsSource.split("\n");
			String[] sourceLines = fragSource.toString().split("\n");
			for(String line : errorsLines)
			{
				ChunkStoriesLogger.getInstance().log(line, ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.WARN);
				if(line.toLowerCase().startsWith("error: "))
				{
					String[] parsed = line.split(":");
					if(parsed.length >= 3)
					{
						int lineNumber = Integer.parseInt(parsed[2]);
						if(sourceLines.length > lineNumber)
						{
							System.out.println("@line: "+lineNumber+": "+sourceLines[lineNumber]);
						}
					}
				}
			}
			return;
		}
		if (glGetShaderi(vertexS, GL_COMPILE_STATUS) == GL_FALSE)
		{
			ChunkStoriesLogger.getInstance().log("Failed to compile shader program " + filename + " (vertex)", ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.ERROR);
			
			String errorsSource = glGetShaderInfoLog(vertexS, 5000);
			
			String[] errorsLines = errorsSource.split("\n");
			String[] sourceLines = fragSource.toString().split("\n");
			for(String line : errorsLines)
			{
				ChunkStoriesLogger.getInstance().log(line, ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.WARN);
				if(line.toLowerCase().startsWith("error: "))
				{
					String[] parsed = line.split(":");
					if(parsed.length >= 3)
					{
						int lineNumber = Integer.parseInt(parsed[2]);
						if(sourceLines.length > lineNumber)
						{
							System.out.println("@line: "+lineNumber+": "+sourceLines[lineNumber]);
						}
					}
				}
			}
			//ChunkStoriesLogger.getInstance().log(vertexSource.toString(), ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.INFO);
			//ChunkStoriesLogger.getInstance().log(glGetShaderInfoLog(vertexS, 5000), ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.WARN);
			return;
		}
		glAttachShader(shaderP, vertexS);
		glAttachShader(shaderP, fragS);

		glLinkProgram(shaderP);

		if(glGetProgrami(shaderP, GL_LINK_STATUS) == GL_FALSE)
		{
			ChunkStoriesLogger.getInstance().log("Failed to link program " + filename + "", ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.ERROR);
			
			String errorsSource = glGetProgramInfoLog(shaderP, 5000);
			
			String[] errorsLines = errorsSource.split("\n");
			String[] sourceLines = fragSource.toString().split("\n");
			for(String line : errorsLines)
			{
				ChunkStoriesLogger.getInstance().log(line, ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.WARN);
				if(line.toLowerCase().startsWith("error: "))
				{
					String[] parsed = line.split(":");
					if(parsed.length >= 3)
					{
						int lineNumber = Integer.parseInt(parsed[2]);
						if(sourceLines.length > lineNumber)
						{
							System.out.println("@line: "+lineNumber+": "+sourceLines[lineNumber]);
						}
					}
				}
			}
			
			return;
		}

		glValidateProgram(shaderP);
		
		if(glGetProgrami(shaderP, GL_VALIDATE_STATUS) == GL_FALSE)
		{
			ChunkStoriesLogger.getInstance().log("Failed to validate program " + filename + "", ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.ERROR);
			
			String errorsSource = glGetProgramInfoLog(shaderP, 5000);
			
			String[] errorsLines = errorsSource.split("\n");
			String[] sourceLines = fragSource.toString().split("\n");
			for(String line : errorsLines)
			{
				ChunkStoriesLogger.getInstance().log(line, ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.WARN);
				if(line.toLowerCase().startsWith("error: "))
				{
					String[] parsed = line.split(":");
					if(parsed.length >= 3)
					{
						int lineNumber = Integer.parseInt(parsed[2]);
						if(sourceLines.length > lineNumber)
						{
							System.out.println("@line: "+lineNumber+": "+sourceLines[lineNumber]);
						}
					}
				}
			}
			
			return;
		}
		
		//ChunkStoriesLogger.getInstance().log("Shader program " + filename + " sucessfully loaded and compiled ! (P:" + shaderP + ")", ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.INFO);

		loadOK = true;
	}

	public void setUniformSampler(int id, String name, Texture2D texture)
	{
		setUniformSampler(id, name, texture.getId());
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

	public void setUniformSamplerCubemap(int id, String name, int texId)
	{
		this.setUniformInt(name, id);
		selectTextureUnit(id);
		glBindTexture(GL_TEXTURE_CUBE_MAP, texId);
		glActiveTexture(GL_TEXTURE0);
	}
	
	public void setUniformSamplerCubemap(int id, String name, Cubemap cubemap)
	{
		this.setUniformInt(name, id);
		selectTextureUnit(id);
		glBindTexture(GL_TEXTURE_CUBE_MAP, cubemap.getID());
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

	public void setUniformMatrix3f(String name, Matrix3f matrix3f)
	{
		matrix3f.store(matrix3fBuffer);
		setUniformMatrix3f(name, matrix3fBuffer);
	}

	public void setUniformFloat(String name, float f)
	{
		glUniform1f(getUniformLocation(name), f);
	}

	public void setUniformFloat2(String name, Vector2f vec2)
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

	public void setUniformFloat3(String name, Vector3d vec3)
	{
		setUniformFloat3(name, vec3.x, vec3.y, vec3.z);
	}

	public void setUniformFloat3(String name, float f, float f2, float f3)
	{
		glUniform3f(getUniformLocation(name), f, f2, f3);
	}

	public void setUniformFloat3(String name, double d1, double d2, double d3)
	{
		glUniform3f(getUniformLocation(name), (float)d1, (float)d2, (float)d3);
	}
	
	public void setUniformFloat4(String name, io.xol.engine.math.lalgb.Vector4f vec4)
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
			if(location == -1)
			{
				ChunkStoriesLogger.getInstance().warning("Warning, -1 location for VertexAttrib "+name+" in shader "+this.filename);
				//location = 0;
			}
			attributes.put(name, location);
			return location;
		}
	}

	public void use()
	{
		glUseProgram(shaderP);
	}

	protected void free()
	{
		glDeleteProgram(shaderP);
		glDeleteShader(vertexS);
		glDeleteShader(fragS);

		uniforms.clear();
		attributes.clear();
	}

	public void reload(String[] parameters)
	{
		free();
		load(parameters);
	}

	@Override
	public String toString()
	{
		return "[ShaderProgram : " + this.filename + "]";
	}
}
