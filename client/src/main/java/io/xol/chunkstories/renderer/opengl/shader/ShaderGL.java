//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.renderer.opengl.shader;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL11.GL_INVALID_OPERATION;
import static org.lwjgl.opengl.GL11.GL_INVALID_VALUE;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.GL_VALIDATE_STATUS;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glBindAttribLocation;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glGetAttribLocation;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform2f;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glUniform4f;
import static org.lwjgl.opengl.GL20.glUniformMatrix3fv;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glValidateProgram;
import static org.lwjgl.opengl.GL30.glBindFragDataLocation;
import static org.lwjgl.opengl.GL32.GL_GEOMETRY_SHADER;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.joml.Matrix3fc;
import org.joml.Matrix4fc;
import org.joml.Vector2dc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4dc;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.api.content.mods.ModsManager;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.shader.Shader;

/**
 * Handles a GLSL shader
 */
public class ShaderGL implements Shader
{
	private final ModsManager modsManager;
	
	private final String shaderName;

	private int shaderProgramId;
	private int vertexShaderId;
	private int fragShaderId;
	private int geometryShaderId = -1;

	boolean needValidation = false;
	boolean loadedCorrectly = false;

	private static FloatBuffer matrix4fBuffer = BufferUtils.createFloatBuffer(16);
	private static FloatBuffer matrix3fBuffer = BufferUtils.createFloatBuffer(9);

	private Map<String, Integer> uniformsLocations = new HashMap<String, Integer>();
	private Map<String, Integer> attributesLocations = new HashMap<String, Integer>();

	private HashMap<String, Object> uncommitedUniforms = new HashMap<String, Object>();
	private HashMap<String, Object> commitedUniforms = new HashMap<String, Object>();
	
	private HashMap<String, SamplerType> samplers = new HashMap<>();

	public ShaderGL(ModsManager modsManager, String shaderName) {
		this.modsManager = modsManager;
		this.shaderName = shaderName;
		load();
	}

	public String getShaderName() {
		return shaderName;
	}

