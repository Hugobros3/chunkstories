//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.renderer;

import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.StateMachine.BlendMode;
import io.xol.chunkstories.api.rendering.StateMachine.DepthTestMode;
import io.xol.chunkstories.api.rendering.shader.Shader;

public class ReflectionsRenderer {

	private final WorldRendererImplementation worldRenderer;

	public ReflectionsRenderer(WorldRendererImplementation worldRenderer) {
		super();
		this.worldRenderer = worldRenderer;
	}

	/** Only ran if realtime screen-space reflections are enabled, otherwise a flag is raised and a baked envmap is used. */
	public void renderReflections(RenderingInterface renderer) {
		if(!renderer.renderingConfig().isDoRealtimeReflections())
			return;
		
		Shader reflectionsShader = renderer.useShader("reflections");

		//This isn't a depth-buffered pass.
		renderer.setDepthTestMode(DepthTestMode.DISABLED);
		renderer.setBlendMode(BlendMode.DISABLED);

		//renderer.getRenderTargetManager().setConfiguration(worldRenderer.renderBuffers.fboShadedBuffer);
		renderer.getRenderTargetManager().setConfiguration(worldRenderer.renderBuffers.fboSSR);
		renderer.getRenderTargetManager().clearBoundRenderTargetAll();

		//Required to execute SSR
		renderer.bindTexture2D("shadedBuffer", worldRenderer.renderBuffers.rbShaded);
		renderer.bindTexture2D("depthBuffer", worldRenderer.renderBuffers.rbZBuffer);
		renderer.bindTexture2D("normalBuffer", worldRenderer.renderBuffers.rbNormal);
		renderer.bindTexture2D("specularityBuffer", worldRenderer.renderBuffers.rbSpecularity);
		renderer.bindTexture2D("voxelLightBuffer", worldRenderer.renderBuffers.rbVoxelLight);

		//Required to shade the sky
		renderer.bindTexture2D("sunSetRiseTexture", worldRenderer.worldTextures.sunGlowTexture);
		renderer.bindTexture2D("skyTextureSunny", worldRenderer.worldTextures.skyTextureSunny);
		renderer.bindTexture2D("skyTextureRaining", worldRenderer.worldTextures.skyTextureRaining);
		reflectionsShader.setUniform1f("dayTime", worldRenderer.skyRenderer.getDayTime());

		//Texture2D lightColors = TexturesHandler.getTexture("./textures/environement/lightcolors.png");
		//renderingContext.bindTexture2D("lightColors", lightColors);

		renderer.bindCubemap("environmentCubemap", worldRenderer.renderBuffers.rbEnvironmentMap);

		// Matrices for screen-space transformations
		renderer.getCamera().setupShader(reflectionsShader);
		worldRenderer.skyRenderer.setupShader(reflectionsShader);

		//Disable depth writing and run the deal
		renderer.getRenderTargetManager().setDepthMask(false);
		renderer.drawFSQuad();
		renderer.getRenderTargetManager().setDepthMask(true);

		renderer.setDepthTestMode(DepthTestMode.LESS_OR_EQUAL);
		renderer.setBlendMode(BlendMode.DISABLED);
		
		renderer.getRenderTargetManager().setConfiguration(worldRenderer.renderBuffers.fboShadedBuffer);
	}
}
