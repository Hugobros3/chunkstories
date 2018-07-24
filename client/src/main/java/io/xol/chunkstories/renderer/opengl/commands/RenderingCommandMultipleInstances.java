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
import io.xol.chunkstories.renderer.opengl.shader.ShaderGL.InternalUniformsConfiguration;
import io.xol.chunkstories.renderer.opengl.texture.TexturingConfigurationImplementation;
import io.xol.chunkstories.renderer.opengl.vbo.AttributesConfigurationImplementation;

public class RenderingCommandMultipleInstances extends RenderingCommandSingleInstance {

	final int instances;

	public RenderingCommandMultipleInstances(Primitive primitive, Shader Shader,
			TexturingConfigurationImplementation texturingConfiguration,
			AttributesConfigurationImplementation attributesConfiguration,
			InternalUniformsConfiguration uniformsConfiguration, OpenGLStateMachine StateMachine, Matrix4f objectMatrix,
			int start, int count, int instances) {

		super(primitive, Shader, texturingConfiguration, attributesConfiguration, uniformsConfiguration, StateMachine,
				objectMatrix, start, count);

		this.instances = instances;
	}

	@Override
	public void render(RenderingInterface renderingInterface) throws RenderingException {
		setup(renderingInterface);

		// Do the draw call
		GLCalls.DrawArraysInstanced(modes[primitive.ordinal()], start, count, instances);
	}

}
