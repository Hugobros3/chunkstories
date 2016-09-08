package io.xol.engine.graphics.shaders;

import io.xol.chunkstories.api.rendering.ShaderInterface;
import io.xol.chunkstories.api.rendering.TexturingConfiguration;
import io.xol.chunkstories.api.rendering.UniformsConfiguration;
import io.xol.chunkstories.tools.ChunkStoriesLogger;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import io.xol.engine.graphics.textures.Cubemap;
import io.xol.engine.graphics.textures.Texture1D;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.math.lalgb.Matrix3f;
import io.xol.engine.math.lalgb.Vector2f;
import io.xol.engine.math.lalgb.Vector3d;
import io.xol.engine.math.lalgb.Vector3f;
import io.xol.engine.math.lalgb.Vector4f;

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
public class ShaderProgram implements ShaderInterface
{
	private String filename;
	String shaderName = filename;

	private int shaderProgramId;
	private int vertexShaderId;
	private int fragShaderId;

	boolean loadOK = false;

	private static FloatBuffer matrix4fBuffer = BufferUtils.createFloatBuffer(16);
	private static FloatBuffer matrix3fBuffer = BufferUtils.createFloatBuffer(9);

	private Map<String, Integer> uniformsLocations = new HashMap<String, Integer>();
	private Map<String, Integer> attributesLocations = new HashMap<String, Integer>();
	
	private Map<String, Integer> uniformsAttributesIntegers = new HashMap<String, Integer>();
	private Map<String, Float> uniformsAttributesFloat = new HashMap<String, Float>();
	private Map<String, Vector2f> uniformsAttributes2Float = new HashMap<String, Vector2f>();
	private Map<String, Vector3f> uniformsAttributes3Float = new HashMap<String, Vector3f>();
	private Map<String, Vector4f> uniformsAttributes4Float = new HashMap<String, Vector4f>();
	private Map<String, Matrix4f> uniformsAttributesMatrix4 = new HashMap<String, Matrix4f>();
	private Map<String, Matrix3f> uniformsAttributesMatrix3 = new HashMap<String, Matrix3f>();

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
	
	public String getShaderName()
	{
		return shaderName;
	}

