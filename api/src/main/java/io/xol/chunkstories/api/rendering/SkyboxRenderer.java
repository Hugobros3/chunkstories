package io.xol.chunkstories.api.rendering;

import io.xol.chunkstories.api.math.vector.sp.Vector3fm;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** Takes care of rendering the 'background' of a frame, typically using some sort of skybox or fancy props.
 *  Is also responsible to setup shader parameters, such as fog
 */
public interface SkyboxRenderer {
	
	public Vector3fm getSunPosition();
	
	public void render(RenderingInterface renderingContext);
	
	public void setupShader(ShaderInterface shaderInterface);
}
