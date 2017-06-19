package io.xol.chunkstories.api.particles;

import io.xol.chunkstories.api.Content;
import io.xol.chunkstories.api.content.NamedWithProperties;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface ParticleType extends NamedWithProperties
{
	public int getID();
	
	public String getName();
	
	public Content.ParticlesTypes store();
	
	/** When should we render those particle types */
	public enum RenderTime {
		/** Actually iterated anyways! Use case is light particles, mostly. */
		NEVER,
		/** Done after the rest of the opaque GBuffer stuff. */
		GBUFFER,
		FORWARD
		;
	}
	
	public enum ParticleRenderingMode {
		BILLBOARD;
	}
	
	public abstract RenderTime getRenderTime();
	
	public default String getShaderName()
	{
		return "particles";
	}
	
	/** Returns null or a path to an asset. */
	public abstract String getAlbedoTexture();
	
	/** Returns null or a path to an asset. */
	public abstract String getNormalTexture();

	/** Returns null or a path to an asset. */
	public abstract String getMaterialTexture();
	
	/** Defaults to 1.0f */
	public abstract float getBillboardSize();
}
