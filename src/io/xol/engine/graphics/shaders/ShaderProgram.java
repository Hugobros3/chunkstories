package io.xol.engine.graphics.shaders;

import io.xol.chunkstories.api.math.Matrix3f;
import io.xol.chunkstories.api.math.Matrix4f;
import io.xol.chunkstories.api.math.vector.Vector2;
import io.xol.chunkstories.api.math.vector.Vector3;
import io.xol.chunkstories.api.math.vector.Vector4;
import io.xol.chunkstories.api.math.vector.sp.Vector2fm;
import io.xol.chunkstories.api.math.vector.sp.Vector3fm;
import io.xol.chunkstories.api.math.vector.sp.Vector4fm;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.api.rendering.pipeline.UniformsConfiguration;
import io.xol.chunkstories.tools.ChunkStoriesLogger;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

//(c) 2015-2017 XolioWare Interactive
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

	private HashMap<String, Object> uncommitedUniforms = new HashMap<String, Object>();
	private HashMap<String, Object> commitedUniforms = new HashMap<String, Object>();

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

		//Anti-Apple bullshit : Apple being so tight-ass about gl 3.2 CORE spec won't let me have gl_FragColor[] in GLSL 150.
		//So I'll just declare out values in my fragment shaders by order of expected bound MRT and roll this hack.
		int j = 0;
		for (String line : fragSource.toString().split("\n"))
		{
			//We're still GLSL 130
			if (line.startsWith("out "))
			{
				String outputName = line.split(" ")[2].replace(";", "");
				//System.out.println("Binding frag output " + outputName + " to location : " + j);
				glBindFragDataLocation(shaderProgramId, j, outputName);
				j++;
			}
		}
		
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
		uncommitedUniforms.put(uniformName, uniformData);
		//uniformsAttributesIntegers.put(uniformName, uniformData);
	}

	@Override
	public void setUniform1f(String uniformName, double uniformData)
	{
		uncommitedUniforms.put(uniformName, ((float)uniformData));
		//uniformsAttributesFloat.put(uniformName, (float) uniformData);
	}

	@Override
	public void setUniform2f(String uniformName, double uniformData_x, double uniformData_y)
	{
		setUniform2f(uniformName, new Vector2fm(uniformData_x, uniformData_y));
		//uncommitedUniforms.put(uniformName, uniformData);
		//uniformsAttributes2Float.put(uniformName, new Vector2fm(uniformData_x, uniformData_y));
	}

	@Override
	public void setUniform2f(String uniformName, Vector2<?> uniformData)
	{
		uncommitedUniforms.put(uniformName, uniformData.castToSinglePrecision());
		//uniformsAttributes2Float.put(uniformName, uniformData);
	}

	@Override
	public void setUniform3f(String uniformName, double uniformData_x, double uniformData_y, double uniformData_z)
	{
		setUniform3f(uniformName, new Vector3fm(uniformData_x, uniformData_y, uniformData_z));
		//uncommitedUniforms.put(uniformName, uniformData);
		//uniformsAttributes3Float.put(uniformName, new Vector3fm(uniformData_x, uniformData_y, uniformData_z));
	}

	@Override
	public void setUniform3f(String uniformName, Vector3<?> uniformData)
	{
		uncommitedUniforms.put(uniformName, uniformData.castToSinglePrecision());
		//uniformsAttributes3Float.put(uniformName, uniformData);
	}

	@Override
	public void setUniform4f(String uniformName, double x, double y, double z, double w)
	{
		setUniform4f(uniformName, new Vector4fm(x, y, z, w));
		//uncommitedUniforms.put(uniformName, uniformData);
		//uniformsAttributes4Float.put(uniformName, new Vector4fm(x, y, z, w));
	}

	@Override
	public void setUniform4f(String uniformName, Vector4<?> uniformData)
	{
		uncommitedUniforms.put(uniformName, uniformData.castToSinglePrecision());
		//uniformsAttributes4Float.put(uniformName, uniformData);
	}

	@Override
	public void setUniformMatrix4f(String uniformName, Matrix4f uniformData)
	{
		uncommitedUniforms.put(uniformName, uniformData);
		//uniformsAttributesMatrix4.put(uniformName, uniformData);
	}

	@Override
	public void setUniformMatrix3f(String uniformName, Matrix3f uniformData)
	{
		uncommitedUniforms.put(uniformName, uniformData);
		//uniformsAttributesMatrix3.put(uniformName, uniformData);
	}

	public class InternalUniformsConfiguration implements UniformsConfiguration
	{
		Map<String, Object> commit;
		
		public InternalUniformsConfiguration(Map<String, Object> commit)
		{
			this.commit = commit;
		}

		@Override
		public boolean isCompatibleWith(UniformsConfiguration u)
		{
			//This is quick n' dirty, should be made better someday
			if (u instanceof InternalUniformsConfiguration)
			{
				InternalUniformsConfiguration t = (InternalUniformsConfiguration) u;
				return t.commit == commit;
			}

			return false;
		}

		@Override
		public void setup(RenderingInterface renderingInterface)
		{
			if(commit != null)
			{
				for (Entry<String, Object> e : commit.entrySet())
				{
					applyUniformAttribute(e.getKey(), e.getValue());
				}
			}
		}
	}

	public void applyUniformAttribute(String uniformName, Object uniformData)
	{
		int uniformLocation = getUniformLocation(uniformName);
		if(uniformLocation == -1)
			return;

		//System.out.println(uniformData);
		
		if(uniformData instanceof Float)
			glUniform1f(uniformLocation, (Float)uniformData);
		if(uniformData instanceof Double)
			glUniform1f(uniformLocation, (Float)((Double)uniformData).floatValue());
		else if(uniformData instanceof Integer)
			glUniform1i(uniformLocation, (Integer)uniformData);
		else if(uniformData instanceof Vector2fm)
			glUniform2f(uniformLocation, ((Vector2fm)uniformData).getX(), ((Vector2fm)uniformData).getY());
		else if(uniformData instanceof Vector4fm)
			glUniform4f(uniformLocation, ((Vector4fm)uniformData).getX(), ((Vector4fm)uniformData).getY(), ((Vector4fm)uniformData).getZ(), ((Vector4fm)uniformData).getW());
		//New vectors
		else if(uniformData instanceof Vector4)
			glUniform4f(uniformLocation, (float)((Vector4<?>)uniformData).getX(), (float)((Vector4<?>)uniformData).getY(), (float)((Vector4<?>)uniformData).getZ(), (float)((Vector4<?>)uniformData).getW());
		else if(uniformData instanceof Vector3)
			glUniform3f(uniformLocation, (float)((Vector3<?>)uniformData).getX(), (float)((Vector3<?>)uniformData).getY(), (float)((Vector3<?>)uniformData).getZ());
		else if(uniformData instanceof Vector2)
			glUniform2f(uniformLocation, (float)((Vector2<?>)uniformData).getX(), (float)((Vector2<?>)uniformData).getY());
		
		else if(uniformData instanceof Matrix4f)
		{
			((Matrix4f)uniformData).store(matrix4fBuffer);
			matrix4fBuffer.position(0);
			glUniformMatrix4(uniformLocation, false, matrix4fBuffer);
			matrix4fBuffer.clear();
		}
		else if(uniformData instanceof Matrix3f)
		{
			((Matrix3f)uniformData).store(matrix3fBuffer);
			matrix3fBuffer.position(0);
			glUniformMatrix3(uniformLocation, false, matrix3fBuffer);
			matrix3fBuffer.clear();
		}
	}

	public InternalUniformsConfiguration getUniformsConfiguration()
	{
		//Skip all this if nothing changed
		if(this.uncommitedUniforms.size() == 0)
			return new InternalUniformsConfiguration(null);
		
		//Make a map of the changes
		HashMap<String, Object> commit = new HashMap<String, Object>(uncommitedUniforms.size());
		
		//Iterate over uncommited changes
		Iterator<Entry<String, Object>> i = uncommitedUniforms.entrySet().iterator();
		while(i.hasNext())
		{
			Entry<String, Object> e = i.next();
			
			//Add it the the commited uniforms list and check it did change
			Object former = commitedUniforms.put(e.getKey(), e.getValue());
			//If it did change ( or didn't exist ), add it to the uniforms configuration commit
			if(former == null || !former.equals(e.getValue()))
				commit.put(e.getKey(), e.getValue());
			
			//Remove the from uncommited list
			i.remove();
		}
		
		//Return the uniform configuration reflecting those changes
		return new InternalUniformsConfiguration(commit);
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
		
		//setUniforms.clear();
		uncommitedUniforms.clear();
		commitedUniforms.clear();
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

	static boolean check(HashMap<String, Object> setUniforms2, Object o)
	{
		return setUniforms2.put("aaa", "whatever") == o;
	}

}
