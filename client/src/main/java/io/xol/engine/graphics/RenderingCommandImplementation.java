//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.engine.graphics;

import io.xol.chunkstories.api.rendering.pipeline.AttributesConfiguration;
import io.xol.chunkstories.api.rendering.pipeline.StateMachine;
import io.xol.chunkstories.api.rendering.pipeline.Shader;
import io.xol.engine.graphics.shaders.ShaderProgram.InternalUniformsConfiguration;
import io.xol.engine.graphics.textures.TexturingConfigurationImplementation;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import io.xol.chunkstories.api.rendering.Primitive;

import static org.lwjgl.opengl.GL11.*;

public abstract class RenderingCommandImplementation {
	// Draw call paramters
	protected Primitive primitive;

	// Pipeline state
	protected Shader shader;
	protected TexturingConfigurationImplementation texturingConfiguration;
	protected AttributesConfigurationImplementation attributesConfiguration;
	protected InternalUniformsConfiguration uniformsConfiguration;
	protected OpenGLStateMachine stateMachine;

	static int modes[] = { GL_POINTS, GL_LINES, GL_TRIANGLES, GL_QUADS };

	protected static Matrix4f temp = new Matrix4f();
	protected static Matrix3f normal = new Matrix3f();

	public RenderingCommandImplementation(Primitive primitive, Shader Shader, TexturingConfigurationImplementation texturingConfiguration,
			AttributesConfigurationImplementation attributesConfiguration, InternalUniformsConfiguration uniformsConfiguration,
			OpenGLStateMachine StateMachine/* , Matrix4f objectMatrix */) {
		this.primitive = primitive;

		this.shader = Shader;
		this.texturingConfiguration = texturingConfiguration;
		this.attributesConfiguration = attributesConfiguration;
		this.uniformsConfiguration = uniformsConfiguration;
		this.stateMachine = StateMachine;
	}

	public Shader getShader() {
		return shader;
	}

	public TexturingConfigurationImplementation getBoundTextures() {
		return texturingConfiguration;
	}

	public AttributesConfiguration getAttributesConfiguration() {
		return attributesConfiguration;
	}

	public InternalUniformsConfiguration getUniformsConfiguration() {
		return uniformsConfiguration;
	}

	public StateMachine getStateMachine() {
		return stateMachine;
	}

	public Primitive getPrimitive() {
		return primitive;
	}

}