	private void load(String[] parameters)
	{
		shaderName = filename;
		if (filename.lastIndexOf("/") != -1)
			shaderName = filename.substring(filename.lastIndexOf("/"), filename.length());
		
		shaderProgramId = glCreateProgram();
		vertexShaderId = glCreateShader(GL_VERTEX_SHADER);
		fragShaderId = glCreateShader(GL_FRAGMENT_SHADER);

		StringBuilder vertexSource = new StringBuilder();
		StringBuilder fragSource = new StringBuilder();
		try
		{
			vertexSource = CustomGLSLReader.loadRecursivly(new File(filename + "/" + shaderName + ".vs"), vertexSource, parameters, false, null);
			fragSource = CustomGLSLReader.loadRecursivly(new File(filename + "/" + shaderName + ".fs"), fragSource, parameters, true, null);
		}
		catch (IOException e)
		{
			ChunkStoriesLogger.getInstance().log("Failed to load shader program " + filename, ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.ERROR);
			e.printStackTrace();
			return;
		}

		//Anti-AMD bullshit : AMD drivers have this stupid design decision of attributing vertex attributes by lexicographical order instead of
		//order of apparition, leading to these annoying issues where an optional attribute is in index zero and disabling it screws the drawcalls
		//To counter this, this piece of code forces the attributes locations to be declared in proper order
		int i = 0;
		for (String line : vertexSource.toString().split("\n"))
		{
			//We're still GLSL 130
			if (line.startsWith("in ") || line.startsWith("attribute "))
			{
				String attributeName = line.split(" ")[2].replace(";", "");
				//System.out.println("Binding " + attributeName + " to location : " + i);
				glBindAttribLocation(shaderProgramId, i, attributeName);
				i++;
			}
		}

		glShaderSource(vertexShaderId, vertexSource);
		glCompileShader(vertexShaderId);

		glShaderSource(fragShaderId, fragSource);
		glCompileShader(fragShaderId);

		if (glGetShaderi(fragShaderId, GL_COMPILE_STATUS) == GL_FALSE)
		{
			ChunkStoriesLogger.getInstance().log("Failed to compile shader program " + filename + " (fragment)", ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.ERROR);

			String errorsSource = glGetShaderInfoLog(fragShaderId, 5000);

			String[] errorsLines = errorsSource.split("\n");
			String[] sourceLines = fragSource.toString().split("\n");
			for (String line : errorsLines)
			{
				ChunkStoriesLogger.getInstance().log(line, ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.WARN);
				if (line.toLowerCase().startsWith("error: "))
				{
					String[] parsed = line.split(":");
					if (parsed.length >= 3)
					{
						int lineNumber = Integer.parseInt(parsed[2]);
						if (sourceLines.length > lineNumber)
						{
							System.out.println("@line: " + lineNumber + ": " + sourceLines[lineNumber]);
						}
					}
				}
			}
			return;
		}
		if (glGetShaderi(vertexShaderId, GL_COMPILE_STATUS) == GL_FALSE)
		{
			ChunkStoriesLogger.getInstance().log("Failed to compile shader program " + filename + " (vertex)", ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.ERROR);

			String errorsSource = glGetShaderInfoLog(vertexShaderId, 5000);

			String[] errorsLines = errorsSource.split("\n");
			String[] sourceLines = fragSource.toString().split("\n");
			for (String line : errorsLines)
			{
				ChunkStoriesLogger.getInstance().log(line, ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.WARN);
				if (line.toLowerCase().startsWith("error: "))
				{
					String[] parsed = line.split(":");
					if (parsed.length >= 3)
					{
						int lineNumber = Integer.parseInt(parsed[2]);
						if (sourceLines.length > lineNumber)
						{
							System.out.println("@line: " + lineNumber + ": " + sourceLines[lineNumber]);
						}
					}
				}
			}
			return;
		}
		glAttachShader(shaderProgramId, vertexShaderId);
		glAttachShader(shaderProgramId, fragShaderId);

		glLinkProgram(shaderProgramId);

		if (glGetProgrami(shaderProgramId, GL_LINK_STATUS) == GL_FALSE)
		{
			ChunkStoriesLogger.getInstance().log("Failed to link program " + filename + "", ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.ERROR);

			String errorsSource = glGetProgramInfoLog(shaderProgramId, 5000);

			String[] errorsLines = errorsSource.split("\n");
			String[] sourceLines = fragSource.toString().split("\n");
			for (String line : errorsLines)
			{
				ChunkStoriesLogger.getInstance().log(line, ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.WARN);
				if (line.toLowerCase().startsWith("error: "))
				{
					String[] parsed = line.split(":");
					if (parsed.length >= 3)
					{
						int lineNumber = Integer.parseInt(parsed[2]);
						if (sourceLines.length > lineNumber)
						{
							System.out.println("@line: " + lineNumber + ": " + sourceLines[lineNumber]);
						}
					}
				}
			}

			return;
		}

		glValidateProgram(shaderProgramId);

		if (glGetProgrami(shaderProgramId, GL_VALIDATE_STATUS) == GL_FALSE)
		{
			ChunkStoriesLogger.getInstance().log("Failed to validate program " + filename + "", ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.ERROR);

			String errorsSource = glGetProgramInfoLog(shaderProgramId, 5000);

			String[] errorsLines = errorsSource.split("\n");
			String[] sourceLines = fragSource.toString().split("\n");
			for (String line : errorsLines)
			{
				ChunkStoriesLogger.getInstance().log(line, ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.WARN);
				if (line.toLowerCase().startsWith("error: "))
				{
					String[] parsed = line.split(":");
					if (parsed.length >= 3)
					{
						int lineNumber = Integer.parseInt(parsed[2]);
						if (sourceLines.length > lineNumber)
						{
							System.out.println("@line: " + lineNumber + ": " + sourceLines[lineNumber]);
						}
					}
				}
			}

			return;
		}
		
		loadOK = true;
	}
	
	/*public void setUniformSampler(int id, String name, Texture2D texture)
	{
		if(id == 0)
		{
			this.setUniformInt(name, id);
			selectTextureUnit(id);
			texture.bind();
			//glBindTexture(GL_TEXTURE_2D, texId);
			glActiveTexture(GL_TEXTURE0);
		}
		else
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
	}*/

	public int getUniformLocation(String name)
	{
		if (uniformsLocations.containsKey(name))
			return uniformsLocations.get(name);
		else
		{
			int location = glGetUniformLocation(shaderProgramId, name);
			
			if(location == GL_INVALID_OPERATION || location == GL_INVALID_VALUE)
				 location = -1;
			
			uniformsLocations.put(name, location);
			return location;
		}
	}

	/*public void setUniformMatrix4f(String name, FloatBuffer fb)
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
		setUniformFloat3(name, vec3.getX(), vec3.getY(), vec3.getZ());
	}

	public void setUniformFloat3(String name, double d1, double d2, double d3)
	{
		glUniform3f(getUniformLocation(name), (float) d1, (float) d2, (float) d3);
	}

	public void setUniformFloat4(String name, Vector4f vec4)
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
*/

