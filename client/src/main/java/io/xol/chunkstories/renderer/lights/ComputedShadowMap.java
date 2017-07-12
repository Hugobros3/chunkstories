package io.xol.chunkstories.renderer.lights;

import org.joml.Matrix4f;
import io.xol.chunkstories.api.rendering.textures.Texture2D;

public interface ComputedShadowMap
{
	public Texture2D getShadowMap();
	
	public Matrix4f getShadowTransformationMatrix();
}
