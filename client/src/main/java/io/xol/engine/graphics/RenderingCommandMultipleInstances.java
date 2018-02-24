//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.engine.graphics;

import org.joml.Matrix4f;

import io.xol.chunkstories.api.exceptions.rendering.RenderingException;
import io.xol.chunkstories.api.rendering.Primitive;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.pipeline.AttributesConfiguration;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.api.rendering.pipeline.TexturingConfiguration;
import io.xol.chunkstories.api.rendering.pipeline.UniformsConfiguration;

public class RenderingCommandMultipleInstances extends RenderingCommandSingleInstance{

	final int instances;
	
	public RenderingCommandMultipleInstances(Primitive primitive, ShaderInterface shaderInterface,
			TexturingConfiguration texturingConfiguration, AttributesConfiguration attributesConfiguration,
			UniformsConfiguration uniformsConfiguration, PipelineConfiguration pipelineConfiguration,
			Matrix4f objectMatrix, int start, int count, int instances) {
		super(primitive, shaderInterface, texturingConfiguration, attributesConfiguration, uniformsConfiguration,
				pipelineConfiguration, objectMatrix, start, count);
		
		this.instances = instances;
	}

	@Override
	public void render(RenderingInterface renderingInterface) throws RenderingException
	{
		setup(renderingInterface);

		//Do the draw call
		GLCalls.DrawArraysInstanced(modes[primitive.ordinal()], start, count, instances);
	}

}