	private void load() {
		this.samplers.clear();

		shaderProgramId = glCreateProgram();

		StringBuilder vertexSource = new StringBuilder();
		StringBuilder fragSource = new StringBuilder();
		StringBuilder geometrySource = null;
		try {
			Asset vertexShader = modsManager.getAsset("./shaders/" + shaderName + "/" + shaderName + ".vs");
			Asset fragmentShader = modsManager.getAsset("./shaders/" + shaderName + "/" + shaderName + ".fs");

			// This might not exist !
			Asset geometryShader = modsManager.getAsset("./shaders/" + shaderName + "/" + shaderName + ".gs");

			GLSLPreprocessor.loadRecursivly(modsManager, vertexShader, vertexSource, false, null);
			GLSLPreprocessor.loadRecursivly(modsManager, fragmentShader, fragSource, true, null);

			// If a geometry shader asset was found
			if (geometryShader != null) {
				geometrySource = new StringBuilder();
				GLSLPreprocessor.loadRecursivly(modsManager, geometryShader, geometrySource, true, null);
			}
		} catch (IOException e) {
			logger().error("Failed to load shader program " + shaderName);
			logger().error("Exception: {}", e);
			return;
		}
		
		vertexShaderId = glCreateShader(GL_VERTEX_SHADER);
		fragShaderId = glCreateShader(GL_FRAGMENT_SHADER);
		
		if(geometrySource != null)
			geometryShaderId = glCreateShader(GL_GEOMETRY_SHADER);

		//Anti-AMD bullshit : AMD drivers have this stupid design decision of attributing vertex attributes by lexicographical order instead of
		//order of apparition, leading to these annoying issues where an optional attribute is in index zero and disabling it screws the drawcalls
		//To counter this, this piece of code forces the attributes locations to be declared in proper order
		int i = 0;
		for (String line : vertexSource.toString().split("\n")) {
			if (line.startsWith("in ")) {
				String attributeName = line.split(" ")[2].replace(";", "");
				glBindAttribLocation(shaderProgramId, i, attributeName);
				i++;
			}
		}

		glShaderSource(vertexShaderId, vertexSource);
		glCompileShader(vertexShaderId);

		//Parse the fragment shader to look for outputs and assign them locations based on their order of appearance
		//Also look for samplers
		int j = 0;
		for (String line : fragSource.toString().split("\n")) {
			if (line.startsWith("out ")) {
				String outputName = line.split(" ")[2].replace(";", "");
				if(outputName.equals("gl_FragDepth")) {
					logger.info("Writing to depth in frag enabled");
					continue;
				}
				glBindFragDataLocation(shaderProgramId, j, outputName);
				j++;
			} else if(line.startsWith("uniform ")) {
				String tokens[] = line.split(" ");
				if(tokens.length >= 3) {
					if(tokens[1].startsWith("sampler") || tokens[1].startsWith("usampler") || tokens[1].startsWith("isampler")) {
						SamplerType type = null;

						if(tokens[1].endsWith("Shadow"))
							tokens[1] = tokens[1].substring(0, tokens[1].length() - 6);
						
						if(tokens[1].endsWith("1D"))
							type = SamplerType.TEXTURE_1D;
						else if(tokens[1].endsWith("2D"))
							type = SamplerType.TEXTURE_2D;
						else if(tokens[1].endsWith("3D"))
							type = SamplerType.TEXTURE_3D;
						else if(tokens[1].endsWith("Cube"))
							type = SamplerType.CUBEMAP;
						else if(tokens[1].endsWith("2DArray"))
							type = SamplerType.ARRAY_TEXTURE_2D;
						else {
							logger.error("Could not recognize the sampler type: "+tokens[1]);
						}
						
						tokens[2] = tokens[2].substring(0, tokens[2].length() - 1);
						if(type != null) {
							samplers.put(tokens[2], type);
						}
					}
				}
			}
		}
		
		glShaderSource(fragShaderId, fragSource);
		glCompileShader(fragShaderId);
		
		if (geometrySource != null) {
			glShaderSource(geometryShaderId, geometrySource);
			glCompileShader(geometryShaderId);
		}

		if (glGetShaderi(fragShaderId, GL_COMPILE_STATUS) == GL_FALSE) {
			logger().error("Failed to compile shader program " + shaderName + " (fragment)");

			String errorsSource = glGetShaderInfoLog(fragShaderId, 5000);

			String[] errorsLines = errorsSource.split("\n");
			String[] sourceLines = fragSource.toString().split("\n");
			for (String line : errorsLines) {
				logger().debug(line);
				if (line.toLowerCase().startsWith("error: ")) {
					String[] parsed = line.split(":");
					if (parsed.length >= 3) {
						try {
							int lineNumber = Integer.parseInt(parsed[2]);
							if (sourceLines.length > lineNumber) {
								logger.debug("@line: " + lineNumber + ": " + sourceLines[lineNumber]);
							}
						} catch (Exception e) {
							logger.debug(line);
						}
					}
				}
			}
			return;
		}
		if (glGetShaderi(vertexShaderId, GL_COMPILE_STATUS) == GL_FALSE) {
			logger().error("Failed to compile shader program " + shaderName + " (vertex)");

			String errorsSource = glGetShaderInfoLog(vertexShaderId, 5000);

			String[] errorsLines = errorsSource.split("\n");
			String[] sourceLines = vertexSource.toString().split("\n");
			for (String line : errorsLines) {
				logger().debug(line);
				if (line.toLowerCase().startsWith("error: ")) {
					String[] parsed = line.split(":");
					if (parsed.length >= 3) {
						try {
							int lineNumber = Integer.parseInt(parsed[2]);
							if (sourceLines.length > lineNumber) {
								logger.debug("@line: " + lineNumber + ": " + sourceLines[lineNumber]);
							}
						} catch (Exception e) {
							logger.debug(line);
						}
					}
				}
			}
			return;
		}
		
		if (geometrySource != null && glGetShaderi(geometryShaderId, GL_COMPILE_STATUS) == GL_FALSE) {
			logger().error("Failed to compile shader program " + shaderName + " (geometry)");

			String errorsSource = glGetShaderInfoLog(geometryShaderId, 5000);

			String[] errorsLines = errorsSource.split("\n");
			String[] sourceLines = geometrySource.toString().split("\n");
			for (String line : errorsLines) {
				logger().debug(line);
				if (line.toLowerCase().startsWith("error: ")) {
					String[] parsed = line.split(":");
					if (parsed.length >= 3) {
						try {
							int lineNumber = Integer.parseInt(parsed[2]);
							if (sourceLines.length > lineNumber) {
								logger.debug("@line: " + lineNumber + ": " + sourceLines[lineNumber]);
							}
						} catch (Exception e) {
							logger.debug(line);
						}
					}
				}
			}
			return;
		}
		
		glAttachShader(shaderProgramId, vertexShaderId);
		glAttachShader(shaderProgramId, fragShaderId);

		if (geometrySource != null)
			glAttachShader(shaderProgramId, geometryShaderId);

		glLinkProgram(shaderProgramId);

		if (glGetProgrami(shaderProgramId, GL_LINK_STATUS) == GL_FALSE) {
			logger().error("Failed to link program " + shaderName + "");

			String errorsSource = glGetProgramInfoLog(shaderProgramId, 5000);

			String[] errorsLines = errorsSource.split("\n");
			for (String line : errorsLines) {
				logger().debug(line);
			}

			return;
		}

		needValidation = true;

		loadedCorrectly = true;
	}

