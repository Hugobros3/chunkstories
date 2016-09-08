package io.xol.engine.graphics;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import io.xol.chunkstories.api.rendering.AttributesConfiguration;
import io.xol.chunkstories.api.rendering.PipelineConfiguration;
import io.xol.chunkstories.api.rendering.Renderable;
import io.xol.chunkstories.api.rendering.RenderingCommand;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.ShaderInterface;
import io.xol.chunkstories.api.rendering.TexturingConfiguration;
import io.xol.chunkstories.api.rendering.UniformsConfiguration;
import io.xol.engine.math.lalgb.Matrix4f;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class RenderingCommandImplementation implements RenderingCommand, Renderable
{
	List<Matrix4f> objectMatrices = new LinkedList<Matrix4f>();

	protected ShaderInterface shaderInterface;
	protected TexturingConfiguration texturingConfiguration;
	protected AttributesConfiguration attributesConfiguration;
	protected UniformsConfiguration uniformsConfiguration;
	protected PipelineConfiguration pipelineConfiguration;

	public RenderingCommandImplementation(ShaderInterface shaderInterface, TexturingConfiguration texturingConfiguration, AttributesConfiguration attributesConfiguration, UniformsConfiguration uniformsConfiguration,
			PipelineConfiguration pipelineConfiguration, Matrix4f objectMatrix)
	{
		this.shaderInterface = shaderInterface;
		this.texturingConfiguration = texturingConfiguration;
		this.attributesConfiguration = attributesConfiguration;
		this.uniformsConfiguration = uniformsConfiguration;
		this.pipelineConfiguration = pipelineConfiguration;
		this.objectMatrices.add(objectMatrix);
	}

	@Override
	public Collection<Matrix4f> getObjectsMatrices()
	{
		return objectMatrices;
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

	public RenderingCommand merge(RenderingCommandImplementation mergeWith)
	{
		//Debug
		assert mergeWith.canMerge(this);

		for (Matrix4f foreightObject : mergeWith.getObjectsMatrices())
			objectMatrices.add(foreightObject);

		return this;
	}

	@Override
	public RenderingCommand render(RenderingInterface renderingInterface)
	{
		return null;
	}

}