package io.xol.engine.graphics;

import io.xol.chunkstories.api.rendering.RenderingCommand;
import io.xol.chunkstories.api.rendering.pipeline.AttributesConfiguration;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.api.rendering.pipeline.TexturingConfiguration;
import io.xol.chunkstories.api.rendering.pipeline.UniformsConfiguration;
import io.xol.chunkstories.api.math.Matrix3f;
import io.xol.chunkstories.api.math.Matrix4f;
import io.xol.chunkstories.api.rendering.Primitive;

import static org.lwjgl.opengl.GL11.*;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class RenderingCommandImplementation implements RenderingCommand
{
	//Draw call paramters
	protected Primitive primitive;
	int start, count;
	
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
			PipelineConfiguration pipelineConfiguration/*, Matrix4f objectMatrix*/, int start, int count)
	{
		this.primitive = primitive;
		this.start = start;
		this.count = count;
		
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

	/**
	 * Used to automatically instanciate similar rendering commands
	 */
	public final boolean canMerge(RenderingCommand renderingCommand)
	{
		if(getPrimitive() != renderingCommand.getPrimitive())
			return false;
		
		//Check shader
		if (!getShader().equals(renderingCommand.getShader()))
			return false;

		//These rendering commands use the same pipeline configuration
		if(!getPipelineConfiguration().equals(renderingCommand.getPipelineConfiguration()))
			return false;
		
		//These rendering commands do not use different textures for the same samplers
		if (!getBoundTextures().isCompatibleWith(renderingCommand.getBoundTextures()))
			return false;

		//These rendering commands do not use different attributes sources for the same attribute locations
		if (!getAttributesConfiguration().isCompatibleWith(renderingCommand.getAttributesConfiguration()))
			return false;

		//These rendering commands do not sets differents values to the same uniforms
		if (!getUniformsConfiguration().isCompatibleWith(renderingCommand.getUniformsConfiguration()))
			return false;

		return true;
	}

	@Override
	public Primitive getPrimitive()
	{
		return primitive;
	}

}