	private static final Logger logger = LoggerFactory.getLogger("rendering.shaders");
	public static Logger logger() {
		return logger;
	}

	public int getUniformLocation(String name) {
		if (uniformsLocations.containsKey(name))
			return uniformsLocations.get(name);
		else {
			int location = glGetUniformLocation(shaderProgramId, name);

			if (location == GL_INVALID_OPERATION || location == GL_INVALID_VALUE)
				location = -1;

			uniformsLocations.put(name, location);
			return location;
		}
	}

	@Override
	public void setUniform1i(String uniformName, int uniformData) {
		uncommitedUniforms.put(uniformName, uniformData);
	}

	@Override
	public void setUniform1f(String uniformName, float uniformData) {
		uncommitedUniforms.put(uniformName, (uniformData));
	}

	@Override
	public void setUniform1f(String uniformName, double uniformData) {
		uncommitedUniforms.put(uniformName, ((float) uniformData));
	}

	@Override
	public void setUniform2f(String uniformName, float uniformData_x, float uniformData_y) {
		setUniform2f(uniformName, new Vector2f(uniformData_x, uniformData_y));
	}

	@Override
	public void setUniform2f(String uniformName, double uniformData_x, double uniformData_y) {
		setUniform2f(uniformName, new Vector2f((float) uniformData_x, (float) uniformData_y));
	}

	@Override
	public void setUniform2f(String uniformName, Vector2fc uniformData) {
		uncommitedUniforms.put(uniformName, uniformData);
	}

	@Override
	public void setUniform2f(String uniformName, Vector2dc uniformData) {
		setUniform2f(uniformName, (float) uniformData.x(), (float) uniformData.y());
	}

	@Override
	public void setUniform3f(String uniformName, float uniformData_x, float uniformData_y, float uniformData_z) {
		setUniform3f(uniformName, new Vector3f(uniformData_x, uniformData_y, uniformData_z));
	}

	@Override
	public void setUniform3f(String uniformName, double uniformData_x, double uniformData_y, double uniformData_z) {
		setUniform3f(uniformName, new Vector3f((float) uniformData_x, (float) uniformData_y, (float) uniformData_z));
	}

	@Override
	public void setUniform3f(String uniformName, Vector3fc uniformData) {
		uncommitedUniforms.put(uniformName, uniformData);
	}

	@Override
	public void setUniform3f(String uniformName, Vector3dc uniformData) {
		setUniform3f(uniformName, (float) uniformData.x(), (float) uniformData.y(), (float) uniformData.z());
	}

	@Override
	public void setUniform4f(String uniformName, float x, float y, float z, float w) {
		setUniform4f(uniformName, new Vector4f(x, y, z, w));
	}

	@Override
	public void setUniform4f(String uniformName, double x, double y, double z, double w) {
		setUniform4f(uniformName, new Vector4f((float) x, (float) y, (float) z, (float) w));
	}

	@Override
	public void setUniform4f(String uniformName, Vector4fc uniformData) {
		uncommitedUniforms.put(uniformName, uniformData);
	}

	@Override
	public void setUniform4f(String uniformName, Vector4dc uniformData) {
		setUniform4f(uniformName, (float)uniformData.x(), (float)uniformData.y(), (float)uniformData.z(), (float)uniformData.w());
	}

	@Override
	public void setUniformMatrix4f(String uniformName, Matrix4fc uniformData) {
		uncommitedUniforms.put(uniformName, uniformData);
	}

	@Override
	public void setUniformMatrix3f(String uniformName, Matrix3fc uniformData) {
		uncommitedUniforms.put(uniformName, uniformData);
	}

	public class InternalUniformsConfiguration {
		Map<String, Object> commit;

		public InternalUniformsConfiguration(Map<String, Object> commit) {
			this.commit = commit;
		}

		public void setup(RenderingInterface renderingInterface) {
			if (commit != null) {
				for (Entry<String, Object> e : commit.entrySet()) {
					applyUniformAttribute(e.getKey(), e.getValue());
				}
			}
		}
	}

