package io.xol.chunkstories.api.rendering;

import java.util.Collection;

import io.xol.chunkstories.api.rendering.RenderingInterface.Primitive;
import io.xol.engine.math.lalgb.Matrix4f;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Describes a rendering task the game renderer will have to deal with
 */
public interface RenderingCommand
{
	public Primitive getPrimitive();
	
	public ShaderInterface getShader();

	public TexturingConfiguration getBoundTextures();

	public AttributesConfiguration getAttributesConfiguration();

	public UniformsConfiguration getUniformsConfiguration();

	public PipelineConfiguration getPipelineConfiguration();

	public Collection<Matrix4f> getObjectsMatrices();
	
	/**
	 * Used to automatically instanciate similar rendering commands
	 */
	public boolean canMerge(RenderingCommand renderingCommand);
}
