//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.engine.graphics;

import io.xol.chunkstories.api.rendering.RenderingCommand;
import io.xol.chunkstories.api.rendering.pipeline.AttributesConfiguration;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.api.rendering.pipeline.TexturingConfiguration;
import io.xol.chunkstories.api.rendering.pipeline.UniformsConfiguration;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import io.xol.chunkstories.api.rendering.Primitive;

import static org.lwjgl.opengl.GL11.*;

public abstract class RenderingCommandImplementation implements RenderingCommand
{
	//Draw call paramters
	protected Primitive primitive;
	
	//Pipeline state
	protected ShaderInterface shaderInterface;
	protected TexturingConfiguration texturingConfiguration;
	protected AttributesConfiguration attributesConfiguration;
	protected UniformsConfiguration uniformsConfiguration;
	protected PipelineConfiguration pipelineConfiguration;
	
	static int modes[] = {GL_POINTS, GL_LINES, GL_TRIANGLES, GL_QUADS};
	
	protected static Matrix4f temp = new Matrix4f();
	protected static Matrix3f normal = new Matrix3f();

	public RenderingCommandImplementation(Primitive primitive, ShaderInterface shaderInterface, TexturingConfiguration texturingConfiguration, AttributesConfiguration attributesConfiguration, UniformsConfiguration uniformsConfiguration,
			PipelineConfiguration pipelineConfiguration/*, Matrix4f objectMatrix*/)
	{
		this.primitive = primitive;
		
		this.shaderInterface = shaderInterface;
		this.texturingConfiguration = texturingConfiguration;
		this.attributesConfiguration = attributesConfiguration;
		this.uniformsConfiguration = uniformsConfiguration;
		this.pipelineConfiguration = pipelineConfiguration;
	}

	public ShaderInterface getShader()
	{
		return shaderInterface;
	}

	public TexturingConfiguration getBoundTextures()
	{
		return texturingConfiguration;
	}

	public AttributesConfiguration getAttributesConfiguration()
	{
		return attributesConfiguration;
	}

	public UniformsConfiguration getUniformsConfiguration()
	{
		return uniformsConfiguration;
	}

	public PipelineConfiguration getPipelineConfiguration()
	{
		return pipelineConfiguration;
	}

	@Override
	public Primitive getPrimitive()
	{
		return primitive;
	}

}