	public void applyUniformAttribute(String uniformName, Object uniformData) {
		int uniformLocation = getUniformLocation(uniformName);
		if (uniformLocation == -1)
			return;
		
		if(uniformData instanceof Float)
			glUniform1f(uniformLocation, (Float)uniformData);
		if(uniformData instanceof Double)
			glUniform1f(uniformLocation, (Float)((Double)uniformData).floatValue());
		else if(uniformData instanceof Integer)
			glUniform1i(uniformLocation, (Integer)uniformData);
		else if(uniformData instanceof Vector2fc)
			glUniform2f(uniformLocation, ((Vector2fc)uniformData).x(), ((Vector2fc)uniformData).y());
		else if(uniformData instanceof Vector3fc)
			glUniform3f(uniformLocation, ((Vector3fc)uniformData).x(), ((Vector3fc)uniformData).y(), ((Vector3fc)uniformData).z());
		else if(uniformData instanceof Vector4fc)
			glUniform4f(uniformLocation, ((Vector4fc) uniformData).x(), ((Vector4fc) uniformData).y(), ((Vector4fc) uniformData).z(), ((Vector4fc) uniformData).w());

		else if (uniformData instanceof Matrix4fc) {
			((Matrix4fc) uniformData).get(matrix4fBuffer);
			matrix4fBuffer.position(0);
			glUniformMatrix4fv(uniformLocation, false, matrix4fBuffer);
			matrix4fBuffer.clear();
		} else if (uniformData instanceof Matrix3fc) {
			((Matrix3fc) uniformData).get(matrix3fBuffer);
			matrix3fBuffer.position(0);
			glUniformMatrix3fv(uniformLocation, false, matrix3fBuffer);
			matrix3fBuffer.clear();
		}
	}

	public InternalUniformsConfiguration getUniformsConfiguration() {
		// Skip all this if nothing changed
		if (this.uncommitedUniforms.size() == 0)
			return new InternalUniformsConfiguration(null);

		// Make a map of the changes
		HashMap<String, Object> commit = new HashMap<String, Object>(uncommitedUniforms.size());

		// Iterate over uncommited changes
		Iterator<Entry<String, Object>> i = uncommitedUniforms.entrySet().iterator();
		while (i.hasNext()) {
			Entry<String, Object> e = i.next();

			// Add it the the commited uniforms list and check it did change
			Object former = commitedUniforms.put(e.getKey(), e.getValue());
			// If it did change ( or didn't exist ), add it to the uniforms configuration
			// commit
			if (former == null || !former.equals(e.getValue()))
				commit.put(e.getKey(), e.getValue());

			// Remove the from uncommited list
			i.remove();
		}

		// Return the uniform configuration reflecting those changes
		return new InternalUniformsConfiguration(commit);
	}

	public int getVertexAttributeLocation(String name) {
		if (attributesLocations.containsKey(name))
			return attributesLocations.get(name);
		else {
			int location = glGetAttribLocation(shaderProgramId, name);
			if (location == -1) {
				logger().warn("Warning, -1 location for VertexAttrib " + name + " in shader " + this.shaderName);
				// location = 0;
			}
			attributesLocations.put(name, location);
			return location;
		}
	}

	public void use() {
		if (currentProgram == shaderProgramId)
			return;

		glUseProgram(shaderProgramId);

		currentProgram = shaderProgramId;
		// Reset uniforms when changing shader

		// setUniforms.clear();
		uncommitedUniforms.clear();
		commitedUniforms.clear();
	}

	public void validate() {
		if (needValidation) {
			glValidateProgram(shaderProgramId);

			if (glGetProgrami(shaderProgramId, GL_VALIDATE_STATUS) == GL_FALSE) {
				logger().error("Failed to validate program " + shaderName + "");

				String errorsSource = glGetProgramInfoLog(shaderProgramId, 5000);

				String[] errorsLines = errorsSource.split("\n");
				for (String line : errorsLines) {
					logger().debug(line);
				}

				// return;
			}

			needValidation = false;
		}
	}

	static int currentProgram = -2;

	public void destroy() {
		glDeleteProgram(shaderProgramId);
		glDeleteShader(vertexShaderId);
		glDeleteShader(fragShaderId);
		if (geometryShaderId != -1) {
			glDeleteShader(geometryShaderId);
			geometryShaderId = -1;
		}

		uniformsLocations.clear();
		attributesLocations.clear();
	}

	public void reload() {
		destroy();
		load();
	}

	@Override
	public String toString() {
		return "[ShaderProgram : " + this.shaderName + "]";
	}

	static boolean check(HashMap<String, Object> setUniforms2, Object o) {
		return setUniforms2.put("aaa", "whatever") == o;
	}

	@Override
	public Map<String, SamplerType> samplers() {
		return samplers;
	}

	public boolean isLoadedCorrectly() {
		return loadedCorrectly;
	}
}
