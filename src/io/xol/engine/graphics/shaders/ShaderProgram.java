package io.xol.engine.graphics.shaders;

import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.ShaderInterface;
import io.xol.chunkstories.api.rendering.UniformsConfiguration;
import io.xol.chunkstories.tools.ChunkStoriesLogger;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import io.xol.engine.math.lalgb.Matrix3f;
import io.xol.engine.math.lalgb.Vector2f;
import io.xol.engine.math.lalgb.Vector3d;
import io.xol.engine.math.lalgb.Vector3f;
import io.xol.engine.math.lalgb.Vector4f;

import org.lwjgl.BufferUtils;

import io.xol.engine.math.lalgb.Matrix4f;
import static org.lwjgl.opengl.GL11.*;
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

	private HashMap<String, Integer> uniformsAttributesIntegers = new HashMap<String, Integer>(5);
	private HashMap<String, Float> uniformsAttributesFloat = new HashMap<String, Float>(10);
	private HashMap<String, Vector2f> uniformsAttributes2Float = new HashMap<String, Vector2f>(5);
	private HashMap<String, Vector3f> uniformsAttributes3Float = new HashMap<String, Vector3f>(5);
	private HashMap<String, Vector4f> uniformsAttributes4Float = new HashMap<String, Vector4f>(5);
	private HashMap<String, Matrix4f> uniformsAttributesMatrix4 = new HashMap<String, Matrix4f>(4);
	private HashMap<String, Matrix3f> uniformsAttributesMatrix3 = new HashMap<String, Matrix3f>(4);

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

	public int getUniformLocation(String name)
	{
		if (uniformsLocations.containsKey(name))
			return uniformsLocations.get(name);
		else
		{
			int location = glGetUniformLocation(shaderProgramId, name);

			if (location == GL_INVALID_OPERATION || location == GL_INVALID_VALUE)
				location = -1;

			uniformsLocations.put(name, location);
			return location;
		}
	}
	
	@Override
	public void setUniform1i(String uniformName, int uniformData)
	{
		uniformsAttributesIntegers.put(uniformName, uniformData);
	}

	@Override
	public void setUniform1f(String uniformName, double uniformData)
	{
		uniformsAttributesFloat.put(uniformName, (float) uniformData);
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

	private HashMap<String, Object> setUniforms = new HashMap<String, Object>(256);

	private boolean shouldUpdateUniform(String uniformName, Object uniform)
	{
		Object former = setUniforms.put(uniformName, uniform);
		return former == null || former != uniform;
	}

	public class InternalUniformsConfiguration implements UniformsConfiguration
	{

		long code;

		public InternalUniformsConfiguration(Map<String, Integer> uniformsAttributesIntegers, Map<String, Float> uniformsAttributesFloat, Map<String, Vector2f> uniformsAttributes2Float, Map<String, Vector3f> uniformsAttributes3Float,
				Map<String, Vector4f> uniformsAttributes4Float, Map<String, Matrix4f> uniformsAttributesMatrix4, Map<String, Matrix3f> uniformsAttributesMatrix3)
		{
			//Le close enough
		}

		@Override
		public boolean isCompatibleWith(UniformsConfiguration u)
		{
			//This is quick n' dirty, should be made better someday
			if (u instanceof InternalUniformsConfiguration)
			{
				InternalUniformsConfiguration t = (InternalUniformsConfiguration) u;
				return t.code == code;
			}

			return false;
		}

		@Override
		public void setup(RenderingInterface renderingInterface)
		{
			for (Entry<String, Integer> e : uniformsAttributesIntegers.entrySet())
			{
				if (shouldUpdateUniform(e.getKey(), e.getValue()))
				{
					//if(e.getKey().equals("alb2o"))
					//	System.out.println("m'k" + e.getValue());
					glUniform1i(getUniformLocation(e.getKey()), e.getValue());
				}
			}

			for (Entry<String, Float> e : uniformsAttributesFloat.entrySet())
				if (shouldUpdateUniform(e.getKey(), e.getValue()))
					glUniform1f(getUniformLocation(e.getKey()), e.getValue());

			for (Entry<String, Vector2f> e : uniformsAttributes2Float.entrySet())

				if (shouldUpdateUniform(e.getKey(), e.getValue()))
					glUniform2f(getUniformLocation(e.getKey()), e.getValue().x, e.getValue().y);

			for (Entry<String, Vector3f> e : uniformsAttributes3Float.entrySet())
				if (shouldUpdateUniform(e.getKey(), e.getValue()))
					glUniform3f(getUniformLocation(e.getKey()), e.getValue().x, e.getValue().y, e.getValue().z);

			for (Entry<String, Vector4f> e : uniformsAttributes4Float.entrySet())
				if (shouldUpdateUniform(e.getKey(), e.getValue()))
					glUniform4f(getUniformLocation(e.getKey()), e.getValue().x, e.getValue().y, e.getValue().z, e.getValue().w);

			for (Entry<String, Matrix4f> e : uniformsAttributesMatrix4.entrySet())
				if (shouldUpdateUniform(e.getKey(), e.getValue()))
				{
					e.getValue().store(matrix4fBuffer);
					matrix4fBuffer.position(0);
					glUniformMatrix4(getUniformLocation(e.getKey()), false, matrix4fBuffer);
					matrix4fBuffer.clear();
				}

			for (Entry<String, Matrix3f> e : uniformsAttributesMatrix3.entrySet())
				if (shouldUpdateUniform(e.getKey(), e.getValue()))
				{
					e.getValue().store(matrix3fBuffer);
					matrix3fBuffer.position(0);
					//System.out.println("uniformName"+e.getKey()+" / "+e.getValue());
					glUniformMatrix3(getUniformLocation(e.getKey()), false, matrix3fBuffer);
					matrix3fBuffer.clear();
				}
		}
	}

	public InternalUniformsConfiguration getUniformsConfiguration()
	{
		return new InternalUniformsConfiguration(null, null, null, null, null, null, null);

		/*return new InternalUniformsConfiguration((Map<String, Integer>) uniformsAttributesIntegers.clone()
				, (Map<String, Float>) uniformsAttributesFloat.clone()
				, (Map<String, Vector2f>) uniformsAttributes2Float.clone()
				, (Map<String, Vector3f>) uniformsAttributes3Float.clone()
				, (Map<String, Vector4f>) uniformsAttributes4Float.clone()
				, (Map<String, Matrix4f>) uniformsAttributesMatrix4.clone()
				, (Map<String, Matrix3f>) uniformsAttributesMatrix3.clone()
				);*/
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
		if (currentProgram == shaderProgramId)
			return;

		glUseProgram(shaderProgramId);
		currentProgram = shaderProgramId;
		//Reset uniforms when changing shader
		setUniforms.clear();
	}

	static int currentProgram = -2;

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

	public static void main(String[] a)
	{
		HashMap<String, Object> setUniforms = new HashMap<String, Object>(256);

		System.out.println(setUniforms.put("aaa", 15));
		System.out.println(setUniforms.put("aaa", 15));
		System.out.println(setUniforms.put("aaa", 16));
		System.out.println(check(setUniforms, 16));
	}

	static boolean check(HashMap<String, Object> setUniforms2, Object o)
	{
		return setUniforms2.put("aaa", "whatever") == o;
	}

}
