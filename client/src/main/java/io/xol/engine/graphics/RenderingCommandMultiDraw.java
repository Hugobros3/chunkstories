//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.engine.graphics;

import java.nio.IntBuffer;

import org.joml.Matrix4f;

import io.xol.chunkstories.api.exceptions.rendering.RenderingException;
import io.xol.chunkstories.api.rendering.Primitive;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.pipeline.Shader;
import io.xol.engine.graphics.shaders.ShaderProgram;
import io.xol.engine.graphics.shaders.ShaderProgram.InternalUniformsConfiguration;
import io.xol.engine.graphics.textures.TexturingConfigurationImplementation;

public class RenderingCommandMultiDraw extends RenderingCommandImplementation {
	Matrix4f objectMatrix;
	int sunLight, blockLight;
	IntBuffer starts, counts;

	public RenderingCommandMultiDraw(Primitive primitive, Shader Shader, TexturingConfigurationImplementation texturingConfiguration,
			AttributesConfigurationImplementation attributesConfiguration, InternalUniformsConfiguration uniformsConfiguration, OpenGLStateMachine StateMachine,
			Matrix4f objectMatrix, IntBuffer starts, IntBuffer counts) {
		super(primitive, Shader, texturingConfiguration, attributesConfiguration, uniformsConfiguration, StateMachine);
		this.objectMatrix = objectMatrix;

		this.starts = starts;
		this.counts = counts;
	}

	protected void setup(RenderingInterface renderingInterface) throws RenderingException {
		// Make sure to use the right shader
		((ShaderProgram) shader).use();

		// Setups vertex attributes
		this.attributesConfiguration.setup(renderingInterface);

		// Bind required textures
		this.texturingConfiguration.setup(renderingInterface);

		// Compute & send the object matrix
		if (objectMatrix != null) {
			((ShaderProgram) this.shader).applyUniformAttribute("objectMatrix", objectMatrix);
			// this.Shader.setUniformMatrix4f("objectMatrix", objectMatrix);

			objectMatrix.invert(temp);
			// Matrix4f.invert(objectMatrix, temp);

			temp.transpose();
			// Matrix4f.transpose(temp, temp);
			// TODO make a clean function for this
			normal.m00 = temp.m00();
			normal.m01 = temp.m01();
			normal.m02 = temp.m02();

			normal.m10 = temp.m10();
			normal.m11 = temp.m11();
			normal.m12 = temp.m12();

			normal.m20 = temp.m20();
			normal.m21 = temp.m21();
			normal.m22 = temp.m22();
			((ShaderProgram) this.shader).applyUniformAttribute("objectMatrixNormal", normal);
			// this.Shader.setUniformMatrix3f("objectMatrixNormal", normal);
		}

		// Setup pipeline state
		this.stateMachine.setup(renderingInterface);

		// Updates uniforms
		this.uniformsConfiguration.setup(renderingInterface);

		((ShaderProgram) this.shader).validate();
	}

	public void render(RenderingInterface renderingInterface) throws RenderingException {
		setup(renderingInterface);

		// Do the draw call
		GLCalls.MultiDrawArrays(modes[primitive.ordinal()], starts, counts);
	}
}
