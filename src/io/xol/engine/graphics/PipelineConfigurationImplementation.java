package io.xol.engine.graphics;

import io.xol.chunkstories.api.rendering.PipelineConfiguration;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Abstracts the OpenGL state machine, reduces state changes by tracking them
 */
public final class PipelineConfigurationImplementation implements PipelineConfiguration
{
	public static PipelineConfigurationImplementation DEFAULT = new PipelineConfigurationImplementation(DepthTestMode.LESS_OR_EQUAL, BlendMode.DISABLED, PolygonFillMode.FILL);
	
	private final DepthTestMode depthTestMode;
	private final BlendMode blendMode;
	private final PolygonFillMode polygonFillMode;
	
	public PipelineConfigurationImplementation(DepthTestMode depthTestMode, BlendMode blendMode, PolygonFillMode polygonFillMode)
	{
		this.depthTestMode = depthTestMode;
		this.blendMode = blendMode;
		this.polygonFillMode = polygonFillMode;
	}
	
	@Override
	public DepthTestMode getDepthTestMode()
	{
		return depthTestMode;
	}
	
	@Override
	public BlendMode getBlendMode()
	{
		return blendMode;
	}
	
	@Override
	public PolygonFillMode getPolygonFillMode()
	{
		return polygonFillMode;
	}

	public PipelineConfigurationImplementation setDepthTestMode(DepthTestMode depthTestMode)
	{
		return new PipelineConfigurationImplementation(depthTestMode, blendMode, polygonFillMode);
	}

	public PipelineConfigurationImplementation setBlendMode(BlendMode blendMode)
	{
		return new PipelineConfigurationImplementation(depthTestMode, blendMode, polygonFillMode);
	}

	public PipelineConfigurationImplementation setPolygonFillMode(PolygonFillMode polygonFillMode)
	{
		return new PipelineConfigurationImplementation(depthTestMode, blendMode, polygonFillMode);
	}
	
	public boolean equals(Object o)
	{
		if(o instanceof PipelineConfigurationImplementation)
		{
			PipelineConfigurationImplementation p = (PipelineConfigurationImplementation)o;
			
			if(p.depthTestMode != this.depthTestMode)
				return false;

			if(p.blendMode != this.blendMode)
				return false;

			if(p.polygonFillMode != this.polygonFillMode)
				return false;
			
			return true;
		}
		return false;
	}
}