	@Override
	public void setUniform1i(String uniformName, int uniformData)
	{
		uniformsAttributesIntegers.put(uniformName, uniformData);
	}

	@Override
	public void setUniform1f(String uniformName, double uniformData)
	{
		uniformsAttributesFloat.put(uniformName, (float)uniformData);
	}

	@Override
	public void setUniform2f(String uniformName, double uniformData_x, double uniformData_y)
	{
		uniformsAttributes2Float.put(uniformName, new Vector2f(uniformData_x, uniformData_y));
	}

	@Override
	public void setUniform2f(String uniformName, Vector2f uniformData)
	{
		uniformsAttributes2Float.put(uniformName, uniformData);
	}

	@Override
	public void setUniform3f(String uniformName, double uniformData_x, double uniformData_y, double uniformData_z)
	{
		uniformsAttributes3Float.put(uniformName, new Vector3f(uniformData_x, uniformData_y, uniformData_z));
	}

	@Override
	public void setUniform3f(String uniformName, Vector3d uniformData)
	{
		uniformsAttributes3Float.put(uniformName, uniformData.castToSimplePrecision());
	}

	@Override
	public void setUniform3f(String uniformName, Vector3f uniformData)
	{
		uniformsAttributes3Float.put(uniformName, uniformData);
	}

	@Override
	public void setUniform4f(String uniformName, double x, double y, double z, double w)
	{
		uniformsAttributes4Float.put(uniformName, new Vector4f(x, y, z, w));
	}

	@Override
	public void setUniform4f(String uniformName, Vector4f uniformData)
	{
		uniformsAttributes4Float.put(uniformName, uniformData);
	}

	@Override
	public void setUniformMatrix4f(String uniformName, Matrix4f uniformData)
	{
		uniformsAttributesMatrix4.put(uniformName, uniformData);
	}

	@Override
	public void setUniformMatrix3f(String uniformName, Matrix3f uniformData)
	{
		uniformsAttributesMatrix3.put(uniformName, uniformData);
	}

	class InternalUniformsConfiguration implements UniformsConfiguration {

		long code;
		
		public InternalUniformsConfiguration(Map<String, Integer> uniformsAttributesIntegers, Map<String, Float> uniformsAttributesFloat, Map<String, Vector2f> uniformsAttributes2Float, Map<String, Vector3f> uniformsAttributes3Float,
				Map<String, Vector4f> uniformsAttributes4Float, Map<String, Matrix4f> uniformsAttributesMatrix4, Map<String, Matrix3f> uniformsAttributesMatrix3)
		{
			//Le close enough
			code += uniformsAttributesIntegers.hashCode();
			code += uniformsAttributesFloat.hashCode();
			code += uniformsAttributes2Float.hashCode();
			code += uniformsAttributes3Float.hashCode();
			code += uniformsAttributes4Float.hashCode();
			code += uniformsAttributesMatrix4.hashCode();
			code += uniformsAttributesMatrix3.hashCode();
		}

		@Override
		public boolean isCompatibleWith(UniformsConfiguration u)
		{
			//This is quick n' dirty, should be made better someday
			if(u instanceof InternalUniformsConfiguration)
			{
				InternalUniformsConfiguration t = (InternalUniformsConfiguration)u;
				return t.code == code;
			}
			
			return false;
		}
	}
	
	public UniformsConfiguration getUniformsConfiguration()
	{
		return new InternalUniformsConfiguration(uniformsAttributesIntegers, uniformsAttributesFloat, uniformsAttributes2Float, uniformsAttributes3Float, uniformsAttributes4Float, uniformsAttributesMatrix4, uniformsAttributesMatrix3);
	}
	
	public int getVertexAttributeLocation(String name)
	{
		if (attributesLocations.containsKey(name))
			return attributesLocations.get(name);
		else
		{
			int location = glGetAttribLocation(shaderProgramId, name);
			if (location == -1)
			{
				ChunkStoriesLogger.getInstance().warning("Warning, -1 location for VertexAttrib " + name + " in shader " + this.filename);
				//location = 0;
			}
			attributesLocations.put(name, location);
			return location;
		}
	}
	
	public void use()
	{
		glUseProgram(shaderProgramId);
	}

	protected void free()
	{
		glDeleteProgram(shaderProgramId);
		glDeleteShader(vertexShaderId);
		glDeleteShader(fragShaderId);

		uniformsLocations.clear();
		attributesLocations.clear();
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
