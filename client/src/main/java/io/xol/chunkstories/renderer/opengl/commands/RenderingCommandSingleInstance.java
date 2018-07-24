//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.renderer.opengl.commands;

import org.joml.Matrix4f;

import io.xol.chunkstories.api.exceptions.rendering.RenderingException;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.shader.Shader;
import io.xol.chunkstories.api.rendering.vertex.Primitive;
import io.xol.chunkstories.renderer.opengl.GLCalls;
import io.xol.chunkstories.renderer.opengl.OpenGLStateMachine;
import io.xol.chunkstories.renderer.opengl.shader.ShaderGL;
import io.xol.chunkstories.renderer.opengl.shader.ShaderGL.InternalUniformsConfiguration;
import io.xol.chunkstories.renderer.opengl.texture.TexturingConfigurationImplementation;
import io.xol.chunkstories.renderer.opengl.vbo.AttributesConfigurationImplementation;

public class RenderingCommandSingleInstance extends RenderingCommandImplementation {
	Matrix4f objectMatrix;
	int sunLight, blockLight;
	int start, count;

	public RenderingCommandSingleInstance(Primitive primitive, Shader Shader,
			TexturingConfigurationImplementation texturingConfiguration,
			AttributesConfigurationImplementation attributesConfiguration,
			InternalUniformsConfiguration uniformsConfiguration, OpenGLStateMachine StateMachine, Matrix4f objectMatrix,
			int start, int count) {
		super(primitive, Shader, texturingConfiguration, attributesConfiguration, uniformsConfiguration, StateMachine);
		this.objectMatrix = objectMatrix;

		this.start = start;
		this.count = count;
	}

	protected void setup(RenderingInterface renderingInterface) throws RenderingException {
		// Make sure to use the right shader
		((ShaderGL) shader).use();

		// Setups vertex attributes
		this.attributesConfiguration.setup(renderingInterface);

		// Bind required textures
		this.texturingConfiguration.setup(renderingInterface);

		// Compute & send the object matrix
		if (objectMatrix != null) {
			((ShaderGL) this.shader).applyUniformAttribute("objectMatrix", objectMatrix);
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
			((ShaderGL) this.shader).applyUniformAttribute("objectMatrixNormal", normal);
			// this.Shader.setUniformMatrix3f("objectMatrixNormal", normal);
		}

		// Setup pipeline state
		this.stateMachine.setup(renderingInterface);

		// Updates uniforms
		this.uniformsConfiguration.setup(renderingInterface);

		((ShaderGL) this.shader).validate();
	}

	public void render(RenderingInterface renderingInterface) throws RenderingException {
		setup(renderingInterface);

		// Do the draw call
		GLCalls.DrawArrays(modes[primitive.ordinal()], start, count);
	}
}
