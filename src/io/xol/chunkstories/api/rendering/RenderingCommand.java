package io.xol.chunkstories.api.rendering;

import java.util.Collection;

import io.xol.chunkstories.api.rendering.Primitive;
import io.xol.engine.math.lalgb.Matrix4f;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Describes precisely everything needed to draw something on screen
 */
public interface RenderingCommand extends Renderable
{
	public Primitive getPrimitive();
	
	public ShaderInterface getShader();

	public TexturingConfiguration getBoundTextures();

	public AttributesConfiguration getAttributesConfiguration();

	public UniformsConfiguration getUniformsConfiguration();

	public PipelineConfiguration getPipelineConfiguration();
	
	/**
	 * Used to automatically instanciate similar rendering commands
	 */
	public boolean canMerge(RenderingCommand renderingCommand);
}
