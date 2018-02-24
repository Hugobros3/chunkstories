//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.renderer.lights;

import org.joml.Vector3d;
import org.joml.Vector3f;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.RenderingInterface.LightsAccumulator;
import io.xol.chunkstories.api.rendering.lightning.Light;
import io.xol.chunkstories.api.rendering.lightning.SpotLight;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.engine.graphics.RenderingContext;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class LightsRenderer implements LightsAccumulator
{
	//private final RenderingContext renderingContext;
	
	int lightsBuffer = 0;
	ShaderInterface lightShader;
	private List<Light> lights = new LinkedList<Light>();

	public LightsRenderer(RenderingContext renderingContext)
	{
		//this.renderingContext = renderingContext;
	}

	public void queueLight(Light light)
	{
		lights.add(light);
	}

	public Iterator<Light> getAllLights()
	{
		return lights.iterator();
	}
	
	public void renderPendingLights(RenderingInterface renderingContext)
	{
		lightShader = renderingContext.useShader("light");
		lightsBuffer = 0;
		//Render entities lights
		Iterator<Light> lightsIterator = lights.iterator();
		while(lightsIterator.hasNext())
		{
			renderDefferedLight(renderingContext, lightsIterator.next());
			lightsIterator.remove();
		}
		//Render particles's lights
		//Client.world.particlesHolder.renderLights(this);
		// Render remaining lights
		if (lightsBuffer > 0)
		{
			lightShader.setUniform1i("lightsToRender", lightsBuffer);
			renderingContext.drawFSQuad();
		}
	}
	
	private void renderDefferedLight(RenderingInterface renderingContext, Light light)
	{
		// Light culling
		if (!lightInFrustrum(renderingContext, light))
			return;

		lightShader.setUniform1f("lightDecay[" + lightsBuffer + "]", light.getDecay());
		lightShader.setUniform3f("lightPos[" + lightsBuffer + "]", light.getPosition());
		lightShader.setUniform3f("lightColor[" + lightsBuffer + "]", light.getColor());
		if (light instanceof SpotLight)
		{
			SpotLight spotlight = (SpotLight)light;
			lightShader.setUniform3f("lightDir[" + lightsBuffer + "]", spotlight.getDirection());
			lightShader.setUniform1f("lightAngle[" + lightsBuffer + "]", (float) (spotlight.getAngle() / 180 * Math.PI));
		}
		else
			lightShader.setUniform1f("lightAngle[" + lightsBuffer + "]", 0f);

		lightsBuffer++;
		if (lightsBuffer == 64)
		{
			lightShader.setUniform1i("lightsToRender", lightsBuffer);
			renderingContext.drawFSQuad();
			//drawFSQuad();
			lightsBuffer = 0;
		}
	}

	private boolean lightInFrustrum(RenderingInterface renderingContext, Light light)
	{
		if(renderingContext.getCamera().getCameraPosition().distance(new Vector3d(light.position)) <= light.decay)
			return true;
		
		return renderingContext.getCamera().isBoxInFrustrum(new Vector3f(light.getPosition().x() - light.getDecay(), light.getPosition().y() - light.getDecay(), light.getPosition().z() - light.getDecay()), new Vector3f(light.getDecay() * 2f, light.getDecay() * 2f, light.getDecay() * 2f));
	}
}
