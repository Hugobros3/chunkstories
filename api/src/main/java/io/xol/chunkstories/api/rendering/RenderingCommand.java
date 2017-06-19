package io.xol.chunkstories.api.rendering;

import io.xol.chunkstories.api.rendering.Primitive;
import io.xol.chunkstories.api.rendering.pipeline.AttributesConfiguration;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.api.rendering.pipeline.TexturingConfiguration;
import io.xol.chunkstories.api.rendering.pipeline.UniformsConfiguration;

//(c) 2015-2017 XolioWare Interactive